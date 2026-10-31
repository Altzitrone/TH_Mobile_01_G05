package com.thuchanh2.th_mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thuchanh2.th_mobile.StudyApplication
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.AccountRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginState(val busy: Boolean = true, val account: Account? = null, val error: String? = null)

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StudyApplication
    private val mutableState = MutableStateFlow(LoginState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                app.database.accountDao().ensureDefaultAdmin()
                mutableState.value = LoginState(busy = false, account = app.session.currentAccount())
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.value = LoginState(busy = false, error = "Không thể mở dữ liệu. Vui lòng mở lại ứng dụng.") }
        }
    }

    fun login(username: String, password: String, remember: Boolean) {
        if (mutableState.value.busy) return
        mutableState.value = LoginState(busy = true)
        viewModelScope.launch {
            try {
                val account = app.database.accountDao().login(username, password)
                if (account == null || account.role !in setOf(AccountRole.ADMIN, AccountRole.STUDENT)) {
                    mutableState.value = LoginState(busy = false, error = "Tên đăng nhập hoặc mật khẩu không đúng.")
                } else {
                    app.session.login(account, remember)
                    mutableState.value = LoginState(busy = false, account = account)
                }
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) { mutableState.value = LoginState(busy = false, error = "Không thể đăng nhập. Vui lòng thử lại.") }
        }
    }
}
