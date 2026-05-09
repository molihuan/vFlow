package com.chaomixian.vflow.ui.shortcut_picker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chaomixian.vflow.R
import java.util.Locale

class ShortcutPickerAdapter(
    private val onItemClick: (ShortcutPickerItem) -> Unit
) : RecyclerView.Adapter<ShortcutPickerAdapter.ViewHolder>() {

    private var allItems: List<ShortcutPickerItem> = emptyList()
    private var displayItems: List<ShortcutPickerItem> = emptyList()

    fun submitList(items: List<ShortcutPickerItem>) {
        allItems = items
        displayItems = items
        notifyDataSetChanged()
    }

    fun filter(query: String?) {
        val normalized = query?.trim()?.lowercase(Locale.getDefault()).orEmpty()
        displayItems = if (normalized.isBlank()) {
            allItems
        } else {
            allItems.filter { item ->
                item.shortcutLabel.lowercase(Locale.getDefault()).contains(normalized) ||
                    item.appName.lowercase(Locale.getDefault()).contains(normalized) ||
                    item.packageName.lowercase(Locale.getDefault()).contains(normalized) ||
                    item.activityName.lowercase(Locale.getDefault()).contains(normalized)
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shortcut_picker, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = displayItems.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(displayItems[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.shortcut_app_icon)
        private val titleView: TextView = itemView.findViewById(R.id.shortcut_title)
        private val appNameView: TextView = itemView.findViewById(R.id.shortcut_app_name)
        private val packageView: TextView = itemView.findViewById(R.id.shortcut_package_name)

        fun bind(item: ShortcutPickerItem) {
            iconView.setImageDrawable(item.icon)
            titleView.text = item.shortcutLabel
            appNameView.text = item.appName
            packageView.text = item.packageName
            itemView.setOnClickListener { onItemClick(item) }
        }
    }
}
