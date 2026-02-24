package com.example.print.benchmark;

public record BenchmarkResult(
        String engineName,
        long totalTimeNanos,
        long warmupTimeNanos,
        long steadyStateTotalNanos,
        int steadyStateCount,
        long peakHeapBytes,
        long totalOutputBytes,
        int documentCount,
        long[] perDocumentNanos
) {

    public double totalTimeSeconds() {
        return totalTimeNanos / 1_000_000_000.0;
    }

    public double throughputPerSecond() {
        return documentCount / totalTimeSeconds();
    }

    public double avgTimePerDocMillis() {
        return (totalTimeNanos / (double) documentCount) / 1_000_000.0;
    }

    public double warmupTimeMillis() {
        return warmupTimeNanos / 1_000_000.0;
    }

    public double steadyStateThroughput() {
        double seconds = steadyStateTotalNanos / 1_000_000_000.0;
        return steadyStateCount / seconds;
    }

    public double peakHeapMB() {
        return peakHeapBytes / (1024.0 * 1024.0);
    }

    public double totalOutputMB() {
        return totalOutputBytes / (1024.0 * 1024.0);
    }

    public double avgFileSizeKB() {
        return (totalOutputBytes / (double) documentCount) / 1024.0;
    }

    public double percentileMillis(int percentile) {
        long[] sorted = perDocumentNanos.clone();
        java.util.Arrays.sort(sorted);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.length) - 1;
        index = Math.max(0, Math.min(index, sorted.length - 1));
        return sorted[index] / 1_000_000.0;
    }
}
