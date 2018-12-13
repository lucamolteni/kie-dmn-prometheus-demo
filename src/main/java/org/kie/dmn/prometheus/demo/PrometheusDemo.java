package org.kie.dmn.prometheus.demo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import io.prometheus.client.exporter.HTTPServer;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.dmn.api.core.DMNContext;
import org.kie.dmn.api.core.DMNModel;
import org.kie.dmn.api.core.DMNResult;
import org.kie.dmn.api.core.DMNRuntime;
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

    public static void main(String[] args) throws Exception {

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



        final int parallelism = 4;
        final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        final CyclicBarrier started = new CyclicBarrier(parallelism);
        final Callable<Long> task = () -> {
            started.await();
            final Thread current = Thread.currentThread();
            long executions = 0;
            while (!current.isInterrupted()) {
                evaluateDMNWithPause(dmnRuntime, dmnModel);
                executions++;
            }
            return executions;
        };
        final ArrayList<Future<Long>> tasks = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; i++) {
            tasks.add(executor.submit(task));
        }
        executor.shutdown();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tasks.forEach(future -> future.cancel(true));
        }));
    }

    private static void evaluateDMNWithPause(DMNRuntime dmnRuntime, DMNModel dmnModel) {
        ThreadLocalRandom salaryRandom = ThreadLocalRandom.current();

        int mSalary = salaryRandom.nextInt(1000, 100000 / 12);

        DMNContext context = dmnRuntime.newContext();
        context.set("Monthly Salary", mSalary);

        DMNResult dmnResult = dmnRuntime.evaluateAll(dmnModel, context);

        LOGGER.info("Evaluated rule: monthly {} -> yearly {} ", mSalary, dmnResult.getContext().get("Yearly Salary"));
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
}
