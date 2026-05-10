package com.vmware.vim25.mo.samples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SampleUtilTest {
    @AfterEach
    void clearSettings() {
        System.clearProperty("sample.url");
        System.clearProperty("sample.missing");
    }

    @Test
    void requireSettingReadsSystemProperty() {
        System.setProperty("sample.url", "https://vc.example.test/sdk");

        assertEquals("https://vc.example.test/sdk", SampleUtil.requireSetting("sample.url", "YAVIJAVA_URL", Map.of()));
    }

    @Test
    void requireSettingFallsBackToEnvironment() {
        assertEquals(
                "https://vc.test/sdk",
                SampleUtil.requireSetting("sample.url", "YAVIJAVA_URL", Map.of("YAVIJAVA_URL", "https://vc.test/sdk")));
    }

    @Test
    void missingSettingMessageContainsPropertyAndEnvironmentNames() {
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> SampleUtil.requireSetting("sample.missing", "YAVIJAVA_TEST_MISSING", Map.of()));

        assertTrue(error.getMessage().contains("sample.missing"));
        assertTrue(error.getMessage().contains("YAVIJAVA_TEST_MISSING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "changeme", "https://example.com/sdk"})
    void placeholderSettingsAreRejected(String value) {
        System.setProperty("sample.url", value);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> SampleUtil.requireSetting("sample.url", "YAVIJAVA_URL", Map.of()));

        assertTrue(error.getMessage().contains("sample.url"));
        assertTrue(error.getMessage().contains("YAVIJAVA_URL"));
    }
}
