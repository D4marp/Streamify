package com.app.streamify

import android.graphics.Bitmap
import android.util.Log
import java.io.*
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class VncClient(private val serverIp: String, private val serverPort: Int) {
    companion object {
        private const val TAG = "VncClient"
        private const val VNC_VERSION = "RFB 003.008\n"
        private const val AUTH_NONE = 1
    }

    private var socket: Socket? = null
    private var input: DataInputStream? = null
    private var output: DataOutputStream? = null
    private val isConnected = AtomicBoolean(false)
    
    private var serverWidth = 0
    private var serverHeight = 0
    private var bitsPerPixel = 0
    private var depth = 0
    private var bigEndianFlag = 0
    private var trueColorFlag = 0
    
    interface VncClientListener {
        fun onFrameReceived(bitmap: Bitmap)
        fun onConnectionStateChanged(connected: Boolean)
        fun onError(error: String)
    }
    
    private var listener: VncClientListener? = null
    
    fun setListener(listener: VncClientListener) {
        this.listener = listener
    }

    fun connect(): Boolean {
        return try {
            Log.d(TAG, "Connecting to VNC server at $serverIp:$serverPort")
            socket = Socket(serverIp, serverPort)
            socket?.tcpNoDelay = true
            
            input = DataInputStream(BufferedInputStream(socket?.getInputStream()))
            output = DataOutputStream(BufferedOutputStream(socket?.getOutputStream()))
            
            if (performVncHandshake()) {
                isConnected.set(true)
                listener?.onConnectionStateChanged(true)
                Log.d(TAG, "Successfully connected to VNC server")
                true
            } else {
                disconnect()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}")
            listener?.onError("Connection failed: ${e.message}")
            false
        }
    }
    
    private fun performVncHandshake(): Boolean {
        return try {
            // Read server version
            val serverVersionBytes = ByteArray(12)
            input?.readFully(serverVersionBytes)
            val serverVersion = String(serverVersionBytes).trim()
            Log.d(TAG, "Server version: $serverVersion")
            
            // Send client version
            output?.writeBytes(VNC_VERSION)
            output?.flush()
            
            // Read number of auth types
            val numAuthTypes = input?.readByte()?.toInt() ?: 0
            Log.d(TAG, "Number of auth types: $numAuthTypes")
            
            if (numAuthTypes == 0) {
                Log.e(TAG, "Server rejected connection")
                return false
            }
            
            // Read auth types
            var foundNoAuth = false
            for (i in 0 until numAuthTypes) {
                val authType = input?.readByte()?.toInt() ?: 0
                if (authType == AUTH_NONE) {
                    foundNoAuth = true
                }
                Log.d(TAG, "Auth type: $authType")
            }
            
            if (!foundNoAuth) {
                Log.e(TAG, "No authentication not supported")
                return false
            }
            
            // Choose no authentication
            output?.writeByte(AUTH_NONE)
            output?.flush()
            
            // Read auth result
            val authResult = input?.readInt() ?: -1
            if (authResult != 0) {
                Log.e(TAG, "Authentication failed: $authResult")
                return false
            }
            
            // Send client init (shared flag = 1)
            output?.writeByte(1)
            output?.flush()
            
            // Read server init
            readServerInit()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Handshake failed: ${e.message}")
            false
        }
    }
    
    private fun readServerInit() {
        try {
            serverWidth = input?.readShort()?.toInt() ?: 0
            serverHeight = input?.readShort()?.toInt() ?: 0
            
            bitsPerPixel = input?.readByte()?.toInt() ?: 0
            depth = input?.readByte()?.toInt() ?: 0
            bigEndianFlag = input?.readByte()?.toInt() ?: 0
            trueColorFlag = input?.readByte()?.toInt() ?: 0
            
            val redMax = input?.readShort()?.toInt() ?: 0
            val greenMax = input?.readShort()?.toInt() ?: 0
            val blueMax = input?.readShort()?.toInt() ?: 0
            val redShift = input?.readByte()?.toInt() ?: 0
            val greenShift = input?.readByte()?.toInt() ?: 0
            val blueShift = input?.readByte()?.toInt() ?: 0
            
            // Skip padding
            input?.skipBytes(3)
            
            val nameLength = input?.readInt() ?: 0
            val nameBytes = ByteArray(nameLength)
            input?.readFully(nameBytes)
            val serverName = String(nameBytes)
            
            Log.d(TAG, "Server: $serverName, ${serverWidth}x${serverHeight}, ${bitsPerPixel}bpp")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading server init: ${e.message}")
            throw e
        }
    }

    fun requestFramebufferUpdate() {
        if (!isConnected.get()) return
        
        try {
            // Send framebuffer update request
            output?.writeByte(3) // Message type
            output?.writeByte(0) // Incremental (0 = full update)
            output?.writeShort(0) // X
            output?.writeShort(0) // Y
            output?.writeShort(serverWidth) // Width
            output?.writeShort(serverHeight) // Height
            output?.flush()
            
            Log.d(TAG, "Requested framebuffer update")
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting framebuffer update: ${e.message}")
            listener?.onError("Error requesting update: ${e.message}")
        }
    }

    fun receiveFrame(): Bitmap? {
        if (!isConnected.get()) return null
        
        return try {
            // Read message type
            val messageType = input?.readByte()?.toInt() ?: return null
            
            if (messageType == 0) { // Framebuffer update
                input?.readByte() // Padding
                val numRects = input?.readShort()?.toInt() ?: 0
                
                Log.d(TAG, "Receiving framebuffer update with $numRects rectangles")
                
                for (i in 0 until numRects) {
                    val x = input?.readShort()?.toInt() ?: 0
                    val y = input?.readShort()?.toInt() ?: 0
                    val w = input?.readShort()?.toInt() ?: 0
                    val h = input?.readShort()?.toInt() ?: 0
                    val encoding = input?.readInt() ?: 0
                    
                    Log.d(TAG, "Rectangle $i: $x,$y ${w}x$h encoding=$encoding")
                    
                    if (encoding == 0) { // RAW encoding
                        return readRawRectangle(w, h)
                    } else {
                        Log.w(TAG, "Unsupported encoding: $encoding")
                        // Skip the rectangle data
                        val bytesToSkip = w * h * (bitsPerPixel / 8)
                        input?.skipBytes(bytesToSkip)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving frame: ${e.message}")
            listener?.onError("Error receiving frame: ${e.message}")
            null
        }
    }
    
    private fun readRawRectangle(width: Int, height: Int): Bitmap? {
        return try {
            val bytesPerPixel = bitsPerPixel / 8
            val pixelData = ByteArray(width * height * bytesPerPixel)
            input?.readFully(pixelData)
            
            // Convert to bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            
            var pixelIndex = 0
            for (i in 0 until width * height) {
                val offset = i * bytesPerPixel
                
                // Assume RGBA format (32-bit)
                if (bytesPerPixel == 4) {
                    val r = pixelData[offset].toInt() and 0xFF
                    val g = pixelData[offset + 1].toInt() and 0xFF
                    val b = pixelData[offset + 2].toInt() and 0xFF
                    val a = pixelData[offset + 3].toInt() and 0xFF
                    
                    pixels[pixelIndex++] = (a shl 24) or (r shl 16) or (g shl 8) or b
                } else {
                    // Fallback for other formats
                    pixels[pixelIndex++] = 0xFF000000.toInt() // Black with full alpha
                }
            }
            
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            
            Log.d(TAG, "Created bitmap from raw data: ${width}x${height}")
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error reading raw rectangle: ${e.message}")
            null
        }
    }

    // MIRROR ONLY MODE - POINTER EVENTS DISABLED
    fun sendPointerEvent(x: Int, y: Int, buttonMask: Int) {
        // Disabled for mirror-only mode
        // No pointer events are sent to maintain view-only functionality
        return
    }
    
    // MIRROR ONLY MODE - KEY EVENTS DISABLED  
    fun sendKeyEvent(key: Int, down: Boolean) {
        // Disabled for mirror-only mode
        // No key events are sent to maintain view-only functionality
        return
    }

    fun disconnect() {
        try {
            isConnected.set(false)
            input?.close()
            output?.close()
            socket?.close()
            listener?.onConnectionStateChanged(false)
            Log.d(TAG, "Disconnected from VNC server")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed: ${e.message}")
        }
    }
    
    fun isConnected(): Boolean = isConnected.get()
}
