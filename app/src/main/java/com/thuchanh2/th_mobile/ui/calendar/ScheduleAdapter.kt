package com.thuchanh2.th_mobile.ui.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject

class ScheduleAdapter(
    private val onItemClick: ((StudyScheduleWithSubject) -> Unit)? = null
) : ListAdapter<StudyScheduleWithSubject, ScheduleAdapter.ScheduleViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSubjectCode: TextView = itemView.findViewById(R.id.tvSubjectCode)
        private val tvSubjectName: TextView = itemView.findViewById(R.id.tvSubjectName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)

        fun bind(item: StudyScheduleWithSubject) {
            tvSubjectCode.text = item.subjectCode
            tvSubjectName.text = item.subjectName
            tvDate.text = itemView.context.getString(R.string.schedule_date_label, item.studyDate)
            tvTime.text = "⏰ ${item.startTime} - ${item.endTime}"
            tvLocation.text = "📍 ${item.location}"

            val context = itemView.context
            when (item.status.lowercase()) {
                "confirmed", "đã xác nhận" -> {
                    tvStatus.text = context.getString(R.string.status_confirmed)
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_confirmed)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_confirmed))
                }
                "pending", "đợi duyệt" -> {
                    tvStatus.text = context.getString(R.string.status_pending)
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_pending)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending))
                }
                else -> { // Cancelled
                    tvStatus.text = context.getString(R.string.status_cancelled)
                    tvStatus.background = ContextCompat.getDrawable(context, R.drawable.bg_status_cancelled)
                    tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_cancelled))
                }
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<StudyScheduleWithSubject>() {
        override fun areItemsTheSame(
            oldItem: StudyScheduleWithSubject,
            newItem: StudyScheduleWithSubject
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: StudyScheduleWithSubject,
            newItem: StudyScheduleWithSubject
        ): Boolean {
            return oldItem == newItem
        }
    }
}
