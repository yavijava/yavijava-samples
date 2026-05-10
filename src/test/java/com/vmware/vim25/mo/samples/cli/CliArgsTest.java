package com.vmware.vim25.mo.samples.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CliArgsTest {
    @Test
    void parsesSampleRunWithConnectionOptions() {
        CliArgs args = CliArgs.parse(new String[] {"HelloVM", "--url", "https://vc/sdk", "--user", "root", "--password", "secret"});

        assertEquals("run", args.command());
        assertEquals("HelloVM", args.sampleName());
        assertEquals("https://vc/sdk", args.option("url"));
        assertEquals("root", args.option("user"));
        assertEquals("secret", args.option("password"));
        assertEquals("", args.option("missing"));
        assertEquals(List.of(), args.sampleArgs());
        assertTrue(args.requiresSample());
    }

    @Test
    void parsesEmptyArgumentsAsGeneralHelp() {
        CliArgs args = CliArgs.parse(new String[] {});

        assertEquals("help", args.command());
        assertEquals("", args.sampleName());
        assertEquals(List.of(), args.sampleArgs());
        assertFalse(args.requiresSample());
    }

    @Test
    void preservesSampleArgumentsAfterSeparator() {
        CliArgs args = CliArgs.parse(new String[] {"CloneVM", "--url", "https://vc/sdk", "--", "source-vm", "clone-vm"});

        assertEquals("run", args.command());
        assertEquals("CloneVM", args.sampleName());
        assertEquals("https://vc/sdk", args.option("url"));
        assertEquals(List.of("source-vm", "clone-vm"), args.sampleArgs());
        assertTrue(args.requiresSample());
    }

    @Test
    void preservesOptionLikeSampleArgumentsAfterSeparator() {
        CliArgs args = CliArgs.parse(new String[] {"SomeSample", "--", "--guest-flag", "value"});

        assertEquals("run", args.command());
        assertEquals("SomeSample", args.sampleName());
        assertEquals(List.of("--guest-flag", "value"), args.sampleArgs());
    }

    @Test
    void preservesMalformedFlagLikeSampleArgumentsAfterSeparator() {
        CliArgs args = CliArgs.parse(new String[] {"SomeSample", "--", "-url", "---url"});

        assertEquals("run", args.command());
        assertEquals("SomeSample", args.sampleName());
        assertEquals(List.of("-url", "---url"), args.sampleArgs());
    }

    @Test
    void rejectsTrailingOptionWithoutValue() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[] {"HelloVM", "--url"}));

        assertTrue(error.getMessage().contains("--url"));
    }

    @Test
    void rejectsOptionValueThatLooksLikeAnotherOption() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> CliArgs.parse(new String[] {"HelloVM", "--url", "--user", "root"}));

        assertTrue(error.getMessage().contains("--url"));
    }

    @Test
    void rejectsSingleDashFlagBeforeSeparator() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[] {"HelloVM", "-url", "value"}));

        assertTrue(error.getMessage().contains("-url"));
    }

    @Test
    void rejectsTripleDashFlagBeforeSeparator() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[] {"HelloVM", "---url", "value"}));

        assertTrue(error.getMessage().contains("---url"));
    }

    @Test
    void parsesListCommandWithoutSample() {
        CliArgs args = CliArgs.parse(new String[] {"list"});

        assertEquals("list", args.command());
        assertEquals(null, args.sampleName());
        assertEquals(List.of(), args.sampleArgs());
        assertFalse(args.requiresSample());
    }

    @Test
    void rejectsListCommandWithExtraArgument() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[] {"list", "extra"}));
    }

    @Test
    void parsesHelpCommandForSample() {
        CliArgs args = CliArgs.parse(new String[] {"help", "HelloVM"});

        assertEquals("help", args.command());
        assertEquals("HelloVM", args.sampleName());
        assertEquals(List.of(), args.sampleArgs());
        assertFalse(args.requiresSample());
    }

    @Test
    void rejectsHelpCommandWithExtraArgument() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[] {"help", "HelloVM", "extra"}));
    }
}
