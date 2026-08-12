package com.company.ruanzhu.file.storage;

import java.io.InputStream;

/**
 * Abstract storage client interface for file operations.
 * Implementations include MinIO (production) and local filesystem (dev/testing).
 */
public interface StorageClient {

    /**
     * Upload a file to storage.
     *
     * @param objectName  the object name / key (may include path separators)
     * @param inputStream the file content stream
     * @param contentType the MIME content type (e.g. "application/zip")
     * @return the storage path that can be used to retrieve the file later
     */
    String upload(String objectName, InputStream inputStream, String contentType);

    /**
     * Download a file from storage.
     *
     * @param storagePath the storage path returned by {@link #upload}
     * @return the file content as a byte array
     */
    byte[] download(String storagePath);

    /**
     * Delete a file from storage.
     *
     * @param storagePath the storage path returned by {@link #upload}
     */
    void delete(String storagePath);

    /**
     * Check whether a file exists in storage.
     *
     * @param storagePath the storage path returned by {@link #upload}
     * @return true if the file exists, false otherwise
     */
    boolean exists(String storagePath);

    /**
     * Generate a pre-signed URL for temporary access to a stored file.
     *
     * @param storagePath   the storage path returned by {@link #upload}
     * @param expirySeconds how long the URL should remain valid (in seconds)
     * @return a pre-signed URL string
     */
    String getPresignedUrl(String storagePath, int expirySeconds);
}
