package com.company.ruanzhu.common.config;

import com.company.ruanzhu.file.storage.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Storage configuration that selects the active {@link StorageClient} implementation
 * based on the {@code storage.profile} property.
 *
 * <ul>
 *   <li>{@code storage.profile=minio} (default) – uses {@code MinioStorageClient}</li>
 *   <li>{@code storage.profile=local} – uses {@code LocalStorageClient} (dev only)</li>
 * </ul>
 *
 * The actual bean creation is handled by {@code @ConditionalOnProperty} on each
 * implementation class; this configuration class simply marks the MinIO client as
 * the primary bean when multiple candidates exist.
 */
@Slf4j
@Configuration
public class StorageConfig {

    /**
     * Fallback local storage client used when no other StorageClient bean is available
     * (e.g. when MinIO is unreachable and no local profile is active).
     * This ensures the application can still start in constrained environments.
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(StorageClient.class)
    public StorageClient fallbackStorageClient() {
        log.warn("No StorageClient bean found. This should not happen in normal configuration. "
                + "Check storage.profile property (expected 'minio' or 'local').");
        // Return a no-op implementation to avoid startup failures
        return new NoOpStorageClient();
    }

    /**
     * Minimal no-op implementation used only as a last-resort fallback.
     */
    private static class NoOpStorageClient implements StorageClient {
        @Override
        public String upload(String objectName, java.io.InputStream inputStream, String contentType) {
            throw new UnsupportedOperationException("No storage backend configured");
        }

        @Override
        public byte[] download(String storagePath) {
            throw new UnsupportedOperationException("No storage backend configured");
        }

        @Override
        public void delete(String storagePath) {
            throw new UnsupportedOperationException("No storage backend configured");
        }

        @Override
        public boolean exists(String storagePath) {
            return false;
        }

        @Override
        public String getPresignedUrl(String storagePath, int expirySeconds) {
            throw new UnsupportedOperationException("No storage backend configured");
        }
    }
}
