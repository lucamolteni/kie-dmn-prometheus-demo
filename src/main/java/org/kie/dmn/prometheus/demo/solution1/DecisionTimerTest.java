package org.kie.dmn.prometheus.demo.solution1;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.kie.dmn.model.api.Decision;
import org.kie.dmn.model.v1_1.TDecision;

import static org.junit.Assert.*;

public class DecisionTimerTest {

    final Decision stubDecision = new TDecision();

    @Before
    public void init() {
        stubDecision.setName("Name");
    }

    @Test
    public void purgeTimers() throws InterruptedException {

        DecisionTimer decisionTimer = new DecisionTimer(Duration.ofMillis(15));
        decisionTimer.scheduleTimer(stubDecision, 1, Instant.now());
        boolean notRemoved = decisionTimer.purgeTimers();
        assertFalse(notRemoved);
        assertEquals(1, decisionTimer.getTimerNumbers());
        decisionTimer.scheduleTimer(stubDecision, 2, Instant.now());
        TimeUnit.MILLISECONDS.sleep(20);
        boolean removed = decisionTimer.purgeTimers();
        assertTrue(removed);
        assertEquals(0, decisionTimer.getTimerNumbers());
    }
}