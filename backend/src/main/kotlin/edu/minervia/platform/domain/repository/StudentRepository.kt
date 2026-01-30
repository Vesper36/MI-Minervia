package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StudentRepository : JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    fun findByStudentNumber(studentNumber: String): Optional<Student>
    fun findByEduEmail(eduEmail: String): Optional<Student>
    fun existsByStudentNumber(studentNumber: String): Boolean
    fun existsByEduEmail(eduEmail: String): Boolean
    fun findAllByStatus(status: StudentStatus, pageable: Pageable): Page<Student>
    fun findAllByEnrollmentYear(year: Int, pageable: Pageable): Page<Student>

    @Query("SELECT MAX(CAST(SUBSTRING(s.studentNumber, 7) AS int)) FROM Student s WHERE s.studentNumber LIKE :prefix%")
    fun findMaxSequenceByPrefix(prefix: String): Int?

    @Query("SELECT COUNT(s) FROM Student s WHERE s.status = :status")
    fun countByStatus(status: StudentStatus): Long

    @Query("""
        SELECT s FROM Student s
        WHERE (:query IS NULL OR
               LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(s.eduEmail) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:status IS NULL OR s.status = :status)
        AND (:identityType IS NULL OR s.identityType = :identityType)
        AND (:enrollmentYear IS NULL OR s.enrollmentYear = :enrollmentYear)
        AND (:countryCode IS NULL OR s.countryCode = :countryCode)
    """)
    fun searchStudents(
        query: String?,
        status: StudentStatus?,
        identityType: IdentityType?,
        enrollmentYear: Int?,
        countryCode: String?,
        pageable: Pageable
    ): Page<Student>
}
