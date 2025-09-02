package com.app.streamify

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TouchInputManager private constructor() {
    companion object {
        private const val TAG = "TouchInputManager"
        
        @Volatile
        private var INSTANCE: TouchInputManager? = null
        
        fun getInstance(): TouchInputManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TouchInputManager().also { INSTANCE = it }
            }
        }
    }
    
    private var accessibilityService: TouchAccessibilityService? = null
    private val executor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(100)
    )
    
    fun setAccessibilityService(service: TouchAccessibilityService?) {
        this.accessibilityService = service
        Log.d(TAG, "Accessibility service set: ${service != null}")
    }
    
    fun simulateTouch(x: Float, y: Float, duration: Long = 50L) {
        Log.d(TAG, "=== simulateTouch called: ($x, $y) ===")
        executor.submit {
            try {
                Log.d(TAG, "Executing touch simulation at ($x, $y)")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val service = accessibilityService
                    if (service != null) {
                        Log.d(TAG, "Using accessibility service for touch gesture")
                        val path = Path().apply {
                            moveTo(x, y)
                        }
                        
                        val gestureBuilder = GestureDescription.Builder()
                        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
                        gestureBuilder.addStroke(strokeDescription)
                        
                        val gesture = gestureBuilder.build()
                        val result = service.dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                Log.d(TAG, "Touch gesture completed at ($x, $y)")
                            }
                            
                            override fun onCancelled(gestureDescription: GestureDescription?) {
                                Log.w(TAG, "Touch gesture cancelled at ($x, $y)")
                            }
                        }, null)
                        
                        if (!result) {
                            Log.e(TAG, "Failed to dispatch touch gesture at ($x, $y)")
                            fallbackToShellCommand(x.toInt(), y.toInt())
                        } else {
                            Log.d(TAG, "Touch gesture dispatched successfully at ($x, $y)")
                        }
                    } else {
                        Log.w(TAG, "Accessibility service not available for touch at ($x, $y)")
                        // Fallback to shell command
                        fallbackToShellCommand(x.toInt(), y.toInt())
                    }
                } else {
                    Log.w(TAG, "Gesture API not available (API < 24), using fallback")
                    fallbackToShellCommand(x.toInt(), y.toInt())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error simulating touch: ${e.message}", e)
                fallbackToShellCommand(x.toInt(), y.toInt())
            }
        }
    }
    
    private fun fallbackToShellCommand(x: Int, y: Int) {
        try {
            // Try different shell commands based on Android version
            val commands = listOf(
                "input tap $x $y",
                "am broadcast -a android.intent.action.INPUT_TAP --ei x $x --ei y $y",
                "sendevent /dev/input/event0 3 0 $x && sendevent /dev/input/event0 3 1 $y && sendevent /dev/input/event0 1 330 1 && sendevent /dev/input/event0 0 0 0 && sendevent /dev/input/event0 1 330 0 && sendevent /dev/input/event0 0 0 0"
            )
            
            var success = false
            for (command in commands) {
                try {
                    Log.d(TAG, "Trying fallback command: $command")
                    val process = Runtime.getRuntime().exec(command)
                    val exitCode = process.waitFor()
                    
                    if (exitCode == 0) {
                        Log.d(TAG, "Shell command executed successfully: $command")
                        success = true
                        break
                    } else {
                        Log.w(TAG, "Shell command failed with exit code: $exitCode for: $command")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Command failed: $command, error: ${e.message}")
                    continue
                }
            }
            
            if (!success) {
                Log.e(TAG, "All fallback shell commands failed. Please ensure Accessibility Service is enabled.")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Fallback shell command failed: ${e.message}")
        }
    }
    
    fun simulateDrag(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 200L) {
        executor.submit {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val service = accessibilityService
                    if (service != null) {
                        val path = Path().apply {
                            moveTo(startX, startY)
                            lineTo(endX, endY)
                        }
                        
                        val gestureBuilder = GestureDescription.Builder()
                        val strokeDescription = GestureDescription.StrokeDescription(path, 0, duration)
                        gestureBuilder.addStroke(strokeDescription)
                        
                        val gesture = gestureBuilder.build()
                        service.dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                            override fun onCompleted(gestureDescription: GestureDescription?) {
                                Log.d(TAG, "Drag gesture completed from ($startX, $startY) to ($endX, $endY)")
                            }
                            
                            override fun onCancelled(gestureDescription: GestureDescription?) {
                                Log.w(TAG, "Drag gesture cancelled")
                            }
                        }, null)
                    } else {
                        Log.w(TAG, "Accessibility service not available for drag")
                        // Fallback for drag
                        fallbackToDragCommand(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt(), duration)
                    }
                } else {
                    fallbackToDragCommand(startX.toInt(), startY.toInt(), endX.toInt(), endY.toInt(), duration)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error simulating drag: ${e.message}")
            }
        }
    }
    
    private fun fallbackToDragCommand(startX: Int, startY: Int, endX: Int, endY: Int, duration: Long) {
        try {
            val command = "input swipe $startX $startY $endX $endY $duration"
            Log.d(TAG, "Executing drag command: $command")
            
            val process = Runtime.getRuntime().exec(command)
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Log.d(TAG, "Drag command executed successfully")
            } else {
                Log.w(TAG, "Drag command failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Drag command failed: ${e.message}")
        }
    }
    
    fun isAccessibilityServiceEnabled(): Boolean {
        return accessibilityService != null
    }
}
