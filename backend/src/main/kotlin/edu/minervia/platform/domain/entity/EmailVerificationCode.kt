package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "email_verification_codes")
class EmailVerificationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    val application: RegistrationApplication,

    @Column(nullable = false, length = 6)
    val code: String,

    @Column(nullable = false)
    var attempts: Int = 0,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "verified_at")
    var verifiedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun isExpired(): Boolean = expiresAt.isBefore(Instant.now())

    fun isValid(): Boolean = verifiedAt == null && !isExpired()
}
