package com.c2c.csm.application.service.metric;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.c2c.csm.application.model.Action;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetricsService {
    private static final String COMMAND_TOTAL_METRIC = "c2c.command.total";
    private static final String COMMAND_DURATION_METRIC = "c2c.command.duration";

    private final MeterRegistry meterRegistry;

    public void incrementCommandTotal(Action action, String result) {
        String actionTag = action == null ? Action.UNKNOWN.name() : action.name();
        incrementCounter(COMMAND_TOTAL_METRIC, "action", actionTag, "result", result);
    }

    public void recordCommandDuration(Action action, String result, Duration duration) {
        String actionTag = action == null ? Action.UNKNOWN.name() : action.name();
        recordDuration(COMMAND_DURATION_METRIC, duration, "action", actionTag, "result", result);
    }

    public void recordCommandOutcome(Action action, String result, Duration duration) {
        incrementCommandTotal(action, result);
        recordCommandDuration(action, result, duration);
    }

    public void incrementCounter(String metricName, String... tags) {
        meterRegistry.counter(metricName, tags).increment();
    }

    public void recordDuration(String metricName, Duration duration, String... tags) {
        if (duration == null) {
            return;
        }
        meterRegistry.timer(metricName, tags).record(duration);
    }
}
