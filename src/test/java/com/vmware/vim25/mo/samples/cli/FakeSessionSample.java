package com.vmware.vim25.mo.samples.cli;

public final class FakeSessionSample {
    static String[] lastArgs;

    private FakeSessionSample() {}

    public static void main(String[] args) {
        lastArgs = args;
    }
}
