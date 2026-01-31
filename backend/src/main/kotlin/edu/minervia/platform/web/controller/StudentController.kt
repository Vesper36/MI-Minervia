package edu.minervia.platform.web.controller

import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.service.StudentService
import edu.minervia.platform.web.dto.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Students", description = "Student management operations")
class StudentController(private val studentService: StudentService) {

    @GetMapping
    @Operation(summary = "Search students", description = "Get paginated list of students with optional filters")
    fun getAllStudents(
        @Parameter(description = "Search query (name, email, student number)") @RequestParam(required = false) query: String?,
        @Parameter(description = "Filter by status") @RequestParam(required = false) status: StudentStatus?,
        @Parameter(description = "Filter by identity type") @RequestParam(required = false) identityType: IdentityType?,
        @Parameter(description = "Filter by enrollment year") @RequestParam(required = false) enrollmentYear: Int?,
        @Parameter(description = "Filter by country code") @RequestParam(required = false) countryCode: String?,
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
    @Operation(summary = "Get student by ID", description = "Get detailed student information")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Student found"),
        SwaggerApiResponse(responseCode = "404", description = "Student not found")
    ])
    fun getStudent(@Parameter(description = "Student ID") @PathVariable id: Long): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.getStudentById(id)
        return ResponseEntity.ok(ApiResponse.success(student))
    }

    @GetMapping("/by-number/{studentNumber}")
    @Operation(summary = "Get student by number", description = "Get student by student number")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Student found"),
        SwaggerApiResponse(responseCode = "404", description = "Student not found")
    ])
    fun getStudentByNumber(
        @Parameter(description = "Student number") @PathVariable studentNumber: String
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.getStudentByNumber(studentNumber)
        return ResponseEntity.ok(ApiResponse.success(student))
    }

    @PostMapping
    @Operation(summary = "Create student", description = "Manually create a new student")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "201", description = "Student created"),
        SwaggerApiResponse(responseCode = "400", description = "Validation error")
    ])
    fun createStudent(
        @Valid @RequestBody request: CreateStudentRequest
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.createStudent(request)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(student, "Student created"))
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update student", description = "Update student information")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Student updated"),
        SwaggerApiResponse(responseCode = "404", description = "Student not found")
    ])
    fun updateStudent(
        @Parameter(description = "Student ID") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStudentRequest
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.updateStudent(id, request)
        return ResponseEntity.ok(ApiResponse.success(student, "Student updated"))
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend student", description = "Suspend a student account")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Student suspended"),
        SwaggerApiResponse(responseCode = "404", description = "Student not found")
    ])
    fun suspendStudent(
        @Parameter(description = "Student ID") @PathVariable id: Long,
        @Valid @RequestBody request: SuspendStudentRequest
    ): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.suspendStudent(id, request.reason)
        return ResponseEntity.ok(ApiResponse.success(student, "Student suspended"))
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate student", description = "Reactivate a suspended student account")
    @ApiResponses(value = [
        SwaggerApiResponse(responseCode = "200", description = "Student reactivated"),
        SwaggerApiResponse(responseCode = "404", description = "Student not found")
    ])
    fun reactivateStudent(@Parameter(description = "Student ID") @PathVariable id: Long): ResponseEntity<ApiResponse<StudentDto>> {
        val student = studentService.reactivateStudent(id)
        return ResponseEntity.ok(ApiResponse.success(student, "Student reactivated"))
    }

    @GetMapping("/stats")
    @Operation(summary = "Get student statistics", description = "Get student count statistics by status")
    fun getStats(): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val stats = studentService.getStudentStats()
        return ResponseEntity.ok(ApiResponse.success(stats))
    }
}
