package com.thuchanh2.th_mobile.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.thuchanh2.th_mobile.data.entity.Subject
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SubjectDao {
    @Query("SELECT EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')")
    protected abstract suspend fun isAdmin(adminId: Long): Boolean

    @Query("""SELECT * FROM Subject WHERE EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')
        AND (:keyword = '' OR instr(lower(subjectCode), lower(:keyword)) > 0
        OR instr(lower(subjectName), lower(:keyword)) > 0 OR instr(lower(description), lower(:keyword)) > 0
        OR CAST(credits AS TEXT) = :keyword) ORDER BY subjectName COLLATE NOCASE, id DESC""")
    abstract fun search(adminId: Long, keyword: String): Flow<List<Subject>>

    @Query("SELECT * FROM Subject WHERE id = :id AND EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')")
    abstract suspend fun getById(adminId: Long, id: Long): Subject?

    @Query("SELECT COUNT(*) FROM Subject WHERE EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')")
    abstract fun count(adminId: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM Subject WHERE subjectCode = :code COLLATE NOCASE AND id != :exceptId)")
    protected abstract suspend fun codeExists(code: String, exceptId: Long): Boolean

    @Query("INSERT INTO Subject(subjectCode, subjectName, description, credits) VALUES(:code, :name, :description, :credits)")
    protected abstract suspend fun insertRow(code: String, name: String, description: String, credits: Int): Long

    @Query("UPDATE Subject SET subjectCode = :code, subjectName = :name, description = :description, credits = :credits WHERE id = :id")
    protected abstract suspend fun updateRow(id: Long, code: String, name: String, description: String, credits: Int): Int

    @Query("DELETE FROM Subject WHERE id = :id AND EXISTS(SELECT 1 FROM Account WHERE id = :adminId AND role = 'Admin')")
    abstract suspend fun delete(adminId: Long, id: Long): Int

    @Transaction
    open suspend fun create(adminId: Long, subject: Subject): Long {
        validate(adminId, subject)
        return insertRow(subject.subjectCode, subject.subjectName, subject.description, subject.credits)
    }

    @Transaction
    open suspend fun update(adminId: Long, subject: Subject) {
        validate(adminId, subject)
        check(updateRow(subject.id, subject.subjectCode, subject.subjectName, subject.description, subject.credits) == 1) {
            "Môn học không còn tồn tại."
        }
    }

    private suspend fun validate(adminId: Long, subject: Subject) {
        require(isAdmin(adminId)) { "Chỉ Admin được quản lý môn học." }
        require(subject.subjectCode.isNotBlank() && subject.subjectName.isNotBlank()) { "Mã và tên môn học không được để trống." }
        require(subject.credits > 0) { "Số tín chỉ phải lớn hơn 0." }
        require(!codeExists(subject.subjectCode, subject.id)) { "Mã môn học đã tồn tại." }
    }
}
