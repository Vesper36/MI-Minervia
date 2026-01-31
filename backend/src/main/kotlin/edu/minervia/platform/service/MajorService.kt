package edu.minervia.platform.service

import edu.minervia.platform.domain.entity.Major
import edu.minervia.platform.domain.repository.MajorRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MajorService(
    private val majorRepository: MajorRepository
) {
    fun findById(id: Long): Major? = majorRepository.findById(id).orElse(null)

    fun findByCode(code: String): Major? = majorRepository.findByCode(code).orElse(null)

    fun findAllActive(): List<Major> = majorRepository.findAllByIsActiveTrue()

    fun getCodeById(id: Long): String = findById(id)?.code ?: DEFAULT_MAJOR_CODE

    @Transactional
    fun create(code: String, nameEn: String, namePl: String, nameZh: String? = null): Major {
        require(!majorRepository.existsByCode(code)) { "Major with code $code already exists" }
        return majorRepository.save(
            Major(
                code = code,
                nameEn = nameEn,
                namePl = namePl,
                nameZh = nameZh
            )
        )
    }

    companion object {
        const val DEFAULT_MAJOR_CODE = "CS"
    }
}
