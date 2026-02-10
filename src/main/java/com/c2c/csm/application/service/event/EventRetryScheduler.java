// package com.c2c.csm.application.service.event;

// import java.time.Instant;

// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Component;

// import com.c2c.csm.infrastructure.registry.EventRegistry;

// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// @Component
// @RequiredArgsConstructor
// public class EventRetryScheduler {
//     private static final int BATCH_SIZE = 1000;

//     private final EventRegistry eventRegistry;

//     @Scheduled(cron = "0 0 * * * *")
//     public void pollDueEvents() {
//         var dueEvents = eventRegistry.findDue(Instant.now(), BATCH_SIZE);
//         if (dueEvents.isEmpty()) {
//             return;
//         }
//         log.info("Found {} due events for retry (retry logic not wired yet).", dueEvents.size());
//     }
// }
