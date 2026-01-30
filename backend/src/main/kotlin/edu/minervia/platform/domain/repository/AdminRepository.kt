package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.enums.AdminRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface AdminRepository : JpaRepository<Admin, Long> {
    fun findByUsername(username: String): Optional<Admin>
    fun findByEmail(email: String): Optional<Admin>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findAllByRole(role: AdminRole): List<Admin>
    fun findAllByIsActiveTrue(): List<Admin>
}
