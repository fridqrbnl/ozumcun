package com.ferid.appblocker

import android.content.Context

object Prefs {
    private const val FILE = "blocker"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_ENABLED = "enabled"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun blocked(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_BLOCKED, emptySet())!!.toSet()

    fun setBlocked(ctx: Context, pkg: String, block: Boolean) {
        val set = blocked(ctx).toMutableSet()
        if (block) set.add(pkg) else set.remove(pkg)
        prefs(ctx).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}
