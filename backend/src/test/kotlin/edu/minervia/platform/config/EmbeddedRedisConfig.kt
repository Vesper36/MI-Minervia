package edu.minervia.platform.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.test.context.TestConfiguration
import redis.embedded.RedisServer

/**
 * 测试配置：启动嵌入式 Redis 服务器
 */
@TestConfiguration
class EmbeddedRedisConfig {

    private var redisServer: RedisServer? = null

    @PostConstruct
    fun startRedis() {
        try {
            redisServer = RedisServer(6379)
            redisServer?.start()
        } catch (e: Exception) {
            // Redis 可能已经在运行，忽略错误
            println("Failed to start embedded Redis: ${e.message}")
        }
    }

    @PreDestroy
    fun stopRedis() {
        redisServer?.stop()
    }
}
