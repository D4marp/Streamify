package com.app.streamify

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class TouchAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "TouchAccessibilityService"
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "TouchAccessibilityService connected")
        
        // Configure the accessibility service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_TOUCH_INTERACTION_START or
                        AccessibilityEvent.TYPE_TOUCH_INTERACTION_END or
                        AccessibilityEvent.TYPE_GESTURE_DETECTION_START or
                        AccessibilityEvent.TYPE_GESTURE_DETECTION_END
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                   AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                   AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // Register with TouchInputManager
        TouchInputManager.getInstance().setAccessibilityService(this)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle accessibility events for our use case
        // This service is primarily used for gesture dispatching
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "TouchAccessibilityService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TouchAccessibilityService destroyed")
        TouchInputManager.getInstance().setAccessibilityService(null)
    }
}
