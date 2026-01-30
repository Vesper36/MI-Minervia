package edu.minervia.platform.domain.entity

import edu.minervia.platform.domain.enums.AdminRole
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "admins")
class Admin(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 50)
    var username: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: AdminRole = AdminRole.ADMIN,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null,

    @Column(name = "totp_secret")
    var totpSecret: String? = null,

    @Column(name = "totp_enabled", nullable = false)
    var totpEnabled: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    fun isLocked(): Boolean = lockedUntil?.isAfter(Instant.now()) == true
}
