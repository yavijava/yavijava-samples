package com.vmware.vim25.mo.samples.cli;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SamplesCliTest {
    @Test
    void listPrintsAliasCategoryAndArgStyle() throws Throwable {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = new SamplesCli(catalog(), printStream(output), printStream(new ByteArrayOutputStream())).run(new String[] {"list"});

        assertEquals(0, exitCode);
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("FakeNamed"));
        assertTrue(text.contains("test"));
        assertTrue(text.contains("NAMED_CONNECTION"));
    }

    @Test
    void helpPrintsSampleDetailsAndSeparatorGuidance() throws Throwable {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int exitCode = new SamplesCli(catalog(), printStream(output), printStream(new ByteArrayOutputStream())).run(new String[] {"help", "FakeNamed"});

        assertEquals(0, exitCode);
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Alias: FakeNamed"));
        assertTrue(text.contains("Class: com.vmware.vim25.mo.samples.cli.FakeNamedSample"));
        assertTrue(text.contains("Category: test"));
        assertTrue(text.contains("Arg style: NAMED_CONNECTION"));
        assertTrue(text.contains("--"));
    }

    @Test
    void runAdaptsNamedConnectionArguments() throws Throwable {
        FakeNamedSample.lastArgs = null;

        int exitCode = new SamplesCli(catalog(), printStream(new ByteArrayOutputStream()), printStream(new ByteArrayOutputStream())).run(new String[] {
            "FakeNamed",
            "--url", "https://vc/sdk",
            "--user", "root",
            "--password", "secret",
            "--", "vm-1"
        });

        assertEquals(0, exitCode);
        assertArrayEquals(new String[] {"--url", "https://vc/sdk", "--username", "root", "--password", "secret", "vm-1"},
                FakeNamedSample.lastArgs);
    }

    @Test
    void runAdaptsSessionTokenArguments() throws Throwable {
        FakeSessionSample.lastArgs = null;

        int exitCode = new SamplesCli(catalog(), printStream(new ByteArrayOutputStream()), printStream(new ByteArrayOutputStream())).run(new String[] {
            "FakeSession",
            "--url", "https://vc/sdk",
            "--session-token", "token-1",
            "--", "extra"
        });

        assertEquals(0, exitCode);
        assertArrayEquals(new String[] {"https://vc/sdk", "token-1", "extra"}, FakeSessionSample.lastArgs);
    }

    @Test
    void noArgHelpReturnsZero() throws Throwable {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = new SamplesCli(catalog(), printStream(output), printStream(error)).run(new String[] {});

        assertEquals(0, exitCode);
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Usage:"));
        assertEquals("", error.toString(StandardCharsets.UTF_8));
    }

    @Test
    void unknownSampleReturnsNonzeroAndPrintsConciseError() throws Throwable {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = new SamplesCli(catalog(), printStream(new ByteArrayOutputStream()), printStream(error)).run(new String[] {"NoSuchSample"});

        String text = error.toString(StandardCharsets.UTF_8);
        assertEquals(2, exitCode);
        assertTrue(text.contains("ERROR: Unknown sample: NoSuchSample"));
        assertFalse(text.contains("Exception"));
        assertFalse(text.contains("at com.vmware"));
    }

    @Test
    void missingCredentialsReturnNonzeroAndPrintConciseError() throws Throwable {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = new SamplesCli(catalog(), printStream(new ByteArrayOutputStream()), printStream(error)).run(new String[] {
            "FakePositional",
            "--url", "https://vc/sdk"
        });

        String text = error.toString(StandardCharsets.UTF_8);
        assertEquals(2, exitCode);
        assertTrue(text.contains("ERROR:"));
        assertTrue(text.contains("user"));
        assertFalse(text.contains("Exception"));
        assertFalse(text.contains("at com.vmware"));
    }

    @Test
    void sampleUtilStyleMissingCredentialsReturnNonzeroAndPrintConciseError() throws Throwable {
        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int exitCode = new SamplesCli(catalog(), printStream(new ByteArrayOutputStream()), printStream(error)).run(new String[] {
            "FakeSampleUtilFailure",
            "--url", "https://vc/sdk"
        });

        String text = error.toString(StandardCharsets.UTF_8);
        assertEquals(2, exitCode);
        assertTrue(text.contains("ERROR: Set sample.user or YAVIJAVA_USER before running this sample."));
        assertFalse(text.contains("Exception"));
        assertFalse(text.contains("at com.vmware"));
    }

    private static SampleCatalog catalog() throws Exception {
        String rows = "FakeNamed\tcom.vmware.vim25.mo.samples.cli.FakeNamedSample\ttest\tNAMED_CONNECTION\n"
                + "FakePositional\tcom.vmware.vim25.mo.samples.cli.FakePositionalSample\ttest\tPOSITIONAL_CONNECTION\n"
                + "FakeSampleUtilFailure\tcom.vmware.vim25.mo.samples.cli.FakeSampleUtilFailureSample\ttest\tSYSTEM_PROPERTIES\n"
                + "FakeSession\tcom.vmware.vim25.mo.samples.cli.FakeSessionSample\ttest\tSESSION_TOKEN\n";
        return SampleCatalog.load(new ByteArrayInputStream(rows.getBytes(StandardCharsets.UTF_8)));
    }

    private static PrintStream printStream(ByteArrayOutputStream output) {
        return new PrintStream(output, true, StandardCharsets.UTF_8);
    }
}
