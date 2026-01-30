package edu.minervia.platform.domain.repository

import edu.minervia.platform.domain.entity.SystemConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SystemConfigRepository : JpaRepository<SystemConfig, Long> {
    fun findByConfigKey(configKey: String): Optional<SystemConfig>
    fun existsByConfigKey(configKey: String): Boolean
}
