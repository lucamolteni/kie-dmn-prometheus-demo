package org.kie.dmn.prometheus.demo.solution2;

import java.text.MessageFormat;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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

    /**
     * Number of nanoseconds in a second.
     */
    public static final long NANOSECONDS_PER_SECOND = 1_000_000_000;
    public static final long HALF_SECOND_NANO = 500_000_000;

    public static long toNano(long second) {
        return second * NANOSECONDS_PER_SECOND;
    }

    private final Histogram histogram = Histogram.build().name("dmn_evaluate_decision_nanosecond")
            .help("DMN Evaluation Time")
            .labelNames("decision_name")
            .buckets(HALF_SECOND_NANO, toNano(1), toNano(2), toNano(3), toNano(4))
            .register();

    private final Counter evaluateCount = Counter.build().name("dmn_evaluation_prometheus_total")
            .help("DMN Evaluations")
            .labelNames("decision_name").register();

    @Override
    public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent e) {
        long nanoTime = System.nanoTime();
        BeforeEvaluateDecisionEventImpl event = getBeforeImpl(e);
        event.setTimestamp(nanoTime);
    }

    @Override
    public void afterEvaluateDecision(AfterEvaluateDecisionEvent e) {
        BeforeEvaluateDecisionEventImpl event = getBeforeImpl(getAfterImpl(e).getBeforeEvent());
        String decisionName = getDecisionName(e.getDecision().getDecision());
        long startTime = event.getTimestamp();
        ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();
        int pause = salaryRandom.nextInt(400, 4100);
        try {
            TimeUnit.MILLISECONDS.sleep(pause);

            long elapsed = System.nanoTime() - startTime;
            histogram.labels(decisionName)
                    .observe(elapsed);

            LOGGER.info(MessageFormat.format("pause: {0}s elapsed {1}", pause, elapsed));
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