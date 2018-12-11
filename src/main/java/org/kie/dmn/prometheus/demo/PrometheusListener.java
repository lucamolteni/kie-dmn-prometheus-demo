package org.kie.dmn.prometheus.demo;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.kie.dmn.api.core.ast.DecisionNode;
import org.kie.dmn.api.core.event.AfterEvaluateBKMEvent;
import org.kie.dmn.api.core.event.AfterEvaluateContextEntryEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionServiceEvent;
import org.kie.dmn.api.core.event.AfterEvaluateDecisionTableEvent;
import org.kie.dmn.api.core.event.BeforeEvaluateBKMEvent;
import org.kie.dmn.api.core.event.BeforeEvaluateContextEntryEvent;
import org.kie.dmn.api.core.event.BeforeEvaluateDecisionEvent;
import org.kie.dmn.api.core.event.BeforeEvaluateDecisionServiceEvent;
import org.kie.dmn.api.core.event.BeforeEvaluateDecisionTableEvent;
import org.kie.dmn.api.core.event.DMNRuntimeEventListener;
import org.kie.dmn.model.api.Decision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusListener implements DMNRuntimeEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusListener .class);

    private final Counter evaluateCount = Counter.build().name("dmn_evaluation_prometheus_total")
            .help("DMN Evaluations")
            .labelNames("decision_name").register();

    private final Histogram histogram = Histogram.build().name("dmn_evaluate_decision_second")
            .help("DMN Evaluation Time")
            .labelNames("decision_name")
            .register();

    final class MapKey {

        Decision decision;
        long threadId;

        MapKey(Decision decision, long id) {
            this.decision = decision;
            this.threadId = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(decision, threadId);
        }
    }

    private Map<MapKey, Histogram.Timer> map = new ConcurrentHashMap<>();

    @Override
    public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
        MapKey key = new MapKey(event.getDecision().getDecision(), Thread.currentThread().getId());

        map.computeIfAbsent(key, k -> {
            String decisionName = getDecisionName(event.getDecision());
            return histogram.labels(decisionName).startTimer();
        });
    }

    @Override
    public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {

        String decisionName = getDecisionName(event.getDecision());

        evaluateCount.labels(decisionName).inc();
        MapKey key = new MapKey(event.getDecision().getDecision(), Thread.currentThread().getId());

        Histogram.Timer timer = map.get(key);
        if (timer != null) {
            ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();

            int pause = salaryRandom.nextInt(1000, 3000);
            try {
                Thread.sleep(pause);
                double duration = timer.observeDuration();
                timer.close();
                map.remove(key);
                LOGGER.info(MessageFormat.format("pause: {0}ms - duration = {1}ms", pause, duration));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String getDecisionName(DecisionNode decision) {
        return decision.getDecision().getName();
    }

    @Override
    public void beforeEvaluateBKM(BeforeEvaluateBKMEvent event) {
    }

    @Override
    public void afterEvaluateBKM(AfterEvaluateBKMEvent event) {
        evaluateCount.labels("bkn").inc();
    }

    @Override
    public void beforeEvaluateContextEntry(BeforeEvaluateContextEntryEvent event) {
    }

    @Override
    public void afterEvaluateContextEntry(AfterEvaluateContextEntryEvent event) {
        evaluateCount.labels("contextEntry").inc();
    }

    @Override
    public void beforeEvaluateDecisionTable(BeforeEvaluateDecisionTableEvent event) {
    }

    @Override
    public void afterEvaluateDecisionTable(AfterEvaluateDecisionTableEvent event) {
        evaluateCount.labels("decisionTable").inc();
    }

    @Override
    public void beforeEvaluateDecisionService(BeforeEvaluateDecisionServiceEvent event) {
    }

    @Override
    public void afterEvaluateDecisionService(AfterEvaluateDecisionServiceEvent event) {
        evaluateCount.labels("decisionService").inc();
    }
}