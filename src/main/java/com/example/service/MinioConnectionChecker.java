package com.example.service;

import com.example.config.MinioConfig;
import io.minio.MinioClient;
import io.minio.errors.*;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

@ApplicationScoped
public class MinioConnectionChecker {
    private static final Logger logger = Logger.getLogger(MinioConnectionChecker.class.getName());

    @Inject
    private MinioConfig minioConfig;

    @PostConstruct
    public void checkConnection() {
        try {
            logger.info("=== Проверка подключения к MinIO ===");
            logger.info("Endpoint: " + minioConfig.getEndpoint());
            logger.info("AccessKey: " + minioConfig.getAccessKey());
            logger.info("Bucket: " + minioConfig.getBucketName());

            MinioClient testClient = MinioClient.builder()
                    .endpoint(minioConfig.getEndpoint())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();

            logger.info("Попытка подключения к MinIO...");
            testClient.listBuckets();
            logger.info("Успешное подключение к MinIO!");

        } catch (Exception e) {
            logger.severe("ОШИБКА подключения к MinIO: " + e.getMessage());
            e.printStackTrace();
        }
    }
}