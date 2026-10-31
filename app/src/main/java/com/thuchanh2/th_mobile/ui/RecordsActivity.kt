package com.thuchanh2.th_mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.databinding.ActivityRecordsBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class StudentListActivity : RecordsActivity() { override val students = true }
class SubjectListActivity : RecordsActivity() { override val students = false }

@OptIn(ExperimentalCoroutinesApi::class)
abstract class RecordsActivity : ProtectedActivity() {
    protected abstract val students: Boolean

    override fun onAuthorized(savedInstanceState: Bundle?) {
        val binding = ActivityRecordsBinding.inflate(layoutInflater)
        installContent(binding.root)
        setupToolbar(binding.toolbar, getString(if (students) R.string.student_list_title else R.string.subject_list_title))
        binding.searchInput.hint = getString(if (students) R.string.search_students else R.string.search_subjects)
        binding.addButton.setText(if (students) R.string.add_student else R.string.add_subject)
        val model = ViewModelProvider(this)[RecordListViewModel::class.java]
        if (savedInstanceState != null && model.keyword.value.isEmpty()) model.keyword.value = savedInstanceState.getString("keyword").orEmpty()
        binding.searchInput.fill(model.keyword.value)
        binding.searchInput.editText?.doAfterTextChanged { model.keyword.value = it?.toString().orEmpty() }
        val adapter = RecordAdapter(::viewRecord, ::editRecord, ::confirmDelete)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.addButton.setOnClickListener { editRecord(0) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    val rows = model.keyword.flatMapLatest { keyword ->
                        if (students) studyApp.database.studentDao().search(account.id, keyword.trim()).map { records ->
                            records.map { r -> RecordRow(r.student.id, r.student.fullname, r.student.studentCode, "${r.student.email}\n${r.username}") }
                        } else studyApp.database.subjectDao().search(account.id, keyword.trim()).map { subjects ->
                            subjects.map { s -> RecordRow(s.id, s.subjectName, s.subjectCode, getString(R.string.credit_value, s.credits)) }
                        }
                    }
                    rows.collect {
                        adapter.submitList(it)
                        binding.emptyState.isVisible = it.isEmpty()
                        binding.resultCount.text = getString(R.string.results_count, it.size)
                    }
                } catch (error: CancellationException) { throw error }
                catch (error: Exception) { showError(error) }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("keyword", ViewModelProvider(this)[RecordListViewModel::class.java].keyword.value)
        super.onSaveInstanceState(outState)
    }

    private fun editRecord(id: Long) {
        val destination = if (students) StudentEditorActivity::class.java else SubjectEditorActivity::class.java
        startActivity(Intent(this, destination).putExtra("recordId", id))
    }

    private fun viewRecord(id: Long) = runAction {
        val description = if (students) {
            val record = studyApp.database.studentDao().getById(account.id, id) ?: error(getString(R.string.not_found))
            studentDetail(record)
        } else {
            val subject = studyApp.database.subjectDao().getById(account.id, id) ?: error(getString(R.string.not_found))
            subjectDetail(subject)
        }
        MaterialAlertDialogBuilder(this).setTitle(if (students) R.string.student_details else R.string.subject_details)
            .setMessage(description).setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.edit) { _, _ -> editRecord(id) }.show()
    }

    private fun confirmDelete(id: Long) = runAction {
        val name = if (students) studyApp.database.studentDao().getById(account.id, id)?.student?.fullname
            else studyApp.database.subjectDao().getById(account.id, id)?.subjectName
        check(name != null) { getString(R.string.not_found) }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (students) R.string.delete_student_title else R.string.delete_subject_title)
            .setMessage(getString(if (students) R.string.delete_student_message else R.string.delete_subject_message, name))
            .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.delete, null).create()
        dialog.setOnShowListener {
            val button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                runAction(button) {
                    if (students) studyApp.database.studentDao().delete(account.id, id)
                    else check(studyApp.database.subjectDao().delete(account.id, id) == 1) { getString(R.string.not_found) }
                    dialog.dismiss()
                    notifyUser(getString(R.string.deleted))
                }
            }
        }
        dialog.show()
    }
}
