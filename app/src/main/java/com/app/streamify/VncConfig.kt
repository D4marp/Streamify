package com.app.streamify

import android.content.Context
import android.content.SharedPreferences

class VncConfig(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "vnc_config"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_CLIENT_IP = "client_ip"
        private const val KEY_CLIENT_PORT = "client_port"
        private const val KEY_FRAME_RATE = "frame_rate"
        private const val KEY_IMAGE_QUALITY = "image_quality"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect"
        
        const val DEFAULT_PORT = 5901
        const val MIN_PORT = 1024
        const val MAX_PORT = 65535
        
        // Performance settings
        const val DEFAULT_FRAME_RATE = 25
        const val MIN_FRAME_RATE = 10
        const val MAX_FRAME_RATE = 60
        
        const val DEFAULT_IMAGE_QUALITY = 80
        const val MIN_IMAGE_QUALITY = 30
        const val MAX_IMAGE_QUALITY = 100
        
        // Common VNC ports
        val COMMON_PORTS = listOf(5901, 5902, 5903, 5904, 5905)
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Server configuration
    var serverPort: Int
        get() = prefs.getInt(KEY_SERVER_PORT, DEFAULT_PORT)
        set(value) {
            if (isValidPort(value)) {
                prefs.edit().putInt(KEY_SERVER_PORT, value).apply()
            }
        }
    
    // Client configuration
    var lastClientIp: String
        get() = prefs.getString(KEY_CLIENT_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CLIENT_IP, value).apply()
    
    var lastClientPort: Int
        get() = prefs.getInt(KEY_CLIENT_PORT, DEFAULT_PORT)
        set(value) {
            if (isValidPort(value)) {
                prefs.edit().putInt(KEY_CLIENT_PORT, value).apply()
            }
        }
    
    // Performance configuration
    var frameRate: Int
        get() = prefs.getInt(KEY_FRAME_RATE, DEFAULT_FRAME_RATE)
        set(value) {
            val validValue = value.coerceIn(MIN_FRAME_RATE, MAX_FRAME_RATE)
            prefs.edit().putInt(KEY_FRAME_RATE, validValue).apply()
        }
    
    var imageQuality: Int
        get() = prefs.getInt(KEY_IMAGE_QUALITY, DEFAULT_IMAGE_QUALITY)
        set(value) {
            val validValue = value.coerceIn(MIN_IMAGE_QUALITY, MAX_IMAGE_QUALITY)
            prefs.edit().putInt(KEY_IMAGE_QUALITY, validValue).apply()
        }
    
    var autoReconnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_RECONNECT, value).apply()

    fun isValidPort(port: Int): Boolean {
        return port in MIN_PORT..MAX_PORT
    }
    
    fun getNextAvailablePort(): Int {
        val current = serverPort
        return COMMON_PORTS.find { it > current } ?: (current + 1).coerceAtMost(MAX_PORT)
    }
    
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
