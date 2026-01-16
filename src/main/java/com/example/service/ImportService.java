package com.example.service;

import com.example.dao.DragonDao;
import com.example.dao.ImportHistoryDao;
import com.example.dto.DragonDto;
import com.example.entity.ImportHistory;
import com.example.entity.ImportStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class ImportService {
    private static final Logger logger = Logger.getLogger(ImportService.class.getName());
    private static final Map<String, ImportContext> activeTransactions = new HashMap<>();

    @Inject
    private DragonDao dragonDao;

    @Inject
    private ImportHistoryDao importHistoryDao;

    @Inject
    private DragonService dragonService;

    @Inject
    private Validator validator;

    @Inject
    private MinioService minioService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ImportService() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    public ImportHistory importDragonsFromJson(InputStream fileInputStream, String filename, long fileSize) {

        if ("true".equals(System.getProperty("business.fail"))) {
            throw new RuntimeException("Business logic error occurred");
        }

        String transactionId = generateTransactionId();
        logger.info("Начало распределенной транзакции: " + transactionId);

        ImportContext context = null;

        try {
            // Фаза 1: Prepare - выполняем все операции, но не фиксируем их окончательно
            context = prepareImport(transactionId, fileInputStream, filename, fileSize);
            logger.info("Фаза prepare завершена для транзакции: " + transactionId);

            // Фаза 2: Commit - фиксируем все изменения
            ImportHistory importHistory = commitImport(context);
            logger.info("Фаза commit завершена для транзакции: " + transactionId);

            // Удаляем контекст из памяти после успешного завершения
            activeTransactions.remove(transactionId);

            return importHistory;
        } catch (Exception e) {
            logger.severe("Ошибка при импорте: " + e.getMessage());

            // Если контекст существует, выполняем rollback
            if (context != null) {
                try {
                    rollbackImport(context);
                    logger.info("Rollback выполнен для транзакции: " + transactionId);
                } catch (Exception rollbackException) {
                    logger.severe("Ошибка при rollback транзакции " + transactionId + ": " + rollbackException.getMessage());
                }
            }

            // Создаем запись об ошибке в ImportHistory
            ImportHistory failedHistory = new ImportHistory(
                    LocalDateTime.now(),
                    ImportStatus.FAILED,
                    filename
            );
            failedHistory.setErrorMessage("Ошибка импорта: " + e.getMessage());
            return importHistoryDao.save(failedHistory);
        }
    }

    private ImportContext prepareImport(String transactionId, InputStream fileInputStream, String filename, long fileSize) throws Exception {
        logger.info("Начало фазы prepare для транзакции: " + transactionId);

        // 1. Читаем содержимое файла
        byte[] fileContent = fileInputStream.readAllBytes();

        // 2. Загружаем файл в MinIO
        logger.info("Загрузка файла в MinIO для транзакции: " + transactionId);
        String objectKey = minioService.uploadFile(fileContent, filename, "application/json");
        String fileUrl = minioService.getFileUrl(objectKey);
        logger.info("Файл загружен в MinIO: " + objectKey + " для транзакции: " + transactionId);

        // 3. Валидируем данные
        JsonNode rootNode = objectMapper.readTree(new ByteArrayInputStream(fileContent));

        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("JSON должен быть массивом объектов");
        }

        int totalRecords = rootNode.size();
        if (totalRecords == 0) {
            throw new IllegalArgumentException("JSON файл не содержит данных");
        }

        logger.info("Валидация данных для транзакции: " + transactionId + ", записей: " + totalRecords);

        // 4. Создаем контекст транзакции
        ImportContext context = new ImportContext();
        context.setTransactionId(transactionId);
        context.setObjectKey(objectKey);
        context.setFileUrl(fileUrl);
        context.setFilename(filename);
        context.setFileSize(fileSize);
        context.setFileContent(fileContent);
        context.setTotalRecords(totalRecords);

        // 5. Сохраняем контекст в памяти
        activeTransactions.put(transactionId, context);

        return context;
    }

    private ImportHistory commitImport(ImportContext context) throws Exception {
        logger.info("Начало фазы commit для транзакции: " + context.getTransactionId());

        // 1. Создаем запись в ImportHistory
        ImportHistory importHistory = new ImportHistory(
                LocalDateTime.now(),
                ImportStatus.IN_PROGRESS,
                context.getFilename()
        );
        importHistory.setFileSize(context.getFileSize());
        importHistory.setFileObjectKey(context.getObjectKey());
        importHistory.setFileUrl(context.getFileUrl());
        importHistory = importHistoryDao.save(importHistory);
        logger.info("Создана запись ImportHistory ID: " + importHistory.getId() + " для транзакции: " + context.getTransactionId());

        // 2. Обрабатываем записи
        int recordsProcessed = 0;
        List<String> errors = new ArrayList<>();

        JsonNode rootNode = objectMapper.readTree(new ByteArrayInputStream(context.getFileContent()));

        for (int i = 0; i < context.getTotalRecords(); i++) {
            JsonNode dragonNode = rootNode.get(i);

            try {
                DragonDto dragonDto;
                try {
                    dragonDto = objectMapper.treeToValue(dragonNode, DragonDto.class);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Некорректный формат данных в записи " + (i + 1) + ": " +
                            e.getMessage().replaceAll("at \\[Source: .*?; line: \\d+, column: \\d+\\]", ""));
                }

                Set<ConstraintViolation<DragonDto>> violations = validator.validate(dragonDto);
                if (!violations.isEmpty()) {
                    StringBuilder errorMsg = new StringBuilder();
                    for (ConstraintViolation<DragonDto> violation : violations) {
                        errorMsg.append(violation.getPropertyPath())
                                .append(": ")
                                .append(violation.getMessage())
                                .append("; ");
                    }
                    throw new IllegalArgumentException("Запись " + (i + 1) + ": " + errorMsg.toString());
                }

                if (!dragonService.isDragonNameUnique(dragonDto.getName())) {
                    throw new IllegalArgumentException("Запись " + (i + 1) + ": Дракон с именем '" +
                            dragonDto.getName() + "' уже существует");
                }

                if (dragonDto.getAge() <= 0) {
                    throw new IllegalArgumentException("Запись " + (i + 1) + ": Возраст должен быть больше 0 (передано: " +
                            dragonDto.getAge() + ")");
                }

                if (dragonDto.getWeight() == null || dragonDto.getWeight() <= 0) {
                    throw new IllegalArgumentException("Запись " + (i + 1) + ": Вес должен быть числом больше 0");
                }

                dragonService.save(dragonDto);
                recordsProcessed++;
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        }

        // 3. Обновляем статус ImportHistory
        importHistory.setEndTime(LocalDateTime.now());
        if (errors.isEmpty()) {
            importHistory.setStatus(ImportStatus.SUCCESS);
            importHistory.setRecordsProcessed(recordsProcessed);
        } else if (recordsProcessed > 0) {
            importHistory.setStatus(ImportStatus.PARTIAL_SUCCESS);
            importHistory.setRecordsProcessed(recordsProcessed);
            importHistory.setErrorMessage("Обработано " + recordsProcessed + " из " + context.getTotalRecords() + " записей. Ошибки: " +
                    String.join("; ", errors));
        } else {
            importHistory.setStatus(ImportStatus.FAILED);
            importHistory.setRecordsProcessed(0);
            importHistory.setErrorMessage("Ни одна запись не обработана. Ошибки: " + String.join("; ", errors));
        }

        return importHistoryDao.save(importHistory);
    }

    private void rollbackImport(ImportContext context) {
        logger.info("Начало rollback для транзакции: " + context.getTransactionId());

        try {
            // 1. Удаляем файл из MinIO
            if (context.getObjectKey() != null) {
                logger.info("Удаление файла из MinIO: " + context.getObjectKey() + " для транзакции: " + context.getTransactionId());
                minioService.deleteFile(context.getObjectKey());
                logger.info("Файл удален из MinIO для транзакции: " + context.getTransactionId());
            }

            // 2. Удаляем контекст из памяти
            activeTransactions.remove(context.getTransactionId());

        } catch (Exception e) {
            logger.severe("Ошибка при rollback транзакции " + context.getTransactionId() + ": " + e.getMessage());
            throw new RuntimeException("Ошибка при rollback транзакции", e);
        }
    }

    private String generateTransactionId() {
        return UUID.randomUUID().toString();
    }

    // Восстановление неудавшихся транзакций при старте приложения
    public void recoverFailedTransactions() {
        logger.info("Начало восстановления неудавшихся транзакций");

        // Копируем ключи, чтобы избежать ConcurrentModificationException
        List<String> transactionIds = new ArrayList<>(activeTransactions.keySet());

        for (String transactionId : transactionIds) {
            ImportContext context = activeTransactions.get(transactionId);
            logger.info("Восстановление транзакции: " + transactionId);

            try {
                // Пытаемся завершить транзакцию
                commitImport(context);
                logger.info("Транзакция успешно завершена: " + transactionId);
            } catch (Exception e) {
                logger.warning("Не удалось завершить транзакцию " + transactionId + ": " + e.getMessage());

                // Если не удалось завершить, выполняем rollback
                try {
                    rollbackImport(context);
                    logger.info("Rollback выполнен для транзакции: " + transactionId);
                } catch (Exception rollbackException) {
                    logger.severe("Ошибка при rollback транзакции " + transactionId + ": " + rollbackException.getMessage());
                }
            }
        }
    }

    // Класс для хранения контекста транзакции в памяти
    private static class ImportContext {
        private String transactionId;
        private String objectKey;
        private String fileUrl;
        private String filename;
        private long fileSize;
        private byte[] fileContent;
        private int totalRecords;

        // Геттеры и сеттеры
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getObjectKey() { return objectKey; }
        public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

        public String getFileUrl() { return fileUrl; }
        public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public byte[] getFileContent() { return fileContent; }
        public void setFileContent(byte[] fileContent) { this.fileContent = fileContent; }

        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    }

    // Существующие методы для REST API (оставляем без изменений)
    public byte[] getImportFile(Long importId) throws Exception {
        ImportHistory importHistory = importHistoryDao.findById(importId);
        if (importHistory == null || importHistory.getFileObjectKey() == null) {
            throw new FileNotFoundException("File not found for import id: " + importId);
        }
        return minioService.downloadFile(importHistory.getFileObjectKey());
    }

    public MinioService.FileMetadata getImportFileMetadata(Long importId) throws Exception {
        ImportHistory importHistory = importHistoryDao.findById(importId);
        if (importHistory == null || importHistory.getFileObjectKey() == null) {
            throw new FileNotFoundException("File not found for import id: " + importId);
        }
        return minioService.getFileMetadata(importHistory.getFileObjectKey());
    }

    public List<ImportHistory> getImportHistory() {
        return importHistoryDao.findAll();
    }

    public ImportHistory getImportHistoryById(Long id) {
        return importHistoryDao.findById(id);
    }
}