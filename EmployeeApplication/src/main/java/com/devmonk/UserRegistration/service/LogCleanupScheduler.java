package com.devmonk.UserRegistration.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.devmonk.UserRegistration.repository.ActivityLogRepository;

@Component
public class LogCleanupScheduler {
	
	@Autowired
    private ActivityLogRepository activityLogRepository;

	@Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
	@Transactional
    public void deleteOldLogs() {
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        activityLogRepository.deleteByTimestampBefore(threeDaysAgo);
    }

}
