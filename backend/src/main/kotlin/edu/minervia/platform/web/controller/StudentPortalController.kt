package edu.minervia.platform.web.controller

import edu.minervia.platform.security.StudentUserDetails
import edu.minervia.platform.service.StudentService
import edu.minervia.platform.web.dto.ApiResponse
import edu.minervia.platform.web.dto.ChangePasswordRequest
import edu.minervia.platform.web.dto.StudentDto
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/student/portal")
class StudentPortalController(
    private val studentService: StudentService,
    private val passwordEncoder: PasswordEncoder
) {

    @GetMapping("/me")
    fun getMyProfile(
        @AuthenticationPrincipal userDetails: StudentUserDetails
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.getStudentById(userDetails.getStudentId())
        return ResponseEntity.ok(ApiResponse.success(student))
    }

    @PutMapping("/me/password")
    fun changePassword(
        @AuthenticationPrincipal userDetails: StudentUserDetails,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        val student = userDetails.getStudent()

        if (!passwordEncoder.matches(request.currentPassword, student.passwordHash)) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Current password is incorrect"))
        }

        student.passwordHash = passwordEncoder.encode(request.newPassword)
        studentService.updateStudentEntity(student)

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
