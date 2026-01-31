package edu.minervia.platform.service

import edu.minervia.platform.domain.repository.StudentRepository
import edu.minervia.platform.security.StudentJwtService
import edu.minervia.platform.security.TokenRevocationService
import edu.minervia.platform.security.TokenType
import edu.minervia.platform.web.dto.StudentLoginRequest
import edu.minervia.platform.web.dto.StudentLoginResponse
import edu.minervia.platform.web.dto.StudentRefreshTokenResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class StudentAuthService(
    private val studentRepository: StudentRepository,
    private val passwordEncoder: PasswordEncoder,
    private val studentJwtService: StudentJwtService,
    private val tokenRevocationService: TokenRevocationService
) {
    fun login(request: StudentLoginRequest): StudentLoginResponse {
        val student = studentRepository.findByEduEmail(request.email)
            .or { studentRepository.findByStudentNumber(request.email) }
            .orElseThrow { BadCredentialsException("Invalid credentials") }

        if (!passwordEncoder.matches(request.password, student.passwordHash)) {
            throw BadCredentialsException("Invalid credentials")
        }

        if (student.status != edu.minervia.platform.domain.enums.StudentStatus.ACTIVE) {
            throw BadCredentialsException("Account is not active")
        }

        val tokenPair = studentJwtService.generateTokenPair(
            studentNumber = student.studentNumber,
            studentId = student.id
        )

        return StudentLoginResponse(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            accessExpiresIn = tokenPair.accessExpiresIn,
            refreshExpiresIn = tokenPair.refreshExpiresIn,
            studentNumber = student.studentNumber,
            fullName = student.fullName,
            eduEmail = student.eduEmail
        )
    }

    fun refreshToken(refreshToken: String): StudentRefreshTokenResponse {
        if (!studentJwtService.validateToken(refreshToken)) {
            throw BadCredentialsException("Invalid refresh token")
        }

        val tokenType = studentJwtService.getTokenTypeFromToken(refreshToken)
        if (tokenType != TokenType.REFRESH) {
            throw BadCredentialsException("Token is not a refresh token")
        }

        val jti = studentJwtService.getJtiFromToken(refreshToken)
        if (jti.isNullOrBlank()) {
            throw BadCredentialsException("Invalid refresh token: missing JTI")
        }

        if (tokenRevocationService.isRevoked(jti)) {
            throw BadCredentialsException("Refresh token has been revoked")
        }

        val studentNumber = studentJwtService.getStudentNumberFromToken(refreshToken)
            ?: throw BadCredentialsException("Invalid refresh token")

        val student = studentRepository.findByStudentNumber(studentNumber)
            .orElseThrow { BadCredentialsException("Student not found") }

        if (student.status != edu.minervia.platform.domain.enums.StudentStatus.ACTIVE) {
            throw BadCredentialsException("Account is not active")
        }

        tokenRevocationService.revokeRefreshToken(jti)

        val tokenPair = studentJwtService.generateTokenPair(
            studentNumber = student.studentNumber,
            studentId = student.id
        )

        return StudentRefreshTokenResponse(
            accessToken = tokenPair.accessToken,
            refreshToken = tokenPair.refreshToken,
            accessExpiresIn = tokenPair.accessExpiresIn,
            refreshExpiresIn = tokenPair.refreshExpiresIn
        )
    }

    fun logout(accessToken: String, refreshToken: String?) {
        if (studentJwtService.validateToken(accessToken)) {
            val tokenType = studentJwtService.getTokenTypeFromToken(accessToken)
            if (tokenType == TokenType.ACCESS) {
                studentJwtService.getJtiFromToken(accessToken)?.let {
                    tokenRevocationService.revokeAccessToken(it)
                }
            }
        }
        refreshToken?.let { rt ->
            if (studentJwtService.validateToken(rt)) {
                val tokenType = studentJwtService.getTokenTypeFromToken(rt)
                if (tokenType == TokenType.REFRESH) {
                    studentJwtService.getJtiFromToken(rt)?.let {
                        tokenRevocationService.revokeRefreshToken(it)
                    }
                }
            }
        }
    }
}
