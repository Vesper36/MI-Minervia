package edu.minervia.platform.service.storage

import edu.minervia.platform.config.R2Properties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

@Service
@ConditionalOnProperty(prefix = "app.r2", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class R2StorageService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val r2Properties: R2Properties
) {

    fun generatePresignedUploadUrl(objectKey: String, contentType: String, ttlSeconds: Int): String {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(r2Properties.bucket)
            .key(objectKey)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(ttlSeconds.toLong()))
            .putObjectRequest(putObjectRequest)
            .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    fun generatePresignedDownloadUrl(objectKey: String, ttlSeconds: Int): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(r2Properties.bucket)
            .key(objectKey)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(ttlSeconds.toLong()))
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    fun headObject(objectKey: String): ObjectMetadata {
        val headObjectRequest = HeadObjectRequest.builder()
            .bucket(r2Properties.bucket)
            .key(objectKey)
            .build()

        val response = s3Client.headObject(headObjectRequest)

        return ObjectMetadata(
            contentType = response.contentType(),
            contentLength = response.contentLength(),
            etag = response.eTag()
        )
    }

    fun deleteObject(objectKey: String) {
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(r2Properties.bucket)
            .key(objectKey)
            .build()

        s3Client.deleteObject(deleteObjectRequest)
    }
}

data class ObjectMetadata(
    val contentType: String?,
    val contentLength: Long,
    val etag: String?
)
