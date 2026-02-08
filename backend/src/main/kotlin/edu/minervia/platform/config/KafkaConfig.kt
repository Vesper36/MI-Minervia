package edu.minervia.platform.config

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.KafkaAdmin

/**
 * Kafka configuration following CONSTRAINT [KAFKA-TOPIC-CONFIG]:
 * - partitions=6
 * - replication_factor=3 (or 1 for dev)
 * - retention.ms=604800000 (7 days)
 */
@Configuration
@ConditionalOnProperty(name = ["app.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaConfig {

    companion object {
        const val TOPIC_REGISTRATION_TASKS = "registration-tasks"
        const val TOPIC_PROGRESS_EVENTS = "progress-events"

        // Default partition count per CONSTRAINT
        const val DEFAULT_PARTITIONS = 6
        // Replication factor (use 1 for dev, 3 for prod)
        const val DEV_REPLICATION_FACTOR = 1
        const val PROD_REPLICATION_FACTOR = 3
        // Retention: 7 days in ms
        const val RETENTION_MS = "604800000"
    }

    @Value("\${spring.kafka.bootstrap-servers:localhost:9092}")
    private lateinit var bootstrapServers: String

    @Value("\${app.kafka.replication-factor:1}")
    private var replicationFactor: Int = DEV_REPLICATION_FACTOR

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs = mapOf(
            AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers
        )
        return KafkaAdmin(configs)
    }

    @Bean
    fun registrationTasksTopic(): NewTopic {
        return TopicBuilder.name(TOPIC_REGISTRATION_TASKS)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(replicationFactor)
            .config("retention.ms", RETENTION_MS)
            .build()
    }

    @Bean
    fun progressEventsTopic(): NewTopic {
        return TopicBuilder.name(TOPIC_PROGRESS_EVENTS)
            .partitions(DEFAULT_PARTITIONS)
            .replicas(replicationFactor)
            .config("retention.ms", RETENTION_MS)
            .build()
    }
}
