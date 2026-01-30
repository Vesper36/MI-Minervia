package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.Admin
import edu.minervia.platform.domain.enums.AdminRole
import edu.minervia.platform.domain.repository.AdminRepository
import edu.minervia.platform.web.dto.AdminDto
import edu.minervia.platform.web.dto.CreateAdminRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun getAdminById(id: Long): AdminDto {
        val admin = adminRepository.findById(id)
            .orElseThrow { NoSuchElementException("Admin not found: $id") }
        return admin.toDto()
    }

    fun getAllAdmins(pageable: Pageable): Page<AdminDto> {
        return adminRepository.findAll(pageable).map { it.toDto() }
    }

    @Transactional
    fun createAdmin(request: CreateAdminRequest): AdminDto {
        if (adminRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }
        if (adminRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        val admin = Admin(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            role = request.role
        )

        return adminRepository.save(admin).toDto()
    }

    @Transactional
    fun updateAdminRole(id: Long, role: AdminRole): AdminDto {
        val admin = adminRepository.findById(id)
            .orElseThrow { NoSuchElementException("Admin not found: $id") }
        admin.role = role
        return adminRepository.save(admin).toDto()
    }

    @Transactional
    fun deactivateAdmin(id: Long) {
        val admin = adminRepository.findById(id)
            .orElseThrow { NoSuchElementException("Admin not found: $id") }
        admin.isActive = false
        adminRepository.save(admin)
    }

    @Transactional
    fun activateAdmin(id: Long) {
        val admin = adminRepository.findById(id)
            .orElseThrow { NoSuchElementException("Admin not found: $id") }
        admin.isActive = true
        admin.failedLoginAttempts = 0
        admin.lockedUntil = null
        adminRepository.save(admin)
    }

    private fun Admin.toDto() = AdminDto(
        id = this.id,
        username = this.username,
        email = this.email,
        role = this.role,
        isActive = this.isActive,
        totpEnabled = this.totpEnabled,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
