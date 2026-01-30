package edu.minervia.platform.domain.entity

import edu.minervia.platform.domain.enums.RegistrationCodeStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "registration_codes")
class RegistrationCode(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 32)
    val code: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RegistrationCodeStatus = RegistrationCodeStatus.UNUSED,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    val createdBy: Admin,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    var usedBy: Admin? = null,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    fun isExpired(): Boolean = expiresAt.isBefore(Instant.now())

    fun isValid(): Boolean = status == RegistrationCodeStatus.UNUSED && !isExpired()
}
