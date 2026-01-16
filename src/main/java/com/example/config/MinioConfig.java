package com.example.config;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.logging.Logger;

@ApplicationScoped
public class MinioConfig {
    private static final Logger logger = Logger.getLogger(MinioConfig.class.getName());

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;

    @PostConstruct
    public void init() {
        // Проверяем, запущено ли приложение в Docker
        boolean isInDocker = System.getenv("MINIO_ENDPOINT") != null;

        if (isInDocker) {
            // В Docker - используем имя сервиса
            endpoint = System.getenv("MINIO_ENDPOINT");
            logger.info("Running in Docker, using MinIO endpoint: " + endpoint);
        } else {
            // Локально - используем localhost
            endpoint = "http://localhost:9000";
            logger.info("Running locally, using MinIO endpoint: " + endpoint);
        }

        accessKey = System.getenv("MINIO_ACCESS_KEY");
        if (accessKey == null) accessKey = "minioadmin";

        secretKey = System.getenv("MINIO_SECRET_KEY");
        if (secretKey == null) secretKey = "minioadmin";

        bucketName = System.getenv("MINIO_BUCKET_NAME");
        if (bucketName == null) bucketName = "dragon-imports";

        logger.info("MinIO Config: endpoint=" + endpoint +
                ", bucket=" + bucketName +
                ", accessKey=" + accessKey);
    }

    public String getEndpoint() { return endpoint; }
    public String getAccessKey() { return accessKey; }
    public String getSecretKey() { return secretKey; }
    public String getBucketName() { return bucketName; }
}