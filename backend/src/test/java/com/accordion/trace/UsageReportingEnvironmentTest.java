package com.accordion.trace;

import com.accordion.service.UsageReportingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The backend decides {@code usage-reporting.enabled} itself, but the environment variables
 * every trace client honours must still win over it. Lives in the client's package so it can
 * point the client's environment seam at a map instead of the real environment.
 */
class UsageReportingEnvironmentTest {

    private final Map<String, String> environment = new HashMap<>();
    private Function<String, String> realEnvironment;

    @BeforeEach
    void isolateEnvironment() {
        realEnvironment = TraceClient.environment;
        TraceClient.environment = environment::get;
    }

    @AfterEach
    void restoreEnvironment() {
        TraceClient.environment = realEnvironment;
    }

    @Test
    void doNotTrackDisablesEvenWhenTheBackendSaysEnabled() {
        environment.put("DO_NOT_TRACK", "1");

        UsageReportingService service = newService();

        assertFalse(service.isEnabled(), "DO_NOT_TRACK=1 must switch reporting off");
        service.close();
    }

    @Test
    void traceUsageReportingOffDisablesEvenWhenTheBackendSaysEnabled() {
        environment.put("TRACE_USAGE_REPORTING", "off");

        UsageReportingService service = newService();

        assertFalse(service.isEnabled(), "TRACE_USAGE_REPORTING=off must switch reporting off");
        service.close();
    }

    @Test
    void withoutEitherVariableTheBackendSettingStands() {
        UsageReportingService service = newService();

        assertTrue(service.isEnabled());
        service.close();
    }

    private static UsageReportingService newService() {
        // Enabled, with a key, pointed at a port nothing listens on: nothing is sent either way.
        return new UsageReportingService(true, "http://127.0.0.1:9", "test-key", "1.0");
    }
}
