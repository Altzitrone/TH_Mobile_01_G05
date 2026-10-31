package com.thuchanh2.th_mobile.auth

import android.content.Context
import androidx.core.content.edit
import com.thuchanh2.th_mobile.data.dao.AccountDao
import com.thuchanh2.th_mobile.data.entity.Account
import com.thuchanh2.th_mobile.data.entity.AccountRole

/** SharedPreferences remembers identity only; the database is the source of truth for the role. */
class SessionManager(context: Context, private val accountDao: AccountDao) {
    private val preferences = context.getSharedPreferences("login_session", Context.MODE_PRIVATE)
    private var activeAccountId: Long? = null

    suspend fun currentAccount(): Account? {
        val id = activeAccountId ?: preferences.getLong("accountId", 0).takeIf { it > 0 } ?: return null
        val account = accountDao.findById(id)
        if (account == null || account.role !in setOf(AccountRole.ADMIN, AccountRole.STUDENT)) {
            logout()
            return null
        }
        activeAccountId = account.id
        return account
    }

    fun login(account: Account, remember: Boolean) {
        activeAccountId = account.id
        preferences.edit {
            clear()
            // Reminder identity survives process death without restoring an unchecked login session.
            putLong("notificationAccountId", account.id)
            if (remember) {
                putLong("accountId", account.id)
                putString("username", account.username)
                putString("role", account.role)
                putBoolean("rememberLogin", true)
            }
        }
    }

    fun logout() {
        activeAccountId = null
        preferences.edit { clear() }
    }

    fun reminderAccountId(): Long = activeAccountId ?: preferences.getLong("notificationAccountId", 0)
}
