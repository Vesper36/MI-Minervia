package edu.minervia.platform.domain.entity

import edu.minervia.platform.domain.enums.ApplicationStatus
import edu.minervia.platform.domain.enums.IdentityType
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "registration_applications")
class RegistrationApplication(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_code_id", nullable = false)
    val registrationCode: RegistrationCode,

    @Column(name = "external_email", nullable = false)
    val externalEmail: String,

    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false)
    var identityType: IdentityType,

    @Column(name = "country_code", length = 3)
    var countryCode: String? = null,

    @Column(name = "major_id")
    var majorId: Long? = null,

    @Column(name = "class_id")
    var classId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ApplicationStatus = ApplicationStatus.CODE_VERIFIED,

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    var rejectionReason: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    var approvedBy: Admin? = null,

    @Column(name = "approved_at")
    var approvedAt: Instant? = null,

    @Column(name = "oauth_provider", length = 50)
    var oauthProvider: String? = null,

    @Column(name = "oauth_user_id")
    var oauthUserId: String? = null,

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,

    @Column(name = "user_agent", columnDefinition = "TEXT")
    var userAgent: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
