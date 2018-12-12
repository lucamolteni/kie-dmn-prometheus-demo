package org.kie.dmn.prometheus.demo.solution2;

import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
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
import org.kie.dmn.core.impl.AfterEvaluateDecisionEventImpl;
import org.kie.dmn.core.impl.BeforeEvaluateDecisionEventImpl;
import org.kie.dmn.model.api.Decision;
import org.kie.dmn.prometheus.demo.solution1.PrometheusListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusListener2 implements DMNRuntimeEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusListener.class);

    private final Histogram histogram = Histogram.build().name("dmn_evaluate_decision_second")
            .help("DMN Evaluation Time")
            .labelNames("decision_name")
            .buckets(0.5, 1, 2, 3, 4)
            .register();

    private final Counter evaluateCount = Counter.build().name("dmn_evaluation_prometheus_total")
            .help("DMN Evaluations")
            .labelNames("decision_name").register();

    @Override
    public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent e) {
        String decisionName = getDecisionName(e.getDecision().getDecision());
        Histogram.Timer timer = histogram.labels(decisionName).startTimer();
        BeforeEvaluateDecisionEventImpl event = getBeforeImpl(e);
        event.setMetadata(timer);
    }

    @Override
    public void afterEvaluateDecision(AfterEvaluateDecisionEvent e) {
        BeforeEvaluateDecisionEventImpl event = getBeforeImpl(getAfterImpl(e).getBeforeEvent());
        Histogram.Timer timer = (Histogram.Timer) event.getMetadata();
        ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();
        int pause = salaryRandom.nextInt(1000, 4000);
        try {
            Thread.sleep(pause);
            timer.close();
            LOGGER.info(MessageFormat.format("pause: {0}ms", pause));
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private AfterEvaluateDecisionEventImpl getAfterImpl(AfterEvaluateDecisionEvent e) {
        return (AfterEvaluateDecisionEventImpl) e;
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

    private BeforeEvaluateDecisionEventImpl getBeforeImpl(BeforeEvaluateDecisionEvent e) {
        return (BeforeEvaluateDecisionEventImpl) e;
    }

    private String getDecisionName(Decision decision) {
        return decision.getName();
    }
}