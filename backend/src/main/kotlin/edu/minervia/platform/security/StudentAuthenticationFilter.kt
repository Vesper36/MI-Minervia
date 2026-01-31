package edu.minervia.platform.security

import edu.minervia.platform.domain.enums.StudentStatus
import edu.minervia.platform.domain.repository.StudentRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class StudentAuthenticationFilter(
    private val studentJwtService: StudentJwtService,
    private val studentRepository: StudentRepository,
    private val tokenRevocationService: TokenRevocationService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!request.requestURI.startsWith("/api/portal") && !request.requestURI.startsWith("/api/student")) {
            filterChain.doFilter(request, response)
            return
        }

        if (request.requestURI.startsWith("/api/student/auth")) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val jwt = authHeader.substring(7)

        if (!studentJwtService.validateToken(jwt)) {
            filterChain.doFilter(request, response)
            return
        }

        val actorType = studentJwtService.getActorTypeFromToken(jwt)
        if (actorType != ActorType.STUDENT) {
            filterChain.doFilter(request, response)
            return
        }

        val tokenType = studentJwtService.getTokenTypeFromToken(jwt)
        if (tokenType != TokenType.ACCESS) {
            filterChain.doFilter(request, response)
            return
        }

        val jti = studentJwtService.getJtiFromToken(jwt)
        if (jti.isNullOrBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        if (tokenRevocationService.isRevoked(jti)) {
            filterChain.doFilter(request, response)
            return
        }

        val studentNumber = studentJwtService.getStudentNumberFromToken(jwt)
        val studentId = studentJwtService.getStudentIdFromToken(jwt)

        if (studentNumber != null && studentId != null && SecurityContextHolder.getContext().authentication == null) {
            val student = studentRepository.findByStudentNumber(studentNumber).orElse(null)

            if (student != null && student.status == StudentStatus.ACTIVE) {
                val authorities = listOf(SimpleGrantedAuthority("ROLE_STUDENT"))
                val userDetails = StudentUserDetails(student)

                val authToken = UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities
                )
                authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authToken
            }
        }

        filterChain.doFilter(request, response)
    }
}
