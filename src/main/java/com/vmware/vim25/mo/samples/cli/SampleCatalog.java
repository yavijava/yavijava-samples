package com.vmware.vim25.mo.samples.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class SampleCatalog {
    private static final String CATALOG_RESOURCE = "/sample-catalog.tsv";
    private static final Comparator<SampleEntry> BY_ALIAS = Comparator.comparing(SampleEntry::alias, String.CASE_INSENSITIVE_ORDER);

    private final List<SampleEntry> entries;

    private SampleCatalog(List<SampleEntry> entries) {
        this.entries = entries.stream().sorted(BY_ALIAS).toList();
    }

    public static SampleCatalog loadDefault() throws IOException {
        InputStream input = SampleCatalog.class.getResourceAsStream(CATALOG_RESOURCE);
        if (input == null) {
            throw new IOException("Missing resource " + CATALOG_RESOURCE);
        }
        try (input) {
            return load(input);
        }
    }

    public static SampleCatalog load(InputStream input) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            List<SampleEntry> loaded = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .map(SampleCatalog::parseLine)
                    .toList();
            ensureUnique(loaded);
            return new SampleCatalog(loaded);
        }
    }

    public List<SampleEntry> entries() {
        return entries;
    }

    public Optional<SampleEntry> find(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return entries.stream()
                .filter(entry -> entry.alias().toLowerCase(Locale.ROOT).equals(normalized)
                        || entry.className().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    private static SampleEntry parseLine(String line) {
        String[] columns = line.split("\\t", -1);
        if (columns.length != 4) {
            throw new IllegalArgumentException("Catalog row must contain 4 tab-separated columns: " + line);
        }
        return new SampleEntry(columns[0], columns[1], columns[2], ArgStyle.valueOf(columns[3]));
    }

    private static void ensureUnique(List<SampleEntry> loaded) {
        Set<String> aliases = new HashSet<>();
        Set<String> classNames = new HashSet<>();
        for (SampleEntry entry : loaded) {
            if (!aliases.add(entry.alias().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate sample alias: " + entry.alias());
            }
            if (!classNames.add(entry.className().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Duplicate sample class: " + entry.className());
            }
        }
    }
}
