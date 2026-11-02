package com.parental.kuncigame

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class KunciGameService : AccessibilityService() {

    private val scriptUrl = "https://script.google.com/macros/s/AKfycbxYB6V5lAPVR9VOL2nArNR7zpwZkrHRh3je6YradwDk0O04hdXWDZYY_CCKAF-I8WSZDg/exec?action=getStatus"

    private val lockedPackages = mutableSetOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var isPolling = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        isPolling = true
        handler.post {
            Toast.makeText(applicationContext, "🟢 Layanan Agen Kunci Aktif!", Toast.LENGTH_SHORT).show()
        }
        mulaiPengecekanServerRutin()
    }

    private fun fetchJsonWithRedirect(urlString: String): String? {
        var currentUrl = urlString
        for (i in 0..3) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.instanceFollowRedirects = true

            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                status == HttpURLConnection.HTTP_MOVED_PERM || 
                status == 302 || status == 301) {
                val newUrl = conn.getHeaderField("Location")
                if (newUrl != null) {
                    currentUrl = newUrl
                    continue
                }
            }

            if (status == HttpURLConnection.HTTP_OK) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
            break
        }
        return null
    }

    private fun mulaiPengecekanServerRutin() {
        thread {
            while (isPolling) {
                try {
                    val responseText = fetchJsonWithRedirect(scriptUrl)
                    if (responseText != null) {
                        val json = JSONObject(responseText)
                        if (json.optString("status") == "success") {
                            val dataArray = json.getJSONArray("data")
                            val tempLocked = mutableSetOf<String>()

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val pkgName = item.optString("packageName")
                                val isLocked = item.optBoolean("isLocked", false)

                                if (isLocked && pkgName.isNotEmpty()) {
                                    tempLocked.add(pkgName.lowercase().trim())
                                }
                            }

                            synchronized(lockedPackages) {
                                lockedPackages.clear()
                                lockedPackages.addAll(tempLocked)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                Thread.sleep(3000)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val openedPackage = event.packageName?.toString()?.lowercase()?.trim() ?: return

        synchronized(lockedPackages) {
            if (lockedPackages.contains(openedPackage)) {
                performGlobalAction(GLOBAL_ACTION_HOME)

                handler.post {
                    Toast.makeText(
                        applicationContext,
                        "🔒 Game ini sedang dikunci oleh Orang Tua!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }
}
