package com.vmware.vim25.mo.samples.cli;

import java.util.Map;

public record SampleConfig(String url, String user, String password, String locale, String sessionToken) {
    public static SampleConfig from(CliArgs args) {
        Map<String, String> env = System.getenv();
        return new SampleConfig(
                setting(args, "url", "sample.url", "YAVIJAVA_URL", ""),
                setting(args, "user", "sample.user", "YAVIJAVA_USER", ""),
                setting(args, "password", "sample.password", "YAVIJAVA_PASSWORD", ""),
                setting(args, "locale", "sample.locale", "YAVIJAVA_LOCALE", ""),
                setting(args, "session_token", "sample.sessionToken", "YAVIJAVA_SESSION_TOKEN", "", env));
    }

    public void applySystemProperties() {
        setPropertyIfNonBlank("sample.url", url);
        setPropertyIfNonBlank("sample.user", user);
        setPropertyIfNonBlank("sample.password", password);
        setPropertyIfNonBlank("sample.locale", locale);
    }

    public String requireUrl() {
        return requireRealValue("url", url);
    }

    public String requireUser() {
        return requireRealValue("user", user);
    }

    public String requirePassword() {
        return requireRealValue("password", password);
    }

    public String requireSessionToken() {
        return requireRealValue("session token", sessionToken);
    }

    private static String setting(CliArgs args, String optionName, String propertyName, String envName, String defaultValue) {
        return setting(args, optionName, propertyName, envName, defaultValue, System.getenv());
    }

    private static String setting(CliArgs args, String optionName, String propertyName, String envName, String defaultValue, Map<String, String> env) {
        String cliValue = args.option(optionName);
        if (!isBlank(cliValue)) {
            return cliValue.trim();
        }

        String propertyValue = System.getProperty(propertyName);
        if (!isBlank(propertyValue)) {
            return propertyValue.trim();
        }

        String envValue = env.get(envName);
        if (!isBlank(envValue)) {
            return envValue.trim();
        }

        return defaultValue;
    }

    private static void setPropertyIfNonBlank(String name, String value) {
        if (!isBlank(value)) {
            System.setProperty(name, value.trim());
        }
    }

    private static String requireRealValue(String label, String value) {
        if (isBlank(value)) {
            throw new IllegalArgumentException("Set a non-blank " + label + " before running this sample.");
        }
        String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("changeme")) {
            throw new IllegalArgumentException("Set a real " + label + "; 'changeme' is a placeholder.");
        }
        if (trimmed.contains("example.com")) {
            throw new IllegalArgumentException("Set a real " + label + "; example.com is a placeholder host.");
        }
        return trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
