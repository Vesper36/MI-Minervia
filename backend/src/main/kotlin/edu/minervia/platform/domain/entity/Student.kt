package edu.minervia.platform.domain.entity

import edu.minervia.platform.domain.enums.IdentityType
import edu.minervia.platform.domain.enums.StudentStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "students")
class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "student_number", nullable = false, unique = true, length = 20)
    val studentNumber: String,

    @Column(name = "edu_email", nullable = false, unique = true)
    val eduEmail: String,

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id")
    val application: RegistrationApplication? = null,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false)
    var identityType: IdentityType,

    @Column(name = "country_code", nullable = false, length = 3)
    var countryCode: String,

    @Column(name = "major_id")
    var majorId: Long? = null,

    @Column(name = "class_id")
    var classId: Long? = null,

    @Column(name = "enrollment_year", nullable = false)
    val enrollmentYear: Int,

    @Column(name = "enrollment_date", nullable = false)
    val enrollmentDate: LocalDate,

    @Column(name = "admission_date", nullable = false)
    val admissionDate: LocalDate,

    @Column(precision = 3, scale = 2)
    var gpa: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: StudentStatus = StudentStatus.ACTIVE,

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    var suspensionReason: String? = null,

    @Column(name = "daily_email_limit", nullable = false)
    var dailyEmailLimit: Int = 1,

    @Column(name = "is_simulated", nullable = false)
    val isSimulated: Boolean = true,

    @Column(name = "generation_seed")
    val generationSeed: String? = null,

    @Column(name = "generation_version", length = 20)
    val generationVersion: String? = null,

    @Column(name = "photo_url", length = 500)
    var photoUrl: String? = null,

    @Column(name = "family_background", columnDefinition = "TEXT")
    var familyBackground: String? = null,

    @Column(columnDefinition = "TEXT")
    var interests: String? = null,

    @Column(name = "academic_goals", columnDefinition = "TEXT")
    var academicGoals: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    val fullName: String
        get() = "$firstName $lastName"
}
