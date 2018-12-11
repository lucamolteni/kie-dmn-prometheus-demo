package org.kie.dmn.prometheus.demo;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import io.prometheus.client.Histogram;
import org.kie.dmn.model.api.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionTimer.class);

    private final Histogram histogram = Histogram.build().name("dmn_evaluate_decision_second")
            .help("DMN Evaluation Time")
            .labelNames("decision_name")
            .register();

    private Map<DecisionTimerKey, DecisionTimerValue> map = new ConcurrentHashMap<>();

    public DecisionTimerKey scheduleTimer(Decision decision, long id) {

        DecisionTimer.DecisionTimerKey key = new DecisionTimerKey(decision, Thread.currentThread().getId());

        map.computeIfAbsent(key, k -> {
            String decisionName = getDecisionName(decision);
            return new DecisionTimerValue(histogram.labels(decisionName).startTimer(), Instant.now());
        });

        return new DecisionTimerKey(decision, id);
    }

    public void registerTimer(Decision decision) {
        DecisionTimerKey key = new DecisionTimerKey(decision, Thread.currentThread().getId());

        DecisionTimerValue value = map.get(key);
        if (value != null) {
            ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();

            int pause = salaryRandom.nextInt(1000, 3000);
            try {
                Thread.sleep(pause);
                Histogram.Timer timer = value.timer;
                double duration = timer.observeDuration();
                timer.close();
                map.remove(key);
                LOGGER.info(MessageFormat.format("pause: {0}ms - duration = {1}ms", pause, duration));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    final class DecisionTimerKey {

        Decision decision;
        long threadId;

        DecisionTimerKey(Decision decision, long id) {
            this.decision = decision;
            this.threadId = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(decision, threadId);
        }
    }

    final class DecisionTimerValue {

        Histogram.Timer timer;
        Instant instant;

        public DecisionTimerValue(Histogram.Timer timer, Instant instant) {
            this.timer = timer;
            this.instant = instant;
        }
    }

    private String getDecisionName(Decision decision) {
        return decision.getName();
    }
}
