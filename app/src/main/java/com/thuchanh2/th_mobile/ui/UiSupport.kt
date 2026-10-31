package com.thuchanh2.th_mobile.ui

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.thuchanh2.th_mobile.MainActivity
import com.thuchanh2.th_mobile.R
import com.thuchanh2.th_mobile.StudyApplication
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.AccountRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

val AppCompatActivity.studyApp get() = application as StudyApplication

fun AppCompatActivity.installContent(root: View) {
    enableEdgeToEdge()
    setContentView(root)
    val left = root.paddingLeft; val top = root.paddingTop
    val right = root.paddingRight; val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        insets
    }
}

fun AppCompatActivity.setupToolbar(toolbar: MaterialToolbar, title: String, back: Boolean = true) {
    toolbar.title = title
    if (back) {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}

fun AppCompatActivity.notifyUser(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun AppCompatActivity.showError(error: Throwable) {
    val message = when (error) {
        is SQLiteConstraintException -> getString(R.string.error_duplicate_or_linked)
        is IllegalArgumentException, is IllegalStateException -> error.message ?: getString(R.string.error_generic)
        else -> getString(R.string.error_generic)
    }
    MaterialAlertDialogBuilder(this).setTitle(R.string.error_title).setMessage(message)
        .setPositiveButton(R.string.close, null).show()
}

fun AppCompatActivity.runAction(button: View? = null, action: suspend () -> Unit) {
    button?.isEnabled = false
    lifecycleScope.launch {
        try { action() }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { showError(error) }
        finally { button?.isEnabled = true }
    }
}

fun TextInputLayout.value(): String = editText?.text?.toString()?.trim().orEmpty()
fun TextInputLayout.rawValue(): String = editText?.text?.toString().orEmpty()
fun TextInputLayout.fill(value: String) { editText?.setText(value) }
fun TextInputLayout.validate(message: String?, valid: Boolean): Boolean {
    error = if (valid) null else message
    if (!valid) editText?.requestFocus()
    return valid
}

abstract class ProtectedActivity : AppCompatActivity() {
    protected lateinit var account: Account
    protected open val adminOnly = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            try {
                val current = studyApp.session.currentAccount()
                if (current == null || (adminOnly && current.role != AccountRole.ADMIN)) {
                    startActivity(Intent(this@ProtectedActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                    return@launch
                }
                account = current
                onAuthorized(savedInstanceState)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { showError(error); finish() }
        }
    }

    protected abstract fun onAuthorized(savedInstanceState: Bundle?)

    protected fun confirmLogout() {
        MaterialAlertDialogBuilder(this).setTitle(R.string.logout).setMessage(R.string.logout_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout) { _, _ ->
                studyApp.session.logout()
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            }.show()
    }
}
