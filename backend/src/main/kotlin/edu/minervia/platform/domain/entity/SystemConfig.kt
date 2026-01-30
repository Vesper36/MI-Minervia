package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "system_configs")
class SystemConfig(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    val configKey: String,

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    var configValue: String,

    @Column(length = 500)
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    var updatedBy: Admin? = null,

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
