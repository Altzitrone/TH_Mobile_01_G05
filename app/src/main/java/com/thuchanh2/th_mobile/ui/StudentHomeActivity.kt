package com.thuchanh2.th_mobile.ui

import android.content.Intent
import android.os.Bundle
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.data.entity.AccountRole
import com.thuchanh2.th_mobile.databinding.ActivityStudentHomeBinding

class StudentHomeActivity : ProtectedActivity() {
    override val adminOnly = false

    override fun onAuthorized(savedInstanceState: Bundle?) {
        if (account.role != AccountRole.STUDENT) {
            startActivity(Intent(this, AdminDashboardActivity::class.java)); finish(); return
        }
        val binding = ActivityStudentHomeBinding.inflate(layoutInflater)
        installContent(binding.root)
        setupToolbar(binding.toolbar, getString(R.string.student_home), back = false)
        binding.accountInfo.text = getString(R.string.account_summary, account.username, account.role)
        binding.logoutButton.setOnClickListener { confirmLogout() }
        binding.changePasswordButton.setOnClickListener { showPasswordDialog(account.id) }
        runAction {
            val profile = studyApp.database.studentDao().getOwnProfile(account.id)
            binding.studentName.text = profile?.student?.fullname ?: account.username
            binding.profileInfo.text = profile?.let { studentDetail(it) } ?: getString(R.string.profile_missing)
        }
    }
}
