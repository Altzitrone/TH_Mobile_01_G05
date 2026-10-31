package com.thuchanh2.th_mobile

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.thuchanh2.th_mobile.data.entity.AccountRole
import com.thuchanh2.th_mobile.databinding.ActivityMainBinding
import com.thuchanh2.th_mobile.ui.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        installContent(binding.root)
        val model = ViewModelProvider(this)[LoginViewModel::class.java]
        binding.loginButton.setOnClickListener {
            val usernameOk = binding.usernameInput.validate(getString(R.string.required), binding.usernameInput.value().isNotBlank())
            val passwordOk = binding.passwordInput.validate(getString(R.string.required), binding.passwordInput.rawValue().isNotBlank())
            if (usernameOk && passwordOk) model.login(
                binding.usernameInput.value(), binding.passwordInput.rawValue(), binding.rememberLogin.isChecked
            )
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    binding.progress.isVisible = state.busy
                    binding.loginButton.isEnabled = !state.busy
                    binding.loginError.isVisible = state.error != null
                    binding.loginError.text = state.error
                    state.account?.let { account ->
                        if (!isFinishing) {
                            val destination = if (account.role == AccountRole.ADMIN) AdminDashboardActivity::class.java else StudentHomeActivity::class.java
                            startActivity(Intent(this@MainActivity, destination)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                            finish()
                        }
                    }
                }
            }
        }

    }

}
