package edu.minervia.platform.domain.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "student_family_info")
class StudentFamilyInfo(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    val student: Student,

    @Column(name = "father_name", length = 200)
    var fatherName: String? = null,

    @Column(name = "father_occupation", length = 100)
    var fatherOccupation: String? = null,

    @Column(name = "mother_name", length = 200)
    var motherName: String? = null,

    @Column(name = "mother_occupation", length = 100)
    var motherOccupation: String? = null,

    @Column(name = "home_address", columnDefinition = "TEXT")
    var homeAddress: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
