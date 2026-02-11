package edu.minervia.platform.security

data class StudentPrincipal(
    val studentId: Long,
    val studentNumber: String,
    val eduEmail: String,
    val fullName: String
)
