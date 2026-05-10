package com.vmware.vim25.mo.samples.cli;

public final class FakeSampleUtilFailureSample {
    private FakeSampleUtilFailureSample() {}

    public static void main(String[] args) {
        throw new IllegalStateException("Set sample.user or YAVIJAVA_USER before running this sample.");
    }
}
