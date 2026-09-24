package io.github.fabrizio3443.vertal

import android.app.NotificationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import java.security.KeyStore
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "vertal/privacy_verification",
        ).setMethodCallHandler { call, result ->
            val isDebuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (!isDebuggable) {
                result.notImplemented()
                return@setMethodCallHandler
            }
            when (call.method) {
                "activeNotificationIds" -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        result.success(emptyList<Int>())
                        return@setMethodCallHandler
                    }
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    result.success(manager.activeNotifications.map { it.id })
                }
                "secureStorageKeyAliases" -> {
                    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                    result.success(keyStore.aliases().toList())
                }
                else -> result.notImplemented()
            }
        }
    }
}
