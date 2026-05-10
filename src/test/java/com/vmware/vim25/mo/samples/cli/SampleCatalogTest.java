package com.vmware.vim25.mo.samples.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SampleCatalogTest {
    @Test
    void loadsCatalogRowsFromInputStream() throws Exception {
        SampleCatalog catalog = SampleCatalog.load(stream("# comment\n\nHelloVM\tcom.vmware.vim25.mo.samples.HelloVM\troot\tSYSTEM_PROPERTIES\n"));

        assertEquals(List.of(new SampleEntry("HelloVM", "com.vmware.vim25.mo.samples.HelloVM", "root", ArgStyle.SYSTEM_PROPERTIES)), catalog.entries());
    }

    @Test
    void findsSamplesByAliasOrClassNameIgnoringCase() throws Exception {
        SampleCatalog catalog = SampleCatalog.load(stream("HelloVM\tcom.vmware.vim25.mo.samples.HelloVM\troot\tSYSTEM_PROPERTIES\n"));

        assertEquals("HelloVM", catalog.find("hellovm").orElseThrow().alias());
        assertEquals("HelloVM", catalog.find("COM.VMWARE.VIM25.MO.SAMPLES.HELLOVM").orElseThrow().alias());
    }

    @Test
    void returnsEntriesSortedByAliasIgnoringCase() throws Exception {
        SampleCatalog catalog = SampleCatalog.load(stream("zeta\tcom.example.Zeta\tmisc\tPOSITIONAL_CONNECTION\nAlpha\tcom.example.Alpha\tmisc\tNAMED_CONNECTION\n"));

        assertEquals(List.of("Alpha", "zeta"), catalog.entries().stream().map(SampleEntry::alias).toList());
    }

    @Test
    void returnsImmutableEntries() throws Exception {
        SampleCatalog catalog = SampleCatalog.load(stream("HelloVM\tcom.vmware.vim25.mo.samples.HelloVM\troot\tSYSTEM_PROPERTIES\n"));

        assertThrows(UnsupportedOperationException.class, () -> catalog.entries().add(new SampleEntry("Other", "com.example.Other", "misc", ArgStyle.POSITIONAL_CONNECTION)));
    }

    @Test
    void generatedCatalogContainsRepresentativeSamples() throws Exception {
        SampleCatalog catalog = SampleCatalog.loadDefault();

        SampleEntry helloVm = catalog.find("HelloVM").orElseThrow();
        assertEquals("com.vmware.vim25.mo.samples.HelloVM", helloVm.className());
        assertEquals(ArgStyle.SYSTEM_PROPERTIES, helloVm.argStyle());

        SampleEntry smokeTest = catalog.find("Vsphere9SmokeTest").orElseThrow();
        assertEquals("com.vmware.vim25.mo.samples.Vsphere9SmokeTest", smokeTest.className());
        assertNotNull(smokeTest.category());
    }

    private static ByteArrayInputStream stream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
