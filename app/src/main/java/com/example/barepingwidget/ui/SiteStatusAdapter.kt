package com.example.barepingwidget.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.barepingwidget.R
import com.example.barepingwidget.data.PingStatus
import com.example.barepingwidget.data.SiteResult

/** 主界面状态列表适配器：还原 HTML 页面的「圆点 + 域名 + 延迟」行样式 */
class SiteStatusAdapter : RecyclerView.Adapter<SiteStatusAdapter.VH>() {

    private val items = mutableListOf<SiteResult>()
    private var testing = false

    fun submitList(results: List<SiteResult>) {
        testing = false
        items.clear()
        items.addAll(results)
        notifyDataSetChanged()
    }

    /** 全部置为"检测中"脉冲态 */
    fun markTesting(results: List<SiteResult>) {
        testing = true
        items.clear()
        items.addAll(results)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_site_status, parent, false)
        return VH(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], testing)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val dot: ImageView = view.findViewById(R.id.status_dot)
        private val name: TextView = view.findViewById(R.id.domain_name)
        private val latency: TextView = view.findViewById(R.id.latency)

        fun bind(item: SiteResult, testing: Boolean) {
            name.text = item.site.name
            val ctx = itemView.context

            if (testing || item.status == PingStatus.UNKNOWN) {
                dot.setImageResource(R.drawable.dot_pulse)
                latency.text = ctx.getString(R.string.testing)
                latency.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                return
            }

            val dotRes = when (item.status) {
                PingStatus.GREEN -> R.drawable.dot_green
                PingStatus.YELLOW -> R.drawable.dot_yellow
                PingStatus.RED -> R.drawable.dot_red
                PingStatus.UNKNOWN -> R.drawable.dot_unknown
            }
            dot.setImageResource(dotRes)

            when (item.status) {
                PingStatus.RED -> {
                    latency.text = "—"
                    latency.setTextColor(ContextCompat.getColor(ctx, R.color.status_red))
                }
                PingStatus.GREEN -> {
                    latency.text = "${item.latencyMs} ms"
                    latency.setTextColor(ContextCompat.getColor(ctx, R.color.status_green))
                }
                PingStatus.YELLOW -> {
                    latency.text = "${item.latencyMs} ms"
                    latency.setTextColor(ContextCompat.getColor(ctx, R.color.status_yellow))
                }
                PingStatus.UNKNOWN -> Unit
            }
        }
    }
}
