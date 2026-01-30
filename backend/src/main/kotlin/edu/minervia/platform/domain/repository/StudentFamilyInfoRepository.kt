package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.StudentFamilyInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface StudentFamilyInfoRepository : JpaRepository<StudentFamilyInfo, Long> {
    fun findByStudentId(studentId: Long): Optional<StudentFamilyInfo>
}
