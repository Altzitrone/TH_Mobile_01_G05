package com.thuchanh2.th_mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.model.StudyScheduleWithSubject
import com.thuchanh2.th_mobile.databinding.RowBookingBinding

class StudyScheduleAdapter(
    private val onView: (Long) -> Unit,
    private val onEdit: (Long) -> Unit,
    private val onConfirm: (Long) -> Unit,
    private val onCancel: (Long) -> Unit,
    private val onDelete: (Long) -> Unit
) : ListAdapter<StudyScheduleWithSubject, StudyScheduleAdapter.Holder>(object : DiffUtil.ItemCallback<StudyScheduleWithSubject>() {
    override fun areItemsTheSame(oldItem: StudyScheduleWithSubject, newItem: StudyScheduleWithSubject) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: StudyScheduleWithSubject, newItem: StudyScheduleWithSubject) = oldItem == newItem
}) {
    class Holder(val binding: RowBookingBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
        Holder(RowBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        val context = holder.itemView.context
        holder.binding.apply {
            subjectName.text = row.subjectName
            subjectCode.text = row.subjectCode
            date.text = context.getString(R.string.schedule_date_label, row.studyDate)
            time.text = context.getString(R.string.time_range, row.startTime, row.endTime)
            location.text = row.location
            status.setText(when (row.status) {
                "CONFIRMED" -> R.string.status_confirmed
                "CANCELLED" -> R.string.status_cancelled
                else -> R.string.status_pending
            })
            confirmButton.isVisible = row.status != "CONFIRMED"
            cancelButton.isVisible = row.status != "CANCELLED"
            root.setOnClickListener { onView(row.id) }
            editButton.setOnClickListener { onEdit(row.id) }
            confirmButton.setOnClickListener { onConfirm(row.id) }
            cancelButton.setOnClickListener { onCancel(row.id) }
            deleteButton.setOnClickListener { onDelete(row.id) }
        }
    }
}
