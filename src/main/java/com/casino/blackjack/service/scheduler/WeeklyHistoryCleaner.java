package com.casino.blackjack.service.scheduler;

import com.casino.blackjack.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WeeklyHistoryCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyHistoryCleaner.class);

    private final AdminService adminService;

    public WeeklyHistoryCleaner(AdminService adminService) {
        this.adminService = adminService;
    }

    // Every Sunday at 00:00:00 server time
    @Scheduled(cron = "0 0 0 * * SUN")
    public void clearHistory() {
        LOGGER.info("Weekly scheduled task: clearing all hand history and active game states.");
        adminService.clearAllHistory();
        LOGGER.info("Weekly history clear completed.");
    }
}
