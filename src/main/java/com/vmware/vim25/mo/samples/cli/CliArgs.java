package com.vmware.vim25.mo.samples.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CliArgs {
    private static final String HELP_COMMAND = "help";
    private static final String LIST_COMMAND = "list";
    private static final String RUN_COMMAND = "run";

    private final String command;
    private final String sampleName;
    private final List<String> sampleArgs;
    private final Map<String, String> options;

    private CliArgs(String command, String sampleName, List<String> sampleArgs, Map<String, String> options) {
        this.command = command;
        this.sampleName = sampleName;
        this.sampleArgs = List.copyOf(sampleArgs);
        this.options = Map.copyOf(options);
    }

    public static CliArgs parse(String[] argv) {
        if (argv.length == 0) {
            return new CliArgs(HELP_COMMAND, "", List.of(), Map.of());
        }

        if (LIST_COMMAND.equals(argv[0])) {
            if (argv.length > 1) {
                throw new IllegalArgumentException("list does not accept arguments");
            }
            return new CliArgs(LIST_COMMAND, null, List.of(), Map.of());
        }

        if (HELP_COMMAND.equals(argv[0])) {
            if (argv.length > 2) {
                throw new IllegalArgumentException("help accepts at most one sample name");
            }
            String sampleName = argv.length > 1 ? argv[1] : null;
            return new CliArgs(HELP_COMMAND, sampleName, List.of(), Map.of());
        }

        String sampleName = argv[0];
        Map<String, String> options = new LinkedHashMap<>();
        List<String> sampleArgs = new ArrayList<>();
        boolean readingSampleArgs = false;

        for (int i = 1; i < argv.length; i++) {
            String arg = argv[i];
            if (readingSampleArgs) {
                sampleArgs.add(arg);
            } else if ("--".equals(arg)) {
                readingSampleArgs = true;
            } else if (arg.startsWith("--")) {
                if (arg.startsWith("---")) {
                    throw new IllegalArgumentException("Invalid option " + arg);
                }
                if (i + 1 >= argv.length || argv[i + 1].startsWith("--")) {
                    throw new IllegalArgumentException("Missing value for option " + arg);
                }
                String value = argv[++i];
                options.put(normalizeOptionName(arg), value);
            } else if (arg.startsWith("-")) {
                throw new IllegalArgumentException("Invalid option " + arg);
            } else {
                sampleArgs.add(arg);
            }
        }

        return new CliArgs(RUN_COMMAND, sampleName, sampleArgs, options);
    }

    public String command() {
        return command;
    }

    public String sampleName() {
        return sampleName;
    }

    public List<String> sampleArgs() {
        return Collections.unmodifiableList(sampleArgs);
    }

    public boolean requiresSample() {
        return RUN_COMMAND.equals(command);
    }

    public String option(String name) {
        return options.getOrDefault(normalizeOptionName(name), "");
    }

    private static String normalizeOptionName(String name) {
        return name.replaceFirst("^--", "").replace('-', '_');
    }
}
