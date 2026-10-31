package com.thuchanh2.th_mobile.ui.notification

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.entity.NotificationItem

class NotificationAdapter(
    private val onItemClick: ((NotificationItem) -> Unit)? = null
) : ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIconEmoji: TextView = itemView.findViewById(R.id.tvIconEmoji)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val vUnreadDot: View = itemView.findViewById(R.id.vUnreadDot)

        fun bind(item: NotificationItem) {
            tvTitle.text = item.title
            tvMessage.text = item.message
            vUnreadDot.isVisible = !item.isRead

            tvIconEmoji.text = when (item.type.uppercase()) {
                "UPCOMING" -> "⏰"
                "CHANGE" -> "🔄"
                else -> "🔔"
            }

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                item.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            tvTime.text = relativeTime

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(
            oldItem: NotificationItem,
            newItem: NotificationItem
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: NotificationItem,
            newItem: NotificationItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}
