package edu.minervia.platform.config

import edu.minervia.platform.service.storage.ObjectMetadata
import edu.minervia.platform.service.storage.R2StorageService
import org.mockito.Mockito
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * 测试配置：当 R2 被禁用时提供 Mock 的 R2StorageService
 */
@TestConfiguration
@ConditionalOnProperty(prefix = "app.r2", name = ["enabled"], havingValue = "false")
class TestR2Config {

    @Bean
    @Primary
    fun mockR2StorageService(): R2StorageService {
        val mock = Mockito.mock(R2StorageService::class.java)

        // 配置默认行为
        Mockito.`when`(mock.generatePresignedUploadUrl(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyInt()
        )).thenReturn("https://mock-upload-url.example.com")

        Mockito.`when`(mock.generatePresignedDownloadUrl(
            Mockito.anyString(),
            Mockito.anyInt()
        )).thenReturn("https://mock-download-url.example.com")

        Mockito.`when`(mock.headObject(Mockito.anyString())).thenReturn(
            ObjectMetadata(
                contentType = "application/pdf",
                contentLength = 1024L,
                etag = "mock-etag"
            )
        )

        return mock
    }
}
