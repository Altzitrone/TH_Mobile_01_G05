package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.thuchanh2.th_mobile.auth.PasswordHasher
import com.thuchanh2.th_mobile.data.entity.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
abstract class AccountDao {
    @Query("SELECT * FROM Account WHERE username = :username COLLATE NOCASE LIMIT 1")
    abstract suspend fun findByUsername(username: String): Account?

    @Query("SELECT * FROM Account WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: Long): Account?

    @Query("INSERT OR IGNORE INTO Account(username, password, role) VALUES('admin', :passwordHash, 'Admin')")
    protected abstract suspend fun insertDefaultAdmin(passwordHash: String): Long

    @Query("UPDATE Account SET password = :newHash WHERE id = :id AND password = :oldHash")
    protected abstract suspend fun updatePassword(id: Long, oldHash: String, newHash: String): Int

    suspend fun ensureDefaultAdmin() {
        if (findByUsername("admin") == null) {
            insertDefaultAdmin(withContext(Dispatchers.Default) { PasswordHasher.hash("admin123") })
        }
    }

    suspend fun login(username: String, password: String): Account? {
        val account = findByUsername(username.trim()) ?: return null
        return if (withContext(Dispatchers.Default) { PasswordHasher.verify(password, account.password) }) account else null
    }

    suspend fun changePassword(id: Long, oldPassword: String, newPassword: String) {
        require(newPassword.isNotBlank() && newPassword.length >= 6) { "Mật khẩu mới phải có ít nhất 6 ký tự và không chỉ gồm khoảng trắng." }
        require(newPassword != oldPassword) { "Mật khẩu mới phải khác mật khẩu hiện tại." }
        val account = findById(id) ?: error("Tài khoản không còn tồn tại.")
        require(withContext(Dispatchers.Default) { PasswordHasher.verify(oldPassword, account.password) }) {
            "Mật khẩu hiện tại không đúng."
        }
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(newPassword) }
        check(updatePassword(id, account.password, hash) == 1) { "Tài khoản đã thay đổi. Vui lòng đăng nhập lại." }
    }
}
