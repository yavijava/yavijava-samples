package com.vmware.vim25.mo.samples.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SampleConfigTest {
    @AfterEach
    void clearProperties() {
        System.clearProperty("sample.url");
        System.clearProperty("sample.user");
        System.clearProperty("sample.password");
        System.clearProperty("sample.locale");
        System.clearProperty("sample.sessionToken");
    }

    @Test
    void resolvesCliOptionsBeforeSystemPropertiesAndDefaultsLocale() {
        System.setProperty("sample.url", "https://property/sdk");
        System.setProperty("sample.user", "property-user");
        System.setProperty("sample.password", "property-password");
        System.setProperty("sample.sessionToken", "property-token");

        SampleConfig config = SampleConfig.from(CliArgs.parse(new String[] {
            "HelloVM",
            "--url", "https://cli/sdk",
            "--user", "cli-user",
            "--password", "cli-password",
            "--session-token", "cli-token"
        }));

        assertEquals("https://cli/sdk", config.url());
        assertEquals("cli-user", config.user());
        assertEquals("cli-password", config.password());
        assertEquals("en-US", config.locale());
        assertEquals("cli-token", config.sessionToken());
    }

    @Test
    void resolvesSystemPropertiesWhenCliOptionsAreBlank() {
        System.setProperty("sample.url", "https://property/sdk");
        System.setProperty("sample.user", "property-user");
        System.setProperty("sample.password", "property-password");
        System.setProperty("sample.locale", "fr-FR");
        System.setProperty("sample.sessionToken", "property-token");

        SampleConfig config = SampleConfig.from(CliArgs.parse(new String[] {"HelloVM"}));

        assertEquals("https://property/sdk", config.url());
        assertEquals("property-user", config.user());
        assertEquals("property-password", config.password());
        assertEquals("fr-FR", config.locale());
        assertEquals("property-token", config.sessionToken());
    }

    @Test
    void appliesNonBlankLegacySampleUtilSystemProperties() {
        SampleConfig config = new SampleConfig("https://vc/sdk", "root", "secret", "en-GB", "");

        config.applySystemProperties();

        assertEquals("https://vc/sdk", System.getProperty("sample.url"));
        assertEquals("root", System.getProperty("sample.user"));
        assertEquals("secret", System.getProperty("sample.password"));
        assertEquals("en-GB", System.getProperty("sample.locale"));
    }

    @Test
    void requireMethodsRejectBlankPlaceholderAndExampleValues() {
        IllegalArgumentException blank = assertThrows(IllegalArgumentException.class,
                () -> new SampleConfig(" ", "user", "password", "en-US", "token").requireUrl());
        IllegalArgumentException example = assertThrows(IllegalArgumentException.class,
                () -> new SampleConfig("https://example.com/sdk", "user", "password", "en-US", "token").requireUrl());
        IllegalArgumentException changeme = assertThrows(IllegalArgumentException.class,
                () -> new SampleConfig("https://vc/sdk", "changeme", "password", "en-US", "token").requireUser());
        IllegalArgumentException token = assertThrows(IllegalArgumentException.class,
                () -> new SampleConfig("https://vc/sdk", "user", "password", "en-US", "changeme").requireSessionToken());

        assertTrue(blank.getMessage().contains("url"));
        assertTrue(example.getMessage().contains("example.com"));
        assertTrue(changeme.getMessage().contains("user"));
        assertTrue(token.getMessage().contains("session token"));
    }
}
