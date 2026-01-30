package edu.minervia.platform.web.controller

import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.service.StudentService
import edu.minervia.platform.web.dto.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class StudentController(private val studentService: StudentService) {

    @GetMapping
    fun getAllStudents(
        @RequestParam(required = false) status: StudentStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<StudentListDto>>> {
        val students = if (status != null) {
            studentService.getStudentsByStatus(status, pageable)
        } else {
            studentService.getAllStudents(pageable)
        }
        return ResponseEntity.ok(ApiResponse.success(students))
    }

    @GetMapping("/{id}")
    fun getStudent(@PathVariable id: Long): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.getStudentById(id)
        return ResponseEntity.ok(ApiResponse.success(student))
    }

    @GetMapping("/by-number/{studentNumber}")
    fun getStudentByNumber(@PathVariable studentNumber: String): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.getStudentByNumber(studentNumber)
        return ResponseEntity.ok(ApiResponse.success(student))
    }

    @PostMapping("/{id}/suspend")
    fun suspendStudent(
        @PathVariable id: Long,
        @Valid @RequestBody request: SuspendStudentRequest
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.suspendStudent(id, request.reason)
        return ResponseEntity.ok(ApiResponse.success(student, "Student suspended"))
    }

    @PostMapping("/{id}/reactivate")
    fun reactivateStudent(@PathVariable id: Long): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.reactivateStudent(id)
        return ResponseEntity.ok(ApiResponse.success(student, "Student reactivated"))
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val stats = studentService.getStudentStats()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
}
