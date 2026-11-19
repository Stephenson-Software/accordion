package com.accordion.trace;

import com.accordion.service.UsageReportingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {"false", "FALSE", "off", "no", "0", "", " ", "disabled", "flase", "\"false\""})
    void usageReportingEnabledFailsClosed(String value) {
        // Read through the client's helper, so anything but a clear "on" is off rather than a
        // startup failure (Spring's boolean conversion) or on.
        UsageReportingService service = newService(value);

        assertFalse(service.isEnabled(), "usage-reporting.enabled=" + value + " must switch reporting off");
        service.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "TRUE", "on", "yes", "1", " true "})
    void usageReportingEnabledOnValuesKeepItOn(String value) {
        UsageReportingService service = newService(value);

        assertTrue(service.isEnabled(), "usage-reporting.enabled=" + value + " keeps reporting on");
        service.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "disabled", "0"})
    void traceUsageReportingFailsClosed(String value) {
        environment.put("TRACE_USAGE_REPORTING", value);

        UsageReportingService service = newService();

        assertFalse(service.isEnabled(), "TRACE_USAGE_REPORTING=" + value + " must switch reporting off");
        service.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "yes", "anything"})
    void doNotTrackFailsClosed(String value) {
        environment.put("DO_NOT_TRACK", value);

        UsageReportingService service = newService();

        assertFalse(service.isEnabled(), "DO_NOT_TRACK=" + value + " must switch reporting off");
        service.close();
    }

    private static UsageReportingService newService() {
        return newService("true");
    }

    private static UsageReportingService newService(String enabled) {
        // With a key, pointed at a port nothing listens on: nothing is sent either way. Goes
        // through the constructor Spring uses, so the raw usage-reporting.enabled string is read.
        return new UsageReportingService(enabled, "http://127.0.0.1:9", "test-key", "1.0");
    }
}
