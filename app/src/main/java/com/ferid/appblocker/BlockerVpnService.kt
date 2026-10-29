package com.ferid.appblocker

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * A local "black hole" VPN. Only the blocked apps are routed into it, and
 * nothing ever reads the packets back out, so those apps have no internet.
 * All other apps bypass the VPN and keep working normally.
 * No traffic ever leaves the phone through this app.
 */
class BlockerVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            closeTun()
            stopSelf()
            return START_NOT_STICKY
        }
        applyRules()
        return START_STICKY
    }

    // Called by the system when started as an "Always-on VPN" (e.g. after reboot).
    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == SERVICE_INTERFACE && tun == null) {
            Prefs.setEnabled(this, true)
            applyRules()
        }
        return super.onBind(intent)
    }

    private fun applyRules() {
        val blocked = Prefs.blocked(this).filter { it != packageName }
        if (!Prefs.isEnabled(this) || blocked.isEmpty()) {
            // An empty allow-list would route EVERY app into the VPN, so stop instead.
            closeTun()
            stopSelf()
            return
        }

        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress("10.111.222.1", 32)
            .addAddress("fd00:111:222::1", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)

        var added = 0
        for (pkg in blocked) {
            try {
                builder.addAllowedApplication(pkg)
                added++
            } catch (e: PackageManager.NameNotFoundException) {
                // App was uninstalled; ignore it.
            }
        }
        if (added == 0) {
            closeTun()
            stopSelf()
            return
        }

        val newTun = try {
            builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "establish failed", e)
            null
        }
        if (newTun == null) {
            // VPN permission not granted (or revoked).
            closeTun()
            stopSelf()
            return
        }
        val old = tun
        tun = newTun
        old?.close()
    }

    private fun closeTun() {
        try {
            tun?.close()
        } catch (_: Exception) {
        }
        tun = null
    }

    override fun onRevoke() {
        // Another VPN took over or the user disconnected it from settings.
        Prefs.setEnabled(this, false)
        closeTun()
        stopSelf()
    }

    override fun onDestroy() {
        closeTun()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.ferid.appblocker.STOP"
        private const val TAG = "AppBlocker"
    }
}
