package com.example.service;

import com.example.dao.DragonDao;
import com.example.dao.ImportHistoryDao;
import com.example.dto.DragonDto;
import com.example.entity.ImportHistory;
import com.example.entity.ImportStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Stateless
public class ImportService {

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
        ImportHistory importHistory = new ImportHistory(
                LocalDateTime.now(),
                ImportStatus.IN_PROGRESS,
                filename
        );
        importHistory = importHistoryDao.save(importHistory);

        int recordsProcessed = 0;
        int totalRecords = 0;
        List<String> errors = new ArrayList<>();
        boolean hasFatalError = false;

        try {
            byte[] fileContent = fileInputStream.readAllBytes();

            String objectKey = minioService.uploadFile(
                    fileContent,
                    filename,
                    "application/json"
            );

            String fileUrl = minioService.getFileUrl(objectKey);
            importHistory.setFileSize(fileSize);
            importHistory.setFileObjectKey(objectKey);
            importHistory.setFileUrl(fileUrl);
            importHistoryDao.save(importHistory);

            JsonNode rootNode = objectMapper.readTree(new ByteArrayInputStream(fileContent));

            if (!rootNode.isArray()) {
                throw new IllegalArgumentException("JSON должен быть массивом объектов");
            }

            totalRecords = rootNode.size();

            if (totalRecords == 0) {
                throw new IllegalArgumentException("JSON файл не содержит данных");
            }

            for (int i = 0; i < totalRecords; i++) {
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

        } catch (JsonProcessingException e) {
            hasFatalError = true;
            String errorMsg = e.getOriginalMessage();
            if (errorMsg.contains("not a valid")) {
                errorMsg = errorMsg.substring(0, errorMsg.indexOf(" at [Source:"));
            }
            errors.add("Ошибка формата JSON: " + errorMsg);
        } catch (Exception e) {
            hasFatalError = true;
            errors.add("Фатальная ошибка: " + e.getMessage());
        }

        importHistory.setEndTime(LocalDateTime.now());

        if (hasFatalError) {
            importHistory.setStatus(ImportStatus.FAILED);
            importHistory.setRecordsProcessed(0);
            importHistory.setErrorMessage(String.join("; ", errors));
        } else if (errors.isEmpty()) {
            importHistory.setStatus(ImportStatus.SUCCESS);
            importHistory.setRecordsProcessed(recordsProcessed);
        } else if (recordsProcessed > 0) {
            importHistory.setStatus(ImportStatus.PARTIAL_SUCCESS);
            importHistory.setRecordsProcessed(recordsProcessed);
            importHistory.setErrorMessage("Обработано " + recordsProcessed + " из " + totalRecords + " записей. Ошибки: " +
                    String.join("; ", errors));
        } else {
            importHistory.setStatus(ImportStatus.FAILED);
            importHistory.setRecordsProcessed(0);
            importHistory.setErrorMessage("Ни одна запись не обработана. Ошибки: " + String.join("; ", errors));
        }

        return importHistoryDao.save(importHistory);
    }

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