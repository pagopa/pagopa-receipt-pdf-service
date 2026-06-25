package it.gov.pagopa.receipt.pdf.service.utils;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight performance tracer that integrates Apache Commons {@link StopWatch}
 * with SLF4J MDC, so that each measured step becomes a structured log line
 * (JSON fields when {@code quarkus-logging-json} is on the classpath).
 *
 * <p>Emitted MDC fields (all prefixed with {@code perf.} to be easily filtered on ELK):
 * <ul>
 *     <li>{@code perf.step} - logical name of the measured block</li>
 *     <li>{@code perf.elapsedMs} - elapsed time in milliseconds</li>
 *     <li>{@code perf.<tag>} - any custom tag added via {@link #tag(String, Object)}</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try (PerfTracer t = PerfTracer.start(logger, "cosmos.getReceiptDocument")) {
 *     receipt = cosmosClient.getReceiptDocument(thirdPartyId);
 *     t.tag("found", receipt != null);
 * }
 * }</pre>
 *
 * Example ELK query: {@code mdc.perf.step:"cosmos.getReceiptDocument" AND mdc.perf.elapsedMs:>500}
 */
public final class PerfTracer implements AutoCloseable {

    public static final String MDC_PREFIX = "perf.";
    public static final String MDC_STEP = MDC_PREFIX + "step";
    public static final String MDC_ELAPSED_MS = MDC_PREFIX + "elapsedMs";
    private static final String PERF_LOG_MESSAGE = "perf";

    private final Logger logger;
    private final String step;
    private final StopWatch stopWatch;
    private final List<String> tagKeys = new ArrayList<>();

    private PerfTracer(Logger logger, String step) {
        this.logger = logger;
        this.step = step;
        this.stopWatch = StopWatch.createStarted();
    }

    public static PerfTracer start(Logger logger, String step) {
        return new PerfTracer(logger, step);
    }

    /**
     * Adds an additional MDC field, prefixed with {@code perf.}, available on the final
     * log line emitted by {@link #close()}.
     */
    public PerfTracer tag(String key, Object value) {
        String mdcKey = MDC_PREFIX + key;
        MDC.put(mdcKey, value == null ? "null" : String.valueOf(value));
        tagKeys.add(mdcKey);
        return this;
    }

    public long elapsedMs() {
        return stopWatch.getTime();
    }

    @Override
    public void close() {
        if (!stopWatch.isStopped()) {
            stopWatch.stop();
        }
        MDC.put(MDC_STEP, step);
        MDC.put(MDC_ELAPSED_MS, String.valueOf(stopWatch.getTime()));
        try {
            logger.info(PERF_LOG_MESSAGE);
        } finally {
            MDC.remove(MDC_STEP);
            MDC.remove(MDC_ELAPSED_MS);
            for (String k : tagKeys) {
                MDC.remove(k);
            }
            tagKeys.clear();
        }
    }
}

