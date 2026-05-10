package com.vmware.vim25.mo.samples.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class SamplesCli {
    private final SampleCatalog catalog;
    private final PrintStream out;
    private final PrintStream err;

    SamplesCli(SampleCatalog catalog, PrintStream out) {
        this(catalog, out, System.err);
    }

    SamplesCli(SampleCatalog catalog, PrintStream out, PrintStream err) {
        this.catalog = catalog;
        this.out = out;
        this.err = err;
    }

    public static void main(String[] argv) throws Throwable {
        int exitCode = new SamplesCli(SampleCatalog.loadDefault(), System.out, System.err).run(argv);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] argv) throws Throwable {
        try {
            execute(argv);
            return 0;
        } catch (IllegalArgumentException e) {
            err.println("ERROR: " + e.getMessage());
            return 2;
        } catch (IllegalStateException e) {
            if (!isSampleUtilCredentialError(e)) {
                throw e;
            }
            err.println("ERROR: " + e.getMessage());
            return 2;
        }
    }

    public void execute(String[] argv) throws Throwable {
        CliArgs args = CliArgs.parse(argv);
        switch (args.command()) {
            case "list" -> printList();
            case "help" -> printHelp(args.sampleName());
            case "run" -> runSample(args);
            default -> throw new IllegalArgumentException("Unknown command: " + args.command());
        }
    }

    private void printList() {
        out.printf("%-32s %-16s %s%n", "Alias", "Category", "Arg style");
        for (SampleEntry entry : catalog.entries()) {
            out.printf("%-32s %-16s %s%n", entry.alias(), entry.category(), entry.argStyle());
        }
    }

    private void printHelp(String sampleName) {
        if (sampleName == null || sampleName.isBlank()) {
            printUsage();
            return;
        }

        SampleEntry entry = findEntry(sampleName);
        out.println("Alias: " + entry.alias());
        out.println("Class: " + entry.className());
        out.println("Category: " + entry.category());
        out.println("Argument style: " + entry.argStyle());
        out.println("Use -- before sample-specific arguments, for example: " + entry.alias() + " --url https://vc/sdk -- --sample-arg");
    }

    private void printUsage() {
        out.println("Usage:");
        out.println("  ./gradlew run --args=\"list\"");
        out.println("  ./gradlew run --args=\"help SampleName\"");
        out.println("  ./gradlew run --args=\"SampleName [connection options] -- [sample args]\"");
        out.println();
        out.println("Connection options: --url URL --user USER --password PASS --locale LOCALE --session-token TOKEN");
    }

    private void runSample(CliArgs args) throws Throwable {
        SampleEntry entry = findEntry(args.sampleName());
        SampleConfig config = SampleConfig.from(args);
        String[] sampleArgs = adaptArgs(entry.argStyle(), config, args.sampleArgs());
        invokeMain(entry.className(), sampleArgs);
    }

    private SampleEntry findEntry(String name) {
        return catalog.find(name).orElseThrow(() -> new IllegalArgumentException("Unknown sample: " + name));
    }

    private static String[] adaptArgs(ArgStyle argStyle, SampleConfig config, List<String> sampleArgs) {
        List<String> adapted = new ArrayList<>();
        switch (argStyle) {
            case SYSTEM_PROPERTIES -> config.applySystemProperties();
            case POSITIONAL_CONNECTION -> {
                adapted.add(config.requireUrl());
                adapted.add(config.requireUser());
                adapted.add(config.requirePassword());
            }
            case NAMED_CONNECTION -> {
                adapted.add("--url");
                adapted.add(config.requireUrl());
                adapted.add("--username");
                adapted.add(config.requireUser());
                adapted.add("--password");
                adapted.add(config.requirePassword());
            }
            case SESSION_TOKEN -> {
                adapted.add(config.requireUrl());
                adapted.add(config.requireSessionToken());
            }
        }
        adapted.addAll(sampleArgs);
        return adapted.toArray(String[]::new);
    }

    private static void invokeMain(String className, String[] args) throws Throwable {
        try {
            Class<?> sampleClass = Class.forName(className);
            Method main = sampleClass.getMethod("main", String[].class);
            main.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static boolean isSampleUtilCredentialError(IllegalStateException e) {
        String message = e.getMessage();
        return message != null
                && (message.startsWith("Set ") || message.startsWith("Set a real value for "))
                && message.contains("sample.")
                && message.contains("YAVIJAVA_");
    }
}
