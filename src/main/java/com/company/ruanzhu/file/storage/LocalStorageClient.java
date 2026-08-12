package com.company.ruanzhu.file.storage;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem implementation of {@link StorageClient}.
 * Active only when storage.profile=local (typically via the "dev" Spring profile).
 * Stores files under ./uploads/.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "storage.profile", havingValue = "local")
public class LocalStorageClient implements StorageClient {

    private static final String UPLOAD_BASE_DIR = "./uploads";

    @Override
    public String upload(String objectName, InputStream inputStream, String contentType) {
        Path filePath = resolvePath(objectName);
        try {
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Saved file locally: {}", filePath);
            return objectName;
        } catch (IOException e) {
            log.error("Failed to save file locally: {}", filePath, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Override
    public byte[] download(String storagePath) {
        Path filePath = resolvePath(storagePath);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read local file: {}", filePath, e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    public void delete(String storagePath) {
        Path filePath = resolvePath(storagePath);
        try {
            Files.deleteIfExists(filePath);
            log.debug("Deleted local file: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to delete local file: {}", filePath, e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        return Files.exists(resolvePath(storagePath));
    }

    @Override
    public String getPresignedUrl(String storagePath, int expirySeconds) {
        // Local storage does not support pre-signed URLs; return a relative path instead.
        log.debug("Pre-signed URLs are not supported by local storage; returning path: {}", storagePath);
        return "/uploads/" + storagePath;
    }

    private Path resolvePath(String objectName) {
        return Paths.get(UPLOAD_BASE_DIR, objectName).normalize();
    }
}
