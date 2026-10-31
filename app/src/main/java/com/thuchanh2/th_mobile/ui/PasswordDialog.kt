package com.thuchanh2.th_mobile.ui

import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.databinding.DialogChangePasswordBinding

fun AppCompatActivity.showPasswordDialog(accountId: Long) {
    val binding = DialogChangePasswordBinding.inflate(layoutInflater)
    val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.change_password)
        .setView(binding.root).setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.save, null).create()
    dialog.setOnShowListener {
        val button: Button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
        button.setOnClickListener {
            val old = binding.currentInput.rawValue()
            val new = binding.newInput.rawValue()
            val checks = listOf(
                binding.currentInput.validate(getString(R.string.required), old.isNotEmpty()),
                binding.newInput.validate(getString(R.string.password_invalid), new.isNotBlank() && new.length >= 6),
                binding.confirmInput.validate(getString(R.string.password_mismatch), new == binding.confirmInput.rawValue())
            )
            if (checks.all { it }) runAction(button) {
                studyApp.database.accountDao().changePassword(accountId, old, new)
                dialog.dismiss()
                notifyUser(getString(R.string.password_changed))
            }
        }
    }
    dialog.show()
}
