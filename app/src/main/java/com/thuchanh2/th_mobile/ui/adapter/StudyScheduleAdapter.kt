package com.thuchanh2.th_mobile.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.entity.StudySchedule
import com.thuchanh2.th_mobile.data.entity.ScheduleStatus

class StudyScheduleAdapter(
    private val onCancelClick: (StudySchedule) -> Unit,
    private val onDeleteClick: (StudySchedule) -> Unit
) : ListAdapter<StudySchedule, StudyScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_study_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val btnCancel: Button = itemView.findViewById(R.id.btnCancel)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(schedule: StudySchedule) {
            tvDate.text = schedule.studyDate
            tvTime.text = "${schedule.startTime} - ${schedule.endTime}"
            tvLocation.text = schedule.location
            tvStatus.text = schedule.status

            when (schedule.status) {
                ScheduleStatus.PENDING.name -> tvStatus.setTextColor(Color.parseColor("#FF9800")) // Orange
                ScheduleStatus.CONFIRMED.name -> tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
                ScheduleStatus.CANCELLED.name -> tvStatus.setTextColor(Color.parseColor("#F44336")) // Red
            }

            btnCancel.visibility = if (schedule.status == ScheduleStatus.CANCELLED.name) View.GONE else View.VISIBLE
            
            btnCancel.setOnClickListener {
                onCancelClick(schedule)
            }
            
            btnDelete.setOnClickListener {
                onDeleteClick(schedule)
            }
        }
    }

    class ScheduleDiffCallback : DiffUtil.ItemCallback<StudySchedule>() {
        override fun areItemsTheSame(oldItem: StudySchedule, newItem: StudySchedule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StudySchedule, newItem: StudySchedule): Boolean {
            return oldItem == newItem
        }
    }
}
