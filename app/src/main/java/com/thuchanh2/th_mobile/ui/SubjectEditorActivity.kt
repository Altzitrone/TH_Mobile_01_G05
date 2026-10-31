package com.thuchanh2.th_mobile.ui

import android.os.Bundle
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.entity.Subject
import com.thuchanh2.th_mobile.databinding.ActivitySubjectEditorBinding
import java.util.Locale

class SubjectEditorActivity : ProtectedActivity() {
    private lateinit var binding: ActivitySubjectEditorBinding
    private val recordId get() = intent.getLongExtra("recordId", 0)

    override fun onAuthorized(savedInstanceState: Bundle?) {
        binding = ActivitySubjectEditorBinding.inflate(layoutInflater)
        installContent(binding.root)
        setupToolbar(binding.toolbar, getString(if (recordId == 0L) R.string.add_subject else R.string.edit_subject))
        binding.saveButton.setOnClickListener { saveSubject() }
        if (savedInstanceState?.containsKey("code") == true) {
            binding.codeInput.fill(savedInstanceState.getString("code").orEmpty())
            binding.nameInput.fill(savedInstanceState.getString("name").orEmpty())
            binding.creditsInput.fill(savedInstanceState.getString("credits").orEmpty())
            binding.descriptionInput.fill(savedInstanceState.getString("description").orEmpty())
        } else if (recordId != 0L) runAction(binding.saveButton) {
            val subject = studyApp.database.subjectDao().getById(account.id, recordId) ?: error(getString(R.string.not_found))
            binding.codeInput.fill(subject.subjectCode); binding.nameInput.fill(subject.subjectName)
            binding.creditsInput.fill(subject.credits.toString()); binding.descriptionInput.fill(subject.description)
        }
    }

    private fun saveSubject() {
        val code = binding.codeInput.value().uppercase(Locale.ROOT)
        val name = binding.nameInput.value()
        val credits = binding.creditsInput.value().toIntOrNull()
        val checks = listOf(
            binding.codeInput.validate(getString(R.string.required), code.isNotEmpty()),
            binding.nameInput.validate(getString(R.string.required), name.isNotEmpty()),
            binding.creditsInput.validate(getString(R.string.credits_invalid), credits != null && credits > 0)
        )
        if (!checks.all { it }) return
        runAction(binding.saveButton) {
            val subject = Subject(recordId, code, name, binding.descriptionInput.value(), checkNotNull(credits))
            if (recordId == 0L) studyApp.database.subjectDao().create(account.id, subject)
            else studyApp.database.subjectDao().update(account.id, subject)
            notifyUser(getString(R.string.saved))
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::binding.isInitialized) {
            outState.putString("code", binding.codeInput.value()); outState.putString("name", binding.nameInput.value())
            outState.putString("credits", binding.creditsInput.value()); outState.putString("description", binding.descriptionInput.value())
        }
        super.onSaveInstanceState(outState)
    }
}
