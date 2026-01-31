package edu.minervia.platform.security

import edu.minervia.platform.domain.entity.Student
import edu.minervia.platform.domain.enums.StudentStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class StudentUserDetails(
    private val student: Student
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_STUDENT"))
    }

    override fun getPassword(): String = student.passwordHash

    override fun getUsername(): String = student.studentNumber

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = student.status == StudentStatus.ACTIVE

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = student.status == StudentStatus.ACTIVE

    fun getStudentId(): Long = student.id

    fun getStudent(): Student = student

    fun getEduEmail(): String = student.eduEmail

    fun getFullName(): String = student.fullName
}
