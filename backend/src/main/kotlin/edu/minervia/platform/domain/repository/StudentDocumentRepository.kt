package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.DocumentStatus
import edu.minervia.platform.domain.entity.StudentDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StudentDocumentRepository : JpaRepository<StudentDocument, Long> {

    fun findByStudentIdAndStatus(
        studentId: Long,
        status: DocumentStatus,
        pageable: Pageable
    ): Page<StudentDocument>

    fun findByIdAndStudentId(id: Long, studentId: Long): StudentDocument?

    fun findByStudentId(studentId: Long, pageable: Pageable): Page<StudentDocument>
}
