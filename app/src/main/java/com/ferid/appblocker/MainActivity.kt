package com.ferid.appblocker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.VpnService
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private data class AppEntry(val label: String, val pkg: String, val icon: Drawable)

    private lateinit var masterSwitch: Switch
    private lateinit var adapter: AppAdapter
    private var allApps: List<AppEntry> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        masterSwitch = findViewById(R.id.masterSwitch)
        masterSwitch.isChecked = Prefs.isEnabled(this)
        masterSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked != Prefs.isEnabled(this)) {
                Prefs.setEnabled(this, checked)
                applyRules()
            }
        }

        adapter = AppAdapter()
        val list = findViewById<ListView>(R.id.appList)
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val app = adapter.getItem(position)
            val nowBlocked = app.pkg !in Prefs.blocked(this)
            Prefs.setBlocked(this, app.pkg, nowBlocked)
            adapter.notifyDataSetChanged()
            applyRules()
        }

        findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = adapter.filter(s?.toString().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        // The VPN may have been turned off from system settings.
        masterSwitch.isChecked = Prefs.isEnabled(this)
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                .asSequence()
                .filter { it.packageName != packageName }
                .filter { it.requestedPermissions?.contains(Manifest.permission.INTERNET) == true }
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .mapNotNull { info ->
                    val ai = info.applicationInfo ?: return@mapNotNull null
                    AppEntry(ai.loadLabel(pm).toString(), info.packageName, ai.loadIcon(pm))
                }
                .toList()
            val blocked = Prefs.blocked(this)
            val sorted = apps.sortedWith(
                compareBy<AppEntry>({ it.pkg !in blocked }, { it.label.lowercase() })
            )
            runOnUiThread {
                allApps = sorted
                adapter.filter("")
            }
        }.start()
    }

    private fun applyRules() {
        if (Prefs.isEnabled(this)) {
            val consent = VpnService.prepare(this)
            if (consent != null) {
                startActivityForResult(consent, REQ_VPN)
            } else {
                startService(Intent(this, BlockerVpnService::class.java))
            }
        } else {
            startService(
                Intent(this, BlockerVpnService::class.java).setAction(BlockerVpnService.ACTION_STOP)
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_VPN) return
        if (resultCode == RESULT_OK) {
            startService(Intent(this, BlockerVpnService::class.java))
        } else {
            Toast.makeText(this, "VPN permission is required to block apps", Toast.LENGTH_LONG).show()
            Prefs.setEnabled(this, false)
            masterSwitch.isChecked = false
        }
    }

    private inner class AppAdapter : BaseAdapter() {
        private var shown: List<AppEntry> = emptyList()

        fun filter(query: String) {
            val q = query.trim().lowercase()
            shown = if (q.isEmpty()) allApps
            else allApps.filter { q in it.label.lowercase() || q in it.pkg.lowercase() }
            notifyDataSetChanged()
        }

        override fun getCount() = shown.size
        override fun getItem(position: Int) = shown[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_app, parent, false)
            val app = shown[position]
            view.findViewById<ImageView>(R.id.icon).setImageDrawable(app.icon)
            view.findViewById<TextView>(R.id.name).text = app.label
            view.findViewById<TextView>(R.id.pkg).text = app.pkg
            view.findViewById<CheckBox>(R.id.blocked).isChecked =
                app.pkg in Prefs.blocked(this@MainActivity)
            return view
        }
    }

    companion object {
        private const val REQ_VPN = 1
    }
}
