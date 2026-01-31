package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "majors")
class Major(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 10)
    val code: String,

    @Column(name = "name_en", nullable = false, length = 100)
    val nameEn: String,

    @Column(name = "name_pl", nullable = false, length = 100)
    val namePl: String,

    @Column(name = "name_zh", length = 100)
    val nameZh: String? = null,

    @Column(name = "faculty_id")
    val facultyId: Long? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    fun getName(locale: String): String = when (locale) {
        "pl" -> namePl
        "zh", "zh-CN", "zh-TW" -> nameZh ?: nameEn
        else -> nameEn
    }
}
