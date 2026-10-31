package com.thuchanh2.th_mobile.data.dao

import android.util.Patterns
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.thuchanh2.th_mobile.data.entity.Student
import com.thuchanh2.th_mobile.data.entity.StudentRecord
import kotlinx.coroutines.flow.Flow

@Dao
abstract class StudentDao {
    @Query("SELECT EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')")
    protected abstract suspend fun isAdmin(adminId: Long): Boolean

    @Query("""SELECT s.*, a.username FROM Student s JOIN Account a ON a.id = s.accountId
        WHERE EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')
        AND (:keyword = '' OR instr(lower(s.studentCode), lower(:keyword)) > 0
        OR instr(lower(s.fullname), lower(:keyword)) > 0 OR instr(lower(s.email), lower(:keyword)) > 0
        OR instr(lower(s.phone), lower(:keyword)) > 0 OR instr(lower(a.username), lower(:keyword)) > 0)
        ORDER BY s.fullname COLLATE NOCASE, s.id DESC""")
    abstract fun search(adminId: Long, keyword: String): Flow<List<StudentRecord>>

    @Query("""SELECT s.*, a.username FROM Student s JOIN Account a ON a.id = s.accountId
        WHERE s.id = :id AND EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')""")
    abstract suspend fun getById(adminId: Long, id: Long): StudentRecord?

    @Query("""SELECT s.*, a.username FROM Student s JOIN Account a ON a.id = s.accountId
        WHERE a.id = :accountId AND a.role = 'Student'""")
    abstract suspend fun getOwnProfile(accountId: Long): StudentRecord?

    @Query("SELECT COUNT(*) FROM Student WHERE EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')")
    abstract fun count(adminId: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM Student WHERE studentCode = :code COLLATE NOCASE AND id != :exceptId)")
    protected abstract suspend fun codeExists(code: String, exceptId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM Account WHERE username = :username COLLATE NOCASE AND id != :exceptId)")
    protected abstract suspend fun usernameExists(username: String, exceptId: Long): Boolean

    @Query("INSERT INTO Account(username, password, role) VALUES(:username, :passwordHash, 'Student')")
    protected abstract suspend fun insertAccount(username: String, passwordHash: String): Long

    @Query("INSERT INTO Student(studentCode, fullname, email, phone, accountId) VALUES(:code, :name, :email, :phone, :accountId)")
    protected abstract suspend fun insertRow(code: String, name: String, email: String, phone: String, accountId: Long): Long

    @Query("UPDATE Student SET studentCode = :code, fullname = :name, email = :email, phone = :phone WHERE id = :id")
    protected abstract suspend fun updateRow(id: Long, code: String, name: String, email: String, phone: String): Int

    @Query("UPDATE Account SET username = :username, password = COALESCE(:passwordHash, password) WHERE id = :id AND role = 'Student'")
    protected abstract suspend fun updateAccount(id: Long, username: String, passwordHash: String?): Int

    @Query("DELETE FROM Account WHERE id = :id AND role = 'Student'")
    protected abstract suspend fun deleteAccount(id: Long): Int

    // Account and Student are always created/updated/deleted atomically.
    @Transaction
    open suspend fun create(adminId: Long, student: Student, username: String, passwordHash: String): Long {
        require(isAdmin(adminId)) { "Chỉ Admin được quản lý sinh viên." }
        validate(student, username, 0, 0)
        require(passwordHash.isNotBlank()) { "Vui lòng nhập mật khẩu." }
        val accountId = insertAccount(username, passwordHash)
        return insertRow(student.studentCode, student.fullname, student.email, student.phone, accountId)
    }

    @Transaction
    open suspend fun update(adminId: Long, student: Student, username: String, passwordHash: String?) {
        require(isAdmin(adminId)) { "Chỉ Admin được quản lý sinh viên." }
        val existing = getById(adminId, student.id) ?: error("Sinh viên không còn tồn tại.")
        validate(student, username, student.id, existing.student.accountId)
        check(updateAccount(existing.student.accountId, username, passwordHash) == 1)
        check(updateRow(student.id, student.studentCode, student.fullname, student.email, student.phone) == 1)
    }

    @Transaction
    open suspend fun delete(adminId: Long, id: Long) {
        require(isAdmin(adminId)) { "Chỉ Admin được quản lý sinh viên." }
        val existing = getById(adminId, id) ?: error("Sinh viên không còn tồn tại.")
        // The foreign key cascades deletion to the linked Student row.
        check(deleteAccount(existing.student.accountId) == 1)
    }

    private suspend fun validate(student: Student, username: String, exceptStudent: Long, exceptAccount: Long) {
        require(student.studentCode.isNotBlank() && student.fullname.isNotBlank()) { "Mã và họ tên sinh viên không được để trống." }
        require(Patterns.EMAIL_ADDRESS.matcher(student.email).matches()) { "Email không đúng định dạng." }
        require(student.phone.matches(Regex("\\+?[0-9]{8,15}"))) { "Số điện thoại cần từ 8 đến 15 chữ số." }
        require(username.matches(Regex("[A-Za-z0-9._-]{3,32}"))) { "Tên đăng nhập cần 3–32 ký tự, gồm chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang." }
        require(!codeExists(student.studentCode, exceptStudent)) { "Mã sinh viên đã tồn tại." }
        require(!usernameExists(username, exceptAccount)) { "Tên đăng nhập đã tồn tại." }
    }
}
