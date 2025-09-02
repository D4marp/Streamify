package com.app.streamify

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import android.util.Log

object RemoteControlHelper {
    private const val TAG = "RemoteControlHelper"
    
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val accessibilityEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            
            if (accessibilityEnabled == 1) {
                val service = "${context.packageName}/com.app.streamify.TouchAccessibilityService"
                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                    ) ?: ""
                )
                
                while (colonSplitter.hasNext()) {
                    val accessibilityService = colonSplitter.next()
                    if (accessibilityService.equals(service, ignoreCase = true)) {
                        Log.d(TAG, "Accessibility service is enabled: $service")
                        return true
                    }
                }
            }
            Log.w(TAG, "Accessibility service is not enabled")
            false
        } catch (e: Settings.SettingNotFoundException) {
            Log.e(TAG, "Error checking accessibility service: ${e.message}")
            false
        }
    }
    
    fun checkRemoteControlRequirements(context: Context): RemoteControlStatus {
        val accessibilityEnabled = isAccessibilityServiceEnabled(context)
        val touchManagerReady = TouchInputManager.getInstance().isAccessibilityServiceEnabled()
        
        return RemoteControlStatus(
            accessibilityServiceEnabled = accessibilityEnabled,
            touchManagerReady = touchManagerReady,
            isFullyReady = accessibilityEnabled && touchManagerReady
        )
    }
    
    data class RemoteControlStatus(
        val accessibilityServiceEnabled: Boolean,
        val touchManagerReady: Boolean,
        val isFullyReady: Boolean
    ) {
        fun getStatusMessage(): String {
            return when {
                isFullyReady -> "✅ Remote control ready"
                !accessibilityServiceEnabled -> "❌ Enable Accessibility Service in Settings"
                !touchManagerReady -> "⚠️ Accessibility Service connecting..."
                else -> "❌ Remote control not ready"
            }
        }
    }
}
