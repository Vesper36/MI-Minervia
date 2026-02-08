package edu.minervia.platform.web.controller

import edu.minervia.platform.service.AdminService
import edu.minervia.platform.web.dto.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/super-admin/admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
class AdminManagementController(private val adminService: AdminService) {

    @GetMapping
    fun getAllAdmins(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<AdminDto>>> {
        val admins = adminService.getAllAdmins(pageable)
        return ResponseEntity.ok(ApiResponse.success(admins))
    }

    @GetMapping("/{id}")
    fun getAdmin(@PathVariable id: Long): ResponseEntity<ApiResponse<AdminDto>> {
        val admin = adminService.getAdminById(id)
        return ResponseEntity.ok(ApiResponse.success(admin))
    }

    @PostMapping
    fun createAdmin(
        @Valid @RequestBody request: CreateAdminRequest
    ): ResponseEntity<ApiResponse<AdminDto>> {
        val admin = adminService.createAdmin(request)
        return ResponseEntity.ok(ApiResponse.success(admin, "Admin created successfully"))
    }

    @PutMapping("/{id}/role")
    fun updateRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAdminRoleRequest
    ): ResponseEntity<ApiResponse<AdminDto>> {
        val admin = adminService.updateAdminRole(id, request.role)
        return ResponseEntity.ok(ApiResponse.success(admin, "Role updated successfully"))
    }

    @PostMapping("/{id}/deactivate")
    fun deactivateAdmin(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        adminService.deactivateAdmin(id)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Admin deactivated"))
    }

    @PostMapping("/{id}/activate")
    fun activateAdmin(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        adminService.activateAdmin(id)
        return ResponseEntity.ok(ApiResponse.success(Unit, "Admin activated"))
    }
}
