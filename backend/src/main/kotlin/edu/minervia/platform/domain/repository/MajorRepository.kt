package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.Major
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface MajorRepository : JpaRepository<Major, Long> {
    fun findByCode(code: String): Optional<Major>
    fun findAllByIsActiveTrue(): List<Major>
    fun existsByCode(code: String): Boolean
}
