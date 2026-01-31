package com.larrydevincarter.thufir.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class OptionScannerUpdateMonitor {

    private static final Logger log = LoggerFactory.getLogger(OptionScannerUpdateMonitor.class);

    private final AtomicBoolean potentiallyUpdating = new AtomicBoolean(false);

//    @Scheduled(fixedRate = 600000000)
    @Scheduled(cron = "0 0 2 * * *", zone = "America/Chicago")
    public void startUpdatePeriod() {
        potentiallyUpdating.set(true);
        log.info("OptionScanner daily update period flagged — Thufir will check status before calls");
    }

    public boolean isPotentiallyUpdating() {
        return potentiallyUpdating.get();
    }

    public void clearUpdateFlag() {
        potentiallyUpdating.set(false);
        log.info("OptionScanner update flag cleared for the day");
    }
}