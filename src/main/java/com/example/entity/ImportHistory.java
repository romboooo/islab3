package com.example.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_history")
public class ImportHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "import_history_seq")
    @SequenceGenerator(name = "import_history_seq", sequenceName = "import_history_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "filename")
    private String filename;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public ImportHistory() {}

    public ImportHistory(LocalDateTime startTime, ImportStatus status, String filename) {
        this.startTime = startTime;
        this.status = status;
        this.filename = filename;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }

    public Integer getRecordsProcessed() { return recordsProcessed; }
    public void setRecordsProcessed(Integer recordsProcessed) { this.recordsProcessed = recordsProcessed; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}