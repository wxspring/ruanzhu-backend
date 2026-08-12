package com.company.ruanzhu.file.storage;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.profile", havingValue = "minio", matchIfMissing = true)
public class MinioStorageClient implements StorageClient {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Override
    public String upload(String objectName, InputStream inputStream, String contentType) {
        try {
            // Convert to ByteArrayInputStream so we know the size (MinIO requires it for single-part put)
            byte[] data = inputStream.readAllBytes();
            try (InputStream bais = new ByteArrayInputStream(data)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(bais, data.length, -1)
                        .contentType(contentType)
                        .build());
            }
            log.debug("Uploaded object '{}' to MinIO bucket '{}'", objectName, bucketName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload object '{}' to MinIO: {}", objectName, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public byte[] download(String storagePath) {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(storagePath)
                .build())) {
            return is.readAllBytes();
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                log.warn("Object '{}' not found in MinIO bucket '{}'", storagePath, bucketName);
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
            log.error("Failed to download object '{}' from MinIO: {}", storagePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to download object '{}' from MinIO: {}", storagePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build());
            log.debug("Deleted object '{}' from MinIO bucket '{}'", storagePath, bucketName);
        } catch (Exception e) {
            log.error("Failed to delete object '{}' from MinIO: {}", storagePath, e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storagePath)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("Failed to stat object '{}' in MinIO: {}", storagePath, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Failed to check existence of object '{}' in MinIO: {}", storagePath, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public String getPresignedUrl(String storagePath, int expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(bucketName)
                            .object(storagePath)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for '{}': {}", storagePath, e.getMessage(), e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }
}
