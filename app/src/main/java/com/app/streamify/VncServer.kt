package com.app.streamify

import android.graphics.Bitmap
import android.media.ImageReader
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class VncServer(
    private val port: Int,
    private val imageReader: ImageReader?,
    private val screenWidth: Int,
    private val screenHeight: Int
) : Runnable {

    companion object {
        private const val TAG = "VncServer"
        private const val VNC_VERSION = "RFB 003.008\n"
        private const val AUTH_NONE = 1
        private const val AUTH_SUCCESS = 0
        private const val ENCODING_RAW = 0
        
        // Performance optimizations
        private const val MAX_FPS = 25
        private const val MIN_FRAME_INTERVAL = 1000 / MAX_FPS // 40ms for 25fps
        private const val MAX_CLIENTS = 3 // Limit concurrent clients
        
        // DISABLE ALL VERBOSE LOGGING FOR PRODUCTION - ZERO ERROR MESSAGES
        private const val ENABLE_DEBUG_LOGS = false
        
        private fun logD(message: String) {
            if (ENABLE_DEBUG_LOGS) {
                android.util.Log.d(TAG, message)
            }
        }
        
        private fun logE(message: String, throwable: Throwable? = null) {
            if (ENABLE_DEBUG_LOGS) {
                if (throwable != null) {
                    android.util.Log.e(TAG, message, throwable)
                } else {
                    android.util.Log.e(TAG, message)
                }
            }
        }
        private const val BUFFER_SIZE = 8192 // Optimized buffer size
    }

    init {
        if (port <= 0 || port > 65535) {
            throw IllegalArgumentException("Invalid port number: $port")
        }
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw IllegalArgumentException("Invalid screen dimensions: ${screenWidth}x${screenHeight}")
        }
        logD("VncServer initialized with port: $port, screen: ${screenWidth}x${screenHeight}")
    }

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var clientSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    
    // Client connection tracking
    private val connectedClients = mutableSetOf<Socket>()
    private val clientThreads = mutableSetOf<Thread>()
    private val isClientConnected = AtomicBoolean(false)
    private val shouldStopGracefully = AtomicBoolean(false)
    
    // Connection settings
    private val connectionTimeout = 10000 // 10 seconds
    private val readTimeout = 5000 // 5 seconds

    // Cache the last successful screen capture
    private var lastScreenData: ByteArray? = null
    private var lastScreenWidth = 320
    private var lastScreenHeight = 700

    override fun run() {
        try {
            // RESET ALL STATE VARIABLES FOR FRESH START
            isRunning.set(true)
            shouldStopGracefully.set(false)
            isClientConnected.set(false)
            
            // Clear any previous connections
            synchronized(connectedClients) {
                connectedClients.clear()
            }
            clientThreads.clear()
            
            logD( "VNC server starting on port $port")
            logD( "Security: Only accepting connections from local network")

            serverSocket = ServerSocket(port)
            // ENABLE SOCKET REUSE FOR RESTART CAPABILITY
            serverSocket?.reuseAddress = true
            logD( "VNC server listening on port $port")

            while (isRunning.get()) {
                try {
                    val socket = serverSocket?.accept()
                    socket?.let { clientSocket ->
                        // Security check: Only allow local network connections
                        val clientAddress = clientSocket.inetAddress.hostAddress
                        if (isLocalNetworkAddress(clientAddress)) {
                            logD( "Client connected from local network: $clientAddress")
                            
                            // Handle client in separate thread to prevent blocking
                            val clientThread = Thread {
                                handleClient(clientSocket)
                            }
                            clientThread.name = "VNC-Client-$clientAddress"
                            
                            synchronized(clientThreads) {
                                clientThreads.add(clientThread)
                            }
                            
                            clientThread.start()
                        } else {
                            // "Rejected connection from non-local address: $clientAddress")
                            clientSocket.close()
                        }
                    }
                } catch (e: java.net.SocketException) {
                    if (isRunning.get()) {
                        logE( "Socket error accepting client: ${e.message}")
                    } else {
                        logD( "Server socket closed during shutdown")
                    }
                    break
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        logE( "Error accepting client: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            logE( "Error starting VNC server: ${e.message}", e)
        } finally {
            cleanup()
        }
    }
    
    // Add connection health validation
    private fun isConnectionHealthy(socket: Socket?): Boolean {
        return try {
            socket != null && 
            !socket.isClosed && 
            socket.isConnected && 
            !socket.isInputShutdown && 
            !socket.isOutputShutdown &&
            isRunning.get() &&
            !shouldStopGracefully.get()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isLocalNetworkAddress(address: String?): Boolean {
        if (address == null) return false
        
        return address.startsWith("192.168.") ||
               address.startsWith("10.") ||
               address.startsWith("172.16.") ||
               address.startsWith("172.17.") ||
               address.startsWith("172.18.") ||
               address.startsWith("172.19.") ||
               address.startsWith("172.20.") ||
               address.startsWith("172.21.") ||
               address.startsWith("172.22.") ||
               address.startsWith("172.23.") ||
               address.startsWith("172.24.") ||
               address.startsWith("172.25.") ||
               address.startsWith("172.26.") ||
               address.startsWith("172.27.") ||
               address.startsWith("172.28.") ||
               address.startsWith("172.29.") ||
               address.startsWith("172.30.") ||
               address.startsWith("172.31.") ||
               address == "127.0.0.1" ||
               address == "localhost"
    }

    private fun handleClient(socket: Socket) {
        var localOutputStream: DataOutputStream? = null
        var localInputStream: DataInputStream? = null
        
        try {
            // Set socket timeouts to prevent hanging
            socket.soTimeout = readTimeout
            socket.tcpNoDelay = true
            
            // Track this client
            synchronized(connectedClients) {
                connectedClients.add(socket)
            }
            
            localOutputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            localInputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))
            
            // Set class level streams for this client
            outputStream = localOutputStream
            inputStream = localInputStream
            clientSocket = socket
            isClientConnected.set(true)

            performVncHandshake()
            
            logD( "VNC handshake completed, entering message loop")

            while (isConnectionHealthy(socket)) {
                try {
                    // Check for graceful stop signal  
                    if (shouldStopGracefully.get()) {
                        sendDisconnectionNotice()
                        break
                    }
                    
                    // Quick connection health check
                    if (!isConnectionHealthy(socket)) break
                    
                    val messageType = inputStream?.readByte()?.toInt() ?: break
                    
                    when (messageType) {
                        0 -> {
                            handleSetPixelFormat()
                        }
                        2 -> {
                            handleSetEncodings()
                        }
                        3 -> {
                            handleFramebufferUpdateRequest()
                        }
                        4 -> {
                            handleKeyEvent()
                        }
                        5 -> {
                            // REMOTE CONTROL - pointer events
                            handlePointerEvent()
                        }
                        6 -> {
                            handleClientCutText()
                        }
                        else -> {
                            // Skip unknown messages gracefully
                            Thread.sleep(10)
                        }
                    }
                } catch (e: EOFException) {
                    // Client disconnected gracefully - NO ERROR LOGGING
                    break
                } catch (e: java.net.SocketTimeoutException) {
                    // Normal timeout - continue silently
                    if (!isRunning.get()) break
                    continue
                } catch (e: java.net.SocketException) {
                    // Connection issues - handle silently
                    break
                } catch (e: IOException) {
                    // IO issues - handle silently  
                    break
                } catch (e: Exception) {
                    // Any other error - handle silently
                    break
                }
            }
        } catch (e: Exception) {
            logE( "Error handling client: ${e.message}", e)
        } finally {
            // Clean disconnect - no logging
            isClientConnected.set(false)
            
            // Remove from tracking and cleanup
            synchronized(connectedClients) {
                connectedClients.remove(socket)
            }
            cleanupClient(localOutputStream, localInputStream, socket)
        }
    }

    private fun performVncHandshake() {
        try {
            // Send VNC version
            if (!safeWrite { outputStream?.writeBytes(VNC_VERSION) }) return
            if (!safeFlush()) return
            logD( "Sent VNC version")

            // Read client version
            val clientVersionBytes = ByteArray(12)
            inputStream?.readFully(clientVersionBytes)
            val clientVersion = String(clientVersionBytes).trim()
            logD( "Client version: $clientVersion")

            // Send authentication methods
            if (!safeWrite { outputStream?.writeByte(1) }) return        // Number of auth methods
            if (!safeWrite { outputStream?.writeByte(AUTH_NONE) }) return // No authentication
            if (!safeFlush()) return

            // Read client auth choice
            val authChoice = inputStream?.readByte() ?: 0
            logD( "Client auth choice: $authChoice")

            // Send authentication result
            if (!safeWrite { outputStream?.writeInt(AUTH_SUCCESS) }) return
            if (!safeFlush()) return

            // Send server initialization
            sendServerInit()

            // Read client initialization (shared flag)
            val sharedFlag = inputStream?.readByte() ?: 0
            logD( "Client shared flag: $sharedFlag")

        } catch (e: Exception) {
            logE( "Error during VNC handshake: ${e.message}", e)
            throw e
        }
    }

    private fun sendServerInit() {
        try {
            // Fixed dimensions for consistent client display
            val displayWidth = 320
            val displayHeight = 700

            // Framebuffer dimensions - use safe writes
            if (!safeWrite { outputStream?.writeShort(displayWidth) }) return
            if (!safeWrite { outputStream?.writeShort(displayHeight) }) return

            // Pixel format - Use 32-bit RGBA format for consistency
            if (!safeWrite { outputStream?.writeByte(32) }) return // bits per pixel (4 bytes per pixel)
            if (!safeWrite { outputStream?.writeByte(24) }) return // depth (24-bit color)
            if (!safeWrite { outputStream?.writeByte(0) }) return  // big endian flag (0 = little endian)
            if (!safeWrite { outputStream?.writeByte(1) }) return  // true color flag
            if (!safeWrite { outputStream?.writeShort(255) }) return // red max
            if (!safeWrite { outputStream?.writeShort(255) }) return // green max
            if (!safeWrite { outputStream?.writeShort(255) }) return // blue max
            if (!safeWrite { outputStream?.writeByte(16) }) return // red shift
            if (!safeWrite { outputStream?.writeByte(8) }) return  // green shift
            if (!safeWrite { outputStream?.writeByte(0) }) return  // blue shift
            // 3 bytes padding
            if (!safeWrite { outputStream?.writeByte(0) }) return
            if (!safeWrite { outputStream?.writeByte(0) }) return
            if (!safeWrite { outputStream?.writeByte(0) }) return

            // Server name
            val serverName = "Streamify Android VNC"
            if (!safeWrite { outputStream?.writeInt(serverName.length) }) return
            if (!safeWrite { outputStream?.writeBytes(serverName) }) return
            if (!safeFlush()) return

            logD( "Server initialization sent: ${displayWidth}x${displayHeight}, 32bpp")
        } catch (e: Exception) {
            logE( "Error sending server init: ${e.message}")
            // Don't rethrow to prevent cascade failures
        }
    }

    private fun handleSetPixelFormat() {
        try {
            inputStream?.skipBytes(3) // padding
            inputStream?.skipBytes(16) // pixel format (16 bytes)
            logD( "Set pixel format handled")
        } catch (e: Exception) {
            logE( "Error handling set pixel format: ${e.message}")
        }
    }

    private fun handleSetEncodings() {
        try {
            inputStream?.skipBytes(1) // padding
            val numEncodings = inputStream?.readShort()?.toInt() ?: 0

            for (i in 0 until numEncodings) {
                val encoding = inputStream?.readInt() ?: 0
                logD( "Client supports encoding: $encoding")
            }
            logD( "Set encodings handled: $numEncodings encodings")
        } catch (e: Exception) {
            logE( "Error handling set encodings: ${e.message}")
        }
    }

    private fun handleFramebufferUpdateRequest() {
        try {
            val incremental = inputStream?.readByte() ?: 0
            val x = inputStream?.readShort()?.toInt() ?: 0
            val y = inputStream?.readShort()?.toInt() ?: 0
            val w = inputStream?.readShort()?.toInt() ?: 0
            val h = inputStream?.readShort()?.toInt() ?: 0

            logD( "Framebuffer update request: $x,$y ${w}x$h incremental=$incremental")

            // Send update immediately without delay
            sendFramebufferUpdate(0, 0, 320, 700)

        } catch (e: Exception) {
            logE( "Error handling framebuffer update request: ${e.message}")
        }
    }

    private fun sendFramebufferUpdate(x: Int, y: Int, w: Int, h: Int) {
        try {
            // Check if connection is still valid before sending
            if (!isConnectionValid()) {
                logD( "Connection no longer valid, skipping framebuffer update")
                return
            }
            
            // Framebuffer update message header - with safe writes
            safeWrite { outputStream?.writeByte(0) }     // Message type
            safeWrite { outputStream?.writeByte(0) }     // Padding
            safeWrite { outputStream?.writeShort(1) }    // Number of rectangles

            // Rectangle header
            safeWrite { outputStream?.writeShort(x) }    // X position
            safeWrite { outputStream?.writeShort(y) }    // Y position
            safeWrite { outputStream?.writeShort(w) }    // Width
            safeWrite { outputStream?.writeShort(h) }    // Height
            safeWrite { outputStream?.writeInt(ENCODING_RAW) } // Encoding type

            // Send pixel data with safe operation
            captureAndSendScreen(w, h)

            // Safe flush
            safeFlush()
            logD( "Framebuffer update sent: ${w}x${h}")
        } catch (e: Exception) {
            logE( "Error sending framebuffer update: ${e.message}")
            // Don't rethrow to prevent cascade failures
        }
    }

    private fun captureAndSendScreen(targetWidth: Int, targetHeight: Int) {
        try {
            // Check if we should stop before processing
            if (shouldStopGracefully.get() || !isRunning.get()) {
                logD( "Stop requested, skipping screen capture")
                return
            }
            
            // Log phone screen info for debugging
            logD( "Phone screen: ${screenWidth}x${screenHeight}")
            logD( "Target dimensions: ${targetWidth}x${targetHeight}")
            logD( "Aspect ratios - Phone: ${screenWidth.toFloat()/screenHeight}, Target: ${targetWidth.toFloat()/targetHeight}")

            // Try to get the most recent image first
            var image = imageReader?.acquireLatestImage()

            // If no latest image, try a few times with minimal delay
            var attempts = 0
            val maxAttempts = 3

            while (image == null && attempts < maxAttempts) {
                Thread.sleep(5)
                image = imageReader?.acquireLatestImage()
                attempts++
            }

            if (image == null) {
                image = imageReader?.acquireNextImage()
            }

            if (image == null) {
                // "No fresh image available")
                if (lastScreenData != null && lastScreenWidth == targetWidth && lastScreenHeight == targetHeight) {
                    logD( "Using cached screen data")
                    safeWrite { outputStream?.write(lastScreenData!!) }
                    return
                } else {
                    // "No cached screen data available, sending test pattern")
                    sendTestPattern(targetWidth, targetHeight)
                    return
                }
            }

            image.use { img ->
                val planes = img.planes
                if (planes.isEmpty()) {
                    // "No image planes available")
                    if (lastScreenData != null) {
                        safeWrite { outputStream?.write(lastScreenData!!) }
                    } else {
                        sendTestPattern(targetWidth, targetHeight)
                    }
                    return
                }

                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * screenWidth

                logD( "Image buffer info - stride: $rowStride, pixel: $pixelStride, padding: $rowPadding")
                logD( "Buffer capacity: ${buffer.capacity()}, remaining: ${buffer.remaining()}")

                // Create bitmap from image buffer with improved handling
                val bitmap = createBitmapFromBufferImproved(buffer, rowStride, pixelStride)
                if (bitmap == null) {
                    if (lastScreenData != null) {
                        safeWrite { outputStream?.write(lastScreenData!!) }
                    } else {
                        sendTestPattern(targetWidth, targetHeight)
                    }
                    return
                }

                logD( "Created bitmap: ${bitmap.width}x${bitmap.height}")

                // Calculate proper scaling to maintain aspect ratio
                val phoneAspect = screenWidth.toFloat() / screenHeight
                val targetAspect = targetWidth.toFloat() / targetHeight

                val scaledBitmap = if (abs(phoneAspect - targetAspect) > 0.1f) {
                    // Aspect ratios don't match - use proper scaling with letterboxing
                    logD( "Aspect ratio mismatch, using letterbox scaling")
                    createLetterboxedBitmap(bitmap, targetWidth, targetHeight)
                } else {
                    // Aspect ratios match - direct scaling
                    logD( "Aspect ratios match, using direct scaling")
                    Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                }

                logD( "Scaled bitmap: ${scaledBitmap.width}x${scaledBitmap.height}")

                // Convert to RGBA and send
                val rgbaData = bitmapToRgbBytes(scaledBitmap)

                // Cache the screen data for future use
                lastScreenData = rgbaData
                lastScreenWidth = targetWidth
                lastScreenHeight = targetHeight

                safeWrite { outputStream?.write(rgbaData) }

                bitmap.recycle()
                scaledBitmap.recycle()

                logD( "Screen captured and sent: ${rgbaData.size} bytes")
            }
        } catch (e: Exception) {
            logE( "Error capturing screen: ${e.message}")
            sendTestPattern(targetWidth, targetHeight)
        }
    }

    private fun createBitmapFromBufferImproved(buffer: ByteBuffer, rowStride: Int, pixelStride: Int): Bitmap? {
        try {
            logD( "Creating bitmap from buffer - capacity: ${buffer.capacity()}")

            val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(screenWidth * screenHeight)

            buffer.rewind()

            // Handle different pixel formats more carefully
            when (pixelStride) {
                4 -> {
                    // Standard RGBA format
                    for (row in 0 until screenHeight) {
                        for (col in 0 until screenWidth) {
                            val pixelPos = row * rowStride + col * pixelStride

                            if (pixelPos + 3 >= buffer.capacity()) {
                                // "Buffer overflow at pixel ($col,$row), using black")
                                pixels[row * screenWidth + col] = 0xFF000000.toInt()
                                continue
                            }

                            val r = buffer.get(pixelPos).toInt() and 0xFF
                            val g = buffer.get(pixelPos + 1).toInt() and 0xFF
                            val b = buffer.get(pixelPos + 2).toInt() and 0xFF
                            val a = buffer.get(pixelPos + 3).toInt() and 0xFF

                            pixels[row * screenWidth + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                }
                1 -> {
                    // Packed format - might need different handling
                    // "Packed pixel format detected, using alternative method")
                    var bufferIndex = 0
                    for (row in 0 until screenHeight) {
                        val rowStart = row * rowStride
                        for (col in 0 until screenWidth) {
                            val pixelPos = rowStart + col * 4 // Assume 4 bytes per pixel

                            if (pixelPos + 3 >= buffer.capacity()) {
                                pixels[row * screenWidth + col] = 0xFF000000.toInt()
                                continue
                            }

                            val pixel = buffer.getInt(pixelPos)
                            // Handle different byte orders
                            val r = (pixel shr 0) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = (pixel shr 16) and 0xFF
                            val a = (pixel shr 24) and 0xFF

                            pixels[row * screenWidth + col] = (a shl 24) or (r shl 16) or (g shl 8) or b
                        }
                    }
                }
                else -> {
                    logE( "Unsupported pixel stride: $pixelStride")
                    return null
                }
            }

            bitmap.setPixels(pixels, 0, screenWidth, 0, 0, screenWidth, screenHeight)
            logD( "Successfully created bitmap: ${bitmap.width}x${bitmap.height}")
            return bitmap

        } catch (e: Exception) {
            logE( "Error creating improved bitmap from buffer: ${e.message}", e)
            return null
        }
    }

    private fun createLetterboxedBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceAspect = source.width.toFloat() / source.height
        val targetAspect = targetWidth.toFloat() / targetHeight

        val scaledWidth: Int
        val scaledHeight: Int

        if (sourceAspect > targetAspect) {
            // Source is wider - fit to width
            scaledWidth = targetWidth
            scaledHeight = (targetWidth / sourceAspect).toInt()
        } else {
            // Source is taller - fit to height
            scaledHeight = targetHeight
            scaledWidth = (targetHeight * sourceAspect).toInt()
        }

        logD( "Letterbox scaling: ${source.width}x${source.height} -> ${scaledWidth}x${scaledHeight} in ${targetWidth}x${targetHeight}")

        // Create the target bitmap with black background
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.BLACK)

        // Scale source image
        val scaledSource = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        // Center the scaled image
        val left = (targetWidth - scaledWidth) / 2
        val top = (targetHeight - scaledHeight) / 2

        canvas.drawBitmap(scaledSource, left.toFloat(), top.toFloat(), null)

        scaledSource.recycle()
        return result
    }

    private fun bitmapToRgbBytes(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Use 32-bit format (4 bytes per pixel: RGBA)
        val rgbaBytes = ByteArray(width * height * 4)
        var index = 0

        for (pixel in pixels) {
            rgbaBytes[index++] = ((pixel shr 16) and 0xFF).toByte() // Red
            rgbaBytes[index++] = ((pixel shr 8) and 0xFF).toByte()  // Green
            rgbaBytes[index++] = (pixel and 0xFF).toByte()          // Blue
            rgbaBytes[index++] = ((pixel shr 24) and 0xFF).toByte() // Alpha
        }

        return rgbaBytes
    }

    private fun sendTestPattern(width: Int, height: Int) {
        try {
            // Use 32-bit format (4 bytes per pixel: RGBA)
            val rgbaData = ByteArray(width * height * 4)
            var index = 0

            for (y in 0 until height) {
                for (x in 0 until width) {
                    // Create a visible test pattern
                    val red = if (x < width / 3) 255.toByte() else 0.toByte()
                    val green = if (x >= width / 3 && x < 2 * width / 3) 255.toByte() else 0.toByte()
                    val blue = if (x >= 2 * width / 3) 255.toByte() else 0.toByte()

                    rgbaData[index++] = red     // Red
                    rgbaData[index++] = green   // Green
                    rgbaData[index++] = blue    // Blue
                    rgbaData[index++] = 255.toByte() // Alpha (fully opaque)
                }
            }

            safeWrite { outputStream?.write(rgbaData) }
            logD( "Test pattern sent: ${rgbaData.size} bytes (${width}x${height}x4)")
        } catch (e: Exception) {
            logE( "Error sending test pattern: ${e.message}")
        }
    }

    private fun handleKeyEvent() {
        try {
            inputStream?.skipBytes(1) // padding
            inputStream?.readInt() // key
            inputStream?.readInt() // down flag
        } catch (e: Exception) {
            logE( "Error handling key event: ${e.message}")
        }
    }

    private fun handlePointerEvent() {
        logD( "=== ENTERING handlePointerEvent ===")
        try {
            val buttonMask = inputStream?.readByte()?.toInt() ?: 0
            val x = inputStream?.readShort()?.toInt() ?: 0
            val y = inputStream?.readShort()?.toInt() ?: 0
            
            logD( "Received pointer event: ($x, $y) button=$buttonMask")
            logD( "Screen dimensions: ${screenWidth}x${screenHeight}")
            logD( "Last screen dimensions: ${lastScreenWidth}x${lastScreenHeight}")
            
            // Simulate touch using accessibility service
            simulateTouch(x, y, buttonMask)
            
        } catch (e: Exception) {
            logE( "Error handling pointer event: ${e.message}", e)
        }
        logD( "=== EXITING handlePointerEvent ===")
    }
    
    private fun simulateTouch(x: Int, y: Int, buttonMask: Int) {
        try {
            // Scale coordinates from VNC client to actual screen
            val scaledX = if (lastScreenWidth > 0) {
                (x.toFloat() / lastScreenWidth.toFloat() * screenWidth.toFloat())
            } else {
                x.toFloat()
            }
            val scaledY = if (lastScreenHeight > 0) {
                (y.toFloat() / lastScreenHeight.toFloat() * screenHeight.toFloat())
            } else {
                y.toFloat()
            }
            
            logD( "Simulating touch: original($x, $y) -> scaled($scaledX, $scaledY)")
            logD( "ButtonMask: $buttonMask, Screen: ${screenWidth}x${screenHeight}, LastScreen: ${lastScreenWidth}x${lastScreenHeight}")
            
            if (buttonMask == 1) {
                // Button pressed - perform touch
                val touchManager = TouchInputManager.getInstance()
                if (touchManager.isAccessibilityServiceEnabled()) {
                    logD( "Using accessibility service for touch at ($scaledX, $scaledY)")
                    touchManager.simulateTouch(scaledX, scaledY)
                } else {
                    // "Accessibility service not available, using fallback shell command")
                    // Fallback to shell command if accessibility service is not available
                    val command = "input tap ${scaledX.toInt()} ${scaledY.toInt()}"
                    try {
                        val process = Runtime.getRuntime().exec(command)
                        val exitCode = process.waitFor()
                        if (exitCode == 0) {
                            logD( "Shell command executed successfully: $command")
                        } else {
                            // "Shell command failed with exit code: $exitCode")
                        }
                    } catch (e: Exception) {
                        logE( "Shell command fallback failed: ${e.message}")
                    }
                }
                logD( "Touch executed at ($scaledX, $scaledY)")
            }
            // For buttonMask == 0 (button released), we don't need to do anything
            // since our touch simulation is instantaneous
            
        } catch (e: Exception) {
            logE( "Error simulating touch: ${e.message}", e)
        }
    }

    private fun handleClientCutText() {
        try {
            inputStream?.skipBytes(3) // padding
            val length = inputStream?.readInt() ?: 0
            inputStream?.skipBytes(length)
        } catch (e: Exception) {
            logE( "Error handling client cut text: ${e.message}")
        }
    }

    fun stop() {
        logD( "Stopping VNC server...")
        
        // Signal graceful stop to clients
        shouldStopGracefully.set(true)
        
        // Notify connected clients about disconnection
        if (isClientConnected.get()) {
            sendDisconnectionNotice()
        }
        
        isRunning.set(false)
        
        // Give clients a moment to disconnect gracefully
        try {
            Thread.sleep(1000) // Increased to 1 second for better cleanup
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        
        cleanup()
        logD( "VNC server stopped")
    }

    private fun cleanup() {
        try {
            // Close all connected clients first
            synchronized(connectedClients) {
                connectedClients.forEach { socket ->
                    try {
                        if (!socket.isClosed) {
                            socket.close()
                        }
                    } catch (e: Exception) {
                        // "Error closing client socket: ${e.message}")
                    }
                }
                connectedClients.clear()
            }
            
            // Interrupt all client threads
            clientThreads.forEach { thread ->
                try {
                    thread.interrupt()
                } catch (e: Exception) {
                    // "Error interrupting client thread: ${e.message}")
                }
            }
            clientThreads.clear()
            
            // Clean up current client
            cleanupClient()
            
            // Close server socket with proper cleanup
            serverSocket?.let { socket ->
                try {
                    // Force immediate close 
                    socket.close()
                } catch (e: Exception) {
                    // "Error closing server socket: ${e.message}")
                }
            }
            serverSocket = null
            
            // WAIT A MOMENT FOR PORT TO BE FULLY RELEASED
            try {
                Thread.sleep(500) // Additional delay for port cleanup
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            
            // RESET ALL STATE FOR CLEAN RESTART
            isRunning.set(false)
            shouldStopGracefully.set(false)
            isClientConnected.set(false)
            
        } catch (e: Exception) {
            logE( "Error during cleanup: ${e.message}", e)
        }
    }
    
    // Safe write operations to prevent broken pipe errors
    private fun safeWrite(writeOperation: () -> Unit): Boolean {
        return try {
            if (!isConnectionValid()) {
                return false
            }
            writeOperation()
            true
        } catch (e: java.net.SocketException) {
            logD( "Socket closed during write: ${e.message}")
            false
        } catch (e: IOException) {
            logD( "IO error during write: ${e.message}")
            false
        } catch (e: Exception) {
            logE( "Unexpected error during write: ${e.message}")
            false
        }
    }
    
    private fun safeFlush(): Boolean {
        return try {
            if (!isConnectionValid()) {
                return false
            }
            outputStream?.flush()
            true
        } catch (e: java.net.SocketException) {
            logD( "Socket closed during flush: ${e.message}")
            false
        } catch (e: IOException) {
            logD( "IO error during flush: ${e.message}")
            false
        } catch (e: Exception) {
            logE( "Unexpected error during flush: ${e.message}")
            false
        }
    }
    
    private fun sendDisconnectionNotice() {
        try {
            logD( "Sending disconnection notice to client...")
            // Try to send a final frame update to signal disconnection
            if (isConnectionValid()) {
                // Send a final "server disconnecting" message
                safeFlush()
            }
        } catch (e: Exception) {
            logD( "Could not send disconnection notice: ${e.message}")
        }
    }
    
    private fun isConnectionValid(): Boolean {
        return try {
            val socket = clientSocket
            socket != null && 
            !socket.isClosed && 
            socket.isConnected && 
            outputStream != null && 
            inputStream != null &&
            isRunning.get()
        } catch (e: Exception) {
            false
        }
    }

    private fun cleanupClient() {
        try {
            outputStream?.close()
            inputStream?.close()
            clientSocket?.close()
            outputStream = null
            inputStream = null
            clientSocket = null
        } catch (e: Exception) {
            logE( "Error cleaning up client: ${e.message}")
        }
    }
    
    // Overloaded version for specific client cleanup
    private fun cleanupClient(outputStream: DataOutputStream?, inputStream: DataInputStream?, socket: Socket?) {
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            logE( "Error cleaning up specific client: ${e.message}")
        }
    }
}