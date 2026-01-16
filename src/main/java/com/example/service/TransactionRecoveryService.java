// src/main/java/com/example/service/TransactionRecoveryService.java
package com.example.service;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import java.util.logging.Logger;

@Singleton
@Startup
public class TransactionRecoveryService {
    private static final Logger logger = Logger.getLogger(TransactionRecoveryService.class.getName());

    @Inject
    private ImportService importService;

    @PostConstruct
    public void init() {
        logger.info("Сервис восстановления транзакций запущен");
        importService.recoverFailedTransactions();
    }

    @Schedule(hour = "*", minute = "*/5", persistent = false)
    public void recoverTransactions() {
        logger.info("Запуск восстановления неудавшихся транзакций");
        importService.recoverFailedTransactions();
    }
}