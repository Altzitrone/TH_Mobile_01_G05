package com.thuchanh2.th_mobile.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thuchanh2.th_mobile.databinding.RowRecordBinding

data class RecordRow(val id: Long, val title: String, val subtitle: String, val body: String)

class RecordAdapter(
    private val onView: (Long) -> Unit,
    private val onEdit: (Long) -> Unit,
    private val onDelete: (Long) -> Unit
) : ListAdapter<RecordRow, RecordAdapter.Holder>(object : DiffUtil.ItemCallback<RecordRow>() {
    override fun areItemsTheSame(oldItem: RecordRow, newItem: RecordRow) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: RecordRow, newItem: RecordRow) = oldItem == newItem
}) {
    class Holder(val binding: RowRecordBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(RowRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = getItem(position)
        holder.binding.apply {
            title.text = row.title; subtitle.text = row.subtitle; body.text = row.body
            viewButton.setOnClickListener { onView(row.id) }
            editButton.setOnClickListener { onEdit(row.id) }
            deleteButton.setOnClickListener { onDelete(row.id) }
        }
    }
}
