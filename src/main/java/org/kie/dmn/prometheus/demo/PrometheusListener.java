package org.kie.dmn.prometheus.demo;

import io.prometheus.client.Counter;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusListener implements DMNRuntimeEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusListener.class);

    private DecisionTimer decisionTimer = new DecisionTimer();

    private final Counter evaluateCount = Counter.build().name("dmn_evaluation_prometheus_total")
            .help("DMN Evaluations")
            .labelNames("decision_name").register();

    @Override
    public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
        decisionTimer.scheduleTimer(event.getDecision().getDecision(), Thread.currentThread().getId());
    }

    @Override
    public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
        decisionTimer.registerTimer(event.getDecision().getDecision(), Thread.currentThread().getId());
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