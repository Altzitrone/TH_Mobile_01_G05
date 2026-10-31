package com.thuchanh2.th_mobile.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.databinding.ActivityAdminDashboardBinding
import kotlinx.coroutines.launch

class AdminDashboardActivity : ProtectedActivity() {
    override fun onAuthorized(savedInstanceState: Bundle?) {
        val binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        installContent(binding.root)
        setupToolbar(binding.toolbar, getString(R.string.admin_dashboard), back = false)
        binding.welcome.text = getString(R.string.dashboard_subtitle)
        binding.accountInfo.text = getString(R.string.admin_identity, account.username)
        binding.studentsButton.setOnClickListener { startActivity(Intent(this, StudentListActivity::class.java)) }
        binding.subjectsButton.setOnClickListener { startActivity(Intent(this, SubjectListActivity::class.java)) }
        binding.changePasswordButton.setOnClickListener { showPasswordDialog(account.id) }
        binding.logoutButton.setOnClickListener { confirmLogout() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { studyApp.database.studentDao().count(account.id).collect { binding.studentCount.text = getString(R.string.count_value, it) } }
                launch { studyApp.database.subjectDao().count(account.id).collect { binding.subjectCount.text = getString(R.string.count_value, it) } }
            }
        }
    }
}
