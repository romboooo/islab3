package com.example.service;

import com.example.config.MinioConfig;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.apache.commons.io.IOUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@ApplicationScoped
public class MinioService {
    private static final Logger logger = Logger.getLogger(MinioService.class.getName());

    @Inject
    private MinioConfig minioConfig;

    private MinioClient minioClient;
    private boolean initialized = false;

    @PostConstruct
    public void init() {
        try {
            logger.info("Инициализация MinIO клиента...");

            String endpoint = minioConfig.getEndpoint();
            logger.info("MinIO endpoint: " + endpoint);

            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();

            logger.info("Проверка соединения с MinIO...");
            boolean isReachable = checkMinioConnection();

            if (!isReachable) {
                logger.warning("MinIO недоступен, но продолжаем работу. Клиент будет создан при первой попытке использования.");
                return;
            }

            String bucketName = minioConfig.getBucketName();
            logger.info("Проверка bucket: " + bucketName);

            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());

            if (!bucketExists) {
                logger.info("Bucket не существует, создаем: " + bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());

                setBucketPolicy();
            }

            initialized = true;
            logger.info("MinIO клиент успешно инициализирован");

        } catch (Exception e) {
            logger.severe("Ошибка инициализации MinIO клиента: " + e.getMessage());
        }
    }

    private boolean checkMinioConnection() {
        try {
            minioClient.listBuckets();
            return true;
        } catch (Exception e) {
            logger.warning("Не удалось подключиться к MinIO: " + e.getMessage());
            return false;
        }
    }

    private void setBucketPolicy() {
        try {
            String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """.formatted(minioConfig.getBucketName());

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .config(policy)
                            .build());
        } catch (Exception e) {
            logger.warning("Не удалось установить политику bucket: " + e.getMessage());
        }
    }

    private synchronized void ensureInitialized() throws Exception {
        if (!initialized) {
            logger.info("Ленивая инициализация MinIO клиента...");
            init();

            if (!initialized) {
                throw new IllegalStateException("MinIO клиент не инициализирован. Проверьте подключение к MinIO.");
            }
        }
    }

    public String uploadFile(byte[] fileContent, String originalFilename, String contentType)
            throws Exception {

        ensureInitialized();

        String objectName = generateObjectName(originalFilename);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", contentType);
        headers.put("X-Amz-Meta-Original-Filename", originalFilename);
        headers.put("X-Amz-Meta-Uploaded-At", LocalDateTime.now().toString());

        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileContent)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(bais, fileContent.length, -1)
                            .contentType(contentType)
                            .headers(headers)
                            .build());
        }

        logger.info("Файл загружен в MinIO: " + objectName + ", размер: " + fileContent.length + " байт");
        return objectName;
    }

    public byte[] downloadFile(String objectName) throws Exception {
        ensureInitialized();

        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .build())) {
            return IOUtils.toByteArray(stream);
        }
    }

    public String getFileUrl(String objectName) throws Exception {
        ensureInitialized();

        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .expiry(7, TimeUnit.DAYS)
                        .build());
    }

    public FileMetadata getFileMetadata(String objectName) throws Exception {
        ensureInitialized();

        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(objectName)
                        .build());

        FileMetadata metadata = new FileMetadata();
        metadata.setObjectName(objectName);
        metadata.setSize(stat.size());
        metadata.setContentType(stat.contentType());
        metadata.setLastModified(stat.lastModified().toLocalDateTime());
        metadata.setOriginalFilename(stat.userMetadata().get("x-amz-meta-original-filename"));

        return metadata;
    }

    private String generateObjectName(String originalFilename) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String datePath = LocalDateTime.now().format(formatter);
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        return String.format("%s/%s%s", datePath, uuid, extension);
    }

    public static class FileMetadata {
        private String objectName;
        private String originalFilename;
        private long size;
        private String contentType;
        private LocalDateTime lastModified;

        public String getObjectName() { return objectName; }
        public void setObjectName(String objectName) { this.objectName = objectName; }
        public String getOriginalFilename() { return originalFilename; }
        public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    }
}