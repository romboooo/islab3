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
        endpoint = System.getenv("MINIO_ENDPOINT");
        if (endpoint == null) endpoint = "http://localhost:9000";

        accessKey = System.getenv("MINIO_ACCESS_KEY");
        if (accessKey == null) accessKey = "minioadmin";

        secretKey = System.getenv("MINIO_SECRET_KEY");
        if (secretKey == null) secretKey = "minioadmin";

        bucketName = System.getenv("MINIO_BUCKET_NAME");
        if (bucketName == null) bucketName = "dragon-imports";

        logger.info("MinIO Config: endpoint=" + endpoint + ", bucket=" + bucketName);
    }

    public String getEndpoint() { return endpoint; }
    public String getAccessKey() { return accessKey; }
    public String getSecretKey() { return secretKey; }
    public String getBucketName() { return bucketName; }
}