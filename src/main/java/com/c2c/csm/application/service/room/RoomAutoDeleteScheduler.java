package com.c2c.csm.application.service.room;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomAutoDeleteScheduler {
    private final RoomRegistryService roomRegistryService;

    @Scheduled(cron = "0 0 0 * * *")
    public void deleteExpiredRooms() {
        int deleted = roomRegistryService.deleteExpiredRooms(Instant.now());
        if (deleted > 0) {
            log.info("room auto delete completed deletedRooms={}", deleted);
        }
    }
}
