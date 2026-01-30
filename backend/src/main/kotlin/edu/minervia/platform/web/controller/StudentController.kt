package edu.minervia.platform.web.controller

import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.service.StudentService
import edu.minervia.platform.web.dto.*
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/students")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
class StudentController(private val studentService: StudentService) {

    @GetMapping
    fun getAllStudents(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) status: StudentStatus?,
        @RequestParam(required = false) identityType: IdentityType?,
        @RequestParam(required = false) enrollmentYear: Int?,
        @RequestParam(required = false) countryCode: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<StudentListDto>>> {
        val criteria = StudentSearchCriteria(
            query = query,
            status = status,
            identityType = identityType,
            enrollmentYear = enrollmentYear,
            countryCode = countryCode
        )
        val students = studentService.searchStudents(criteria, pageable)
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

    @PostMapping
    fun createStudent(
        @Valid @RequestBody request: CreateStudentRequest
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.createStudent(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(student, "Student created"))
    }

    @PatchMapping("/{id}")
    fun updateStudent(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStudentRequest
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.updateStudent(id, request)
        return ResponseEntity.ok(ApiResponse.success(student, "Student updated"))
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
