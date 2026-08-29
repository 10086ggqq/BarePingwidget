package com.example.barepingwidget.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.barepingwidget.R
import com.example.barepingwidget.data.Site
import com.example.barepingwidget.data.SiteRepository
import com.example.barepingwidget.monitor.MonitorScheduler
import com.example.barepingwidget.widget.PingWidgetProvider
import com.google.android.material.switchmaterial.SwitchMaterial

/** 设置页：通知开关 + 自定义监测站点（添加 / 移除） */
class SettingsActivity : AppCompatActivity() {

    private lateinit var repo: SiteRepository
    private lateinit var siteAdapter: SiteManageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        repo = SiteRepository.getInstance(this)

        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }

        // ---- 通知开关 ----
        val switchNotify = findViewById<SwitchMaterial>(R.id.switch_notify)
        switchNotify.isChecked = repo.notificationsEnabled
        switchNotify.setOnCheckedChangeListener { _, isChecked ->
            repo.notificationsEnabled = isChecked
        }

        // ---- 站点列表 ----
        siteAdapter = SiteManageAdapter(
            sites = repo.getSites().toMutableList(),
            onRemove = { site ->
                repo.removeSite(site.name)
                siteAdapter.remove(site)
                PingWidgetProvider.updateAllWidgets(this)
            }
        )
        findViewById<RecyclerView>(R.id.site_list).apply {
            layoutManager = LinearLayoutManager(this@SettingsActivity)
            adapter = siteAdapter
        }

        // ---- 添加站点 ----
        val inputName = findViewById<EditText>(R.id.input_name)
        val inputUrl = findViewById<EditText>(R.id.input_url)
        findViewById<Button>(R.id.btn_add).setOnClickListener {
            val name = inputName.text.toString().trim()
            var url = inputUrl.text.toString().trim()

            if (name.isEmpty()) {
                toast(getString(R.string.settings_hint_name_empty)); return@setOnClickListener
            }
            if (url.isEmpty()) {
                url = "https://$name/favicon.ico"
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }
            if (siteAdapter.contains(name)) {
                toast(getString(R.string.settings_hint_duplicate)); return@setOnClickListener
            }

            val site = Site(name, url)
            repo.addSite(site)
            siteAdapter.add(site)
            PingWidgetProvider.updateAllWidgets(this)
            inputName.text.clear()
            inputUrl.text.clear()
            toast(getString(R.string.settings_added, name))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // ---------- 站点管理列表适配器 ----------

    private class SiteManageAdapter(
        private val sites: MutableList<Site>,
        private val onRemove: (Site) -> Unit
    ) : RecyclerView.Adapter<SiteManageAdapter.VH>() {

        fun contains(name: String) = sites.any { it.name == name }

        fun add(site: Site) {
            sites.add(site)
            notifyItemInserted(sites.size - 1)
        }

        fun remove(site: Site) {
            val index = sites.indexOfFirst { it.name == site.name }
            if (index >= 0) {
                sites.removeAt(index)
                notifyItemRemoved(index)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_site_manage, parent, false)
            return VH(view)
        }

        override fun getItemCount() = sites.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(sites[position], onRemove)
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val name: TextView = view.findViewById(R.id.site_name)
            private val url: TextView = view.findViewById(R.id.site_url)
            private val remove: ImageButton = view.findViewById(R.id.btn_remove)

            fun bind(site: Site, onRemove: (Site) -> Unit) {
                name.text = site.name
                url.text = site.url
                remove.setOnClickListener { onRemove(site) }
            }
        }
    }
}
