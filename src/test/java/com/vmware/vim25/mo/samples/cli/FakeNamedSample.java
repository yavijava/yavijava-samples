package com.vmware.vim25.mo.samples.cli;

public final class FakeNamedSample {
    static String[] lastArgs;

    private FakeNamedSample() {}

    public static void main(String[] args) {
        lastArgs = args;
    }
}
