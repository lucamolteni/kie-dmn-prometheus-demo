package org.kie.dmn.prometheus.demo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
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
import org.kie.dmn.core.compiler.RuntimeTypeCheckOption;
import org.kie.dmn.core.impl.DMNRuntimeImpl;
import org.kie.dmn.core.util.KieHelper;
import org.kie.dmn.prometheus.demo.solution1.DecisionTimer;
import org.kie.dmn.prometheus.demo.solution1.PrometheusListener;
import org.kie.dmn.prometheus.demo.solution2.PrometheusListener2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrometheusDemo {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusDemo.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();

        KieServices kieServices = KieServices.Factory.get();
        KieContainer kieContainer = KieHelper.getKieContainer(
                kieServices.newReleaseId("org.kie", "kie-dmn-prometheus-demo-" + UUID.randomUUID(), "1.0"),
                kieServices.getResources().newClassPathResource("simple-item-def.dmn", PrometheusDemo.class)
        );

        DMNRuntime dmnRuntime = kieContainer.newKieSession().getKieRuntime(DMNRuntime.class);
        ((DMNRuntimeImpl) dmnRuntime).setOption(new RuntimeTypeCheckOption(true));

//        DMNRuntimeEventListener listener = solution1();
        DMNRuntimeEventListener listener = solution2();



        dmnRuntime.addListener(listener);


        DMNModel dmnModel = dmnRuntime.getModel("https://github.com/kiegroup/kie-dmn/itemdef", "simple-item-def");

        // Prometheus endpoint
        new HTTPServer(Integer.valueOf(System.getProperty("dmn.prometheus.port", "19090")));
        LOGGER.info("Prometheus endpoint on port {}", System.getProperty("dmn.prometheus.port", "19090"));

        // Prometheus endpoint under micrometer
//        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
//        dmnRuntime.addListener(new MicrometerListener(prometheusRegistry));
//
//        InetSocketAddress micrometerAddress = new InetSocketAddress(Integer.valueOf(System.getProperty("dmn.micrometer.port", "29090")));
//        HttpServer micrometerServer = HttpServer.create(micrometerAddress, 0);
//        micrometerServer.createContext("/metrics", httpExchange -> {
//            String response = prometheusRegistry.scrape();
//            httpExchange.sendResponseHeaders(200, response.getBytes().length);
//            try (OutputStream os = httpExchange.getResponseBody()) {
//                os.write(response.getBytes());
//            }
//        });
//        new Thread(micrometerServer::start).start();
//        LOGGER.info("Micrometer endpoint on port {}", System.getProperty("dmn.micrometer.port", "29090"));

        while (true) {
            int mSalary = salaryRandom.nextInt(1000, 100000 / 12);
            int pause = salaryRandom.nextInt(100, 150);

            DMNContext context = dmnRuntime.newContext();
            context.set("Monthly Salary", mSalary);

            DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, context);

            LOGGER.info("Evaluated rule: monthly {} -> yearly {} ... next pause: {}ms", mSalary, dmnResult.getContext().get("Yearly Salary"), pause);

            Thread.sleep(pause);
        }
    }

    private static DMNRuntimeEventListener solution1() {
        Duration evictionTime = Duration.ofSeconds(13);
        DecisionTimer decisionTimer = new DecisionTimer(evictionTime);
        PrometheusListener listener = new PrometheusListener(decisionTimer);
        // Schedule eviction
        Executors.newScheduledThreadPool(1).schedule(decisionTimer::purgeTimers, evictionTime.toMillis(), TimeUnit.MILLISECONDS);
        return listener;
    }

    private static DMNRuntimeEventListener solution2() {
        return new PrometheusListener2();
    }

    // --- //

    public static class MicrometerListener implements DMNRuntimeEventListener {

        private final io.micrometer.core.instrument.Counter evaluateDecisionCount, evaluateBkmCount, evaluateContextEntryCount, evaluateDecisionTableCount, evaluateDecisionServiceCount;

        private MicrometerListener(MeterRegistry meterRegistry) {
            evaluateDecisionCount = meterRegistry.counter("dmn_evaluation_micrometer", "type", "decision");
            evaluateBkmCount = meterRegistry.counter("dmn_evaluation_micrometer", "type", "bkm");
            evaluateContextEntryCount = meterRegistry.counter("dmn_evaluation_micrometer", "type", "contextEntry");
            evaluateDecisionTableCount = meterRegistry.counter("dmn_evaluation_micrometer", "type", "decisionTable");
            evaluateDecisionServiceCount = meterRegistry.counter("dmn_evaluation_micrometer", "type", "decisionService");
        }

        @Override public void beforeEvaluateDecision(BeforeEvaluateDecisionEvent event) {
        }

        @Override public void afterEvaluateDecision(AfterEvaluateDecisionEvent event) {
            evaluateDecisionCount.increment();
        }

        @Override public void beforeEvaluateBKM(BeforeEvaluateBKMEvent event) {
        }

        @Override public void afterEvaluateBKM(AfterEvaluateBKMEvent event) {
            evaluateBkmCount.increment();
        }

        @Override public void beforeEvaluateContextEntry(BeforeEvaluateContextEntryEvent event) {
        }

        @Override public void afterEvaluateContextEntry(AfterEvaluateContextEntryEvent event) {
            evaluateContextEntryCount.increment();
        }

        @Override public void beforeEvaluateDecisionTable(BeforeEvaluateDecisionTableEvent event) {
        }

        @Override public void afterEvaluateDecisionTable(AfterEvaluateDecisionTableEvent event) {
            evaluateDecisionTableCount.increment();
        }

        @Override public void beforeEvaluateDecisionService(BeforeEvaluateDecisionServiceEvent event) {
        }

        @Override public void afterEvaluateDecisionService(AfterEvaluateDecisionServiceEvent event) {
            evaluateDecisionServiceCount.increment();
        }
    }

}
