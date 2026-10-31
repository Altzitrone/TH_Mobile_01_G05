package com.thuchanh2.th_mobile.ui

import android.os.Bundle
import android.util.Patterns
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.auth.PasswordHasher
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.databinding.ActivityStudentEditorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class StudentEditorActivity : ProtectedActivity() {
    private lateinit var binding: ActivityStudentEditorBinding
    private val recordId get() = intent.getLongExtra("recordId", 0)

    override fun onAuthorized(savedInstanceState: Bundle?) {
        binding = ActivityStudentEditorBinding.inflate(layoutInflater)
        installContent(binding.root)
        setupToolbar(binding.toolbar, getString(if (recordId == 0L) R.string.add_student else R.string.edit_student))
        if (recordId != 0L) binding.passwordInput.hint = getString(R.string.password_optional)
        binding.saveButton.setOnClickListener { saveStudent() }
        if (savedInstanceState?.containsKey("code") == true) {
            binding.codeInput.fill(savedInstanceState.getString("code").orEmpty())
            binding.nameInput.fill(savedInstanceState.getString("name").orEmpty())
            binding.emailInput.fill(savedInstanceState.getString("email").orEmpty())
            binding.phoneInput.fill(savedInstanceState.getString("phone").orEmpty())
            binding.usernameInput.fill(savedInstanceState.getString("username").orEmpty())
        } else if (recordId != 0L) runAction(binding.saveButton) {
            val record = studyApp.database.studentDao().getById(account.id, recordId) ?: error(getString(R.string.not_found))
            binding.codeInput.fill(record.student.studentCode)
            binding.nameInput.fill(record.student.fullname)
            binding.emailInput.fill(record.student.email)
            binding.phoneInput.fill(record.student.phone)
            binding.usernameInput.fill(record.username)
        }
    }

    private fun saveStudent() {
        val code = binding.codeInput.value().uppercase(Locale.ROOT)
        val name = binding.nameInput.value()
        val email = binding.emailInput.value()
        val phone = binding.phoneInput.value()
        val username = binding.usernameInput.value().lowercase(Locale.ROOT)
        val password = binding.passwordInput.rawValue()
        val checks = listOf(
            binding.codeInput.validate(getString(R.string.required), code.isNotEmpty()),
            binding.nameInput.validate(getString(R.string.required), name.isNotEmpty()),
            binding.emailInput.validate(getString(R.string.email_invalid), Patterns.EMAIL_ADDRESS.matcher(email).matches()),
            binding.phoneInput.validate(getString(R.string.phone_invalid), phone.matches(Regex("\\+?[0-9]{8,15}"))),
            binding.usernameInput.validate(getString(R.string.username_invalid), username.matches(Regex("[a-z0-9._-]{3,32}"))),
            binding.passwordInput.validate(getString(R.string.password_invalid), (recordId != 0L && password.isEmpty()) || (password.isNotBlank() && password.length >= 6))
        )
        if (!checks.all { it }) return
        runAction(binding.saveButton) {
            val hash = if (password.isEmpty()) null else withContext(Dispatchers.Default) { PasswordHasher.hash(password) }
            val student = Student(id = recordId, studentCode = code, fullname = name, email = email, phone = phone)
            if (recordId == 0L) studyApp.database.studentDao().create(account.id, student, username, checkNotNull(hash))
            else studyApp.database.studentDao().update(account.id, student, username, hash)
            notifyUser(getString(R.string.saved))
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (::binding.isInitialized) {
            outState.putString("code", binding.codeInput.value()); outState.putString("name", binding.nameInput.value())
            outState.putString("email", binding.emailInput.value()); outState.putString("phone", binding.phoneInput.value())
            outState.putString("username", binding.usernameInput.value())
        }
        super.onSaveInstanceState(outState)
    }
}
