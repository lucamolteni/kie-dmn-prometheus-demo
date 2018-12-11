package org.kie.dmn.prometheus.demo;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import io.prometheus.client.Histogram;
import org.kie.dmn.model.api.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecisionTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionTimer.class);

    private final Histogram histogram = Histogram.build().name("dmn_evaluate_decision_second")
            .help("DMN Evaluation Time")
            .labelNames("decision_name")
            .buckets(1, 2, 3, 4)
            .register();

    private final Duration evictionTime;

    private Map<DecisionTimerKey, DecisionTimerValue> map = new ConcurrentHashMap<>();

    public DecisionTimer(Duration evictionTime) {
        this.evictionTime = evictionTime;
    }

    public void scheduleTimer(Decision decision, long id, Instant creationTs) {

        DecisionTimer.DecisionTimerKey key = new DecisionTimerKey(decision, id);

        map.computeIfAbsent(key, k -> {
            String decisionName = getDecisionName(decision);
            return new DecisionTimerValue(histogram.labels(decisionName).startTimer(), creationTs);
        });
    }

    public void signalTimer(Decision decision, long threadId, Consumer<Double> consumer) {
        DecisionTimerKey key = new DecisionTimerKey(decision, threadId);

        DecisionTimerValue value = map.get(key);
        if (value != null) {
            Histogram.Timer timer = value.timer;
            double duration = timer.observeDuration();
            timer.close();
            consumer.accept(duration);
            map.remove(key);
        }
    }

    public boolean purgeTimers() {
        LOGGER.info("Purging timers");
        LOGGER.info("Map size: " + map.entrySet().size());
        boolean removed = map.entrySet().removeIf(kv -> {
            Instant timerCreationTS = kv.getValue().instant;
            Instant evictionTime = Instant.now().minus(this.evictionTime);

            return timerCreationTS.isBefore(evictionTime);
        });
        LOGGER.info("New map size:" + map.entrySet().size());
        return removed;
    }

    public int getTimerNumbers() {
        return map.size();
    }

    final class DecisionTimerKey {

        Decision decision;
        long threadId;

        DecisionTimerKey(Decision decision, long id) {
            this.decision = decision;
            this.threadId = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DecisionTimerKey that = (DecisionTimerKey) o;
            return threadId == that.threadId &&
                    decision.equals(that.decision);
        }

        @Override
        public int hashCode() {
            return Objects.hash(decision, threadId);
        }

        @Override
        public String toString() {
            return "DecisionTimerKey{" +
                    "decision=" + decision +
                    ", threadId=" + threadId +
                    '}';
        }
    }

    final class DecisionTimerValue {

        Histogram.Timer timer;
        Instant instant;

        public DecisionTimerValue(Histogram.Timer timer, Instant instant) {
            this.timer = timer;
            this.instant = instant;
        }

        @Override
        public String toString() {
            return "DecisionTimerValue{" +
                    "timer=" + timer.hashCode() +
                    ", instant=" + instant +
                    '}';
        }
    }

    private String getDecisionName(Decision decision) {
        return decision.getName();
    }
}
