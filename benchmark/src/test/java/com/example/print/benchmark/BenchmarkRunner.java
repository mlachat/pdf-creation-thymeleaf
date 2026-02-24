package com.example.print.benchmark;

import com.example.print.itext.ItextPdfGenerator;
import com.example.print.jasper.JasperPdfGenerator;
import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BenchmarkRunner {

    private static final int DOCUMENT_COUNT = 10_000;
    private static final int WARMUP_COUNT = 100;

    @Test
    void runBenchmark() throws Exception {
        System.out.println("\n=== PDF Generation Benchmark: " + DOCUMENT_COUNT + " Documents ===\n");
        System.out.println("Estimated disk space: ~200 MB per engine\n");

        // Generate shared addresses
        AddressGenerator addressGen = new AddressGenerator(42);
        List<Map<String, Object>> addresses = new ArrayList<>(DOCUMENT_COUNT);
        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            addresses.add(addressGen.generateAddress());
        }

        // Load QR image once
        String qrBase64;
        String qrDataUri;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("images/qr-sample.png")) {
            if (is == null) {
                throw new RuntimeException("qr-sample.png not found on classpath");
            }
            byte[] qrBytes = is.readAllBytes();
            qrBase64 = Base64.getEncoder().encodeToString(qrBytes);
            qrDataUri = "data:image/png;base64," + qrBase64;
        }

        // Run FreeMarker + OpenHTMLtoPDF benchmark
        BenchmarkResult freemarkerResult = runFreemarkerBenchmark(addresses, qrDataUri);

        // Force GC and pause between benchmarks
        System.gc();
        Thread.sleep(2000);

        // Run JasperReports benchmark
        BenchmarkResult jasperResult = runJasperBenchmark(addresses, qrBase64);

        System.gc();
        Thread.sleep(2000);

        // Run iText pdfHTML benchmark
        BenchmarkResult itextResult = runItextBenchmark(addresses, qrDataUri);

        // Print comparison table
        printComparisonTable(freemarkerResult, jasperResult, itextResult);
    }

    private BenchmarkResult runFreemarkerBenchmark(List<Map<String, Object>> addresses, String qrDataUri) {
        System.out.println("Running FreeMarker + OpenHTMLtoPDF benchmark...");

        FreemarkerRenderer renderer = new FreemarkerRenderer();
        PdfGenerator pdfGenerator = new PdfGenerator();
        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();

        long[] perDocNanos = new long[DOCUMENT_COUNT];
        long totalOutputBytes = 0;
        long warmupTimeNanos = 0;

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long totalStart = System.nanoTime();

        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            Map<String, Object> model = buildFreemarkerModel(addresses.get(i), qrDataUri);

            long docStart = System.nanoTime();
            String html = renderer.render("template-a", model);
            byte[] pdf = pdfGenerator.generatePdf(html, baseUri);
            long docEnd = System.nanoTime();

            perDocNanos[i] = docEnd - docStart;
            totalOutputBytes += pdf.length;

            if (i == 0) {
                warmupTimeNanos = perDocNanos[0];
            }

            if ((i + 1) % 1000 == 0) {
                System.out.println("  FreeMarker: " + (i + 1) + "/" + DOCUMENT_COUNT + " done");
            }
        }

        long totalEnd = System.nanoTime();

        System.gc();
        long peakHeap = memoryBean.getHeapMemoryUsage().getUsed();

        long steadyStateTotal = 0;
        for (int i = WARMUP_COUNT; i < DOCUMENT_COUNT; i++) {
            steadyStateTotal += perDocNanos[i];
        }

        System.out.println("  FreeMarker benchmark complete.\n");

        return new BenchmarkResult(
                "FreeMarker+OHTPDF",
                totalEnd - totalStart,
                warmupTimeNanos,
                steadyStateTotal,
                DOCUMENT_COUNT - WARMUP_COUNT,
                peakHeap,
                totalOutputBytes,
                DOCUMENT_COUNT,
                perDocNanos
        );
    }

    private BenchmarkResult runJasperBenchmark(List<Map<String, Object>> addresses, String qrBase64) {
        System.out.println("Running JasperReports benchmark...");

        JasperPdfGenerator jasperGen = new JasperPdfGenerator();

        long[] perDocNanos = new long[DOCUMENT_COUNT];
        long totalOutputBytes = 0;
        long warmupTimeNanos = 0;

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long totalStart = System.nanoTime();

        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            Map<String, Object> params = buildJasperParams(addresses.get(i), qrBase64);

            long docStart = System.nanoTime();
            byte[] pdf = jasperGen.generatePdf(params);
            long docEnd = System.nanoTime();

            perDocNanos[i] = docEnd - docStart;
            totalOutputBytes += pdf.length;

            if (i == 0) {
                warmupTimeNanos = perDocNanos[0];
            }

            if ((i + 1) % 1000 == 0) {
                System.out.println("  JasperReports: " + (i + 1) + "/" + DOCUMENT_COUNT + " done");
            }
        }

        long totalEnd = System.nanoTime();

        System.gc();
        long peakHeap = memoryBean.getHeapMemoryUsage().getUsed();

        long steadyStateTotal = 0;
        for (int i = WARMUP_COUNT; i < DOCUMENT_COUNT; i++) {
            steadyStateTotal += perDocNanos[i];
        }

        System.out.println("  JasperReports benchmark complete.\n");

        return new BenchmarkResult(
                "JasperReports",
                totalEnd - totalStart,
                warmupTimeNanos,
                steadyStateTotal,
                DOCUMENT_COUNT - WARMUP_COUNT,
                peakHeap,
                totalOutputBytes,
                DOCUMENT_COUNT,
                perDocNanos
        );
    }

    private BenchmarkResult runItextBenchmark(List<Map<String, Object>> addresses, String qrDataUri) {
        System.out.println("Running iText pdfHTML benchmark...");

        FreemarkerRenderer renderer = new FreemarkerRenderer();
        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();
        ItextPdfGenerator itextGen = new ItextPdfGenerator(baseUri);

        long[] perDocNanos = new long[DOCUMENT_COUNT];
        long totalOutputBytes = 0;
        long warmupTimeNanos = 0;

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

        long totalStart = System.nanoTime();

        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            Map<String, Object> model = buildFreemarkerModel(addresses.get(i), qrDataUri);

            long docStart = System.nanoTime();
            String html = renderer.render("template-a", model);
            byte[] pdf = itextGen.generatePdf(html);
            long docEnd = System.nanoTime();

            perDocNanos[i] = docEnd - docStart;
            totalOutputBytes += pdf.length;

            if (i == 0) {
                warmupTimeNanos = perDocNanos[0];
            }

            if ((i + 1) % 1000 == 0) {
                System.out.println("  iText: " + (i + 1) + "/" + DOCUMENT_COUNT + " done");
            }
        }

        long totalEnd = System.nanoTime();

        System.gc();
        long peakHeap = memoryBean.getHeapMemoryUsage().getUsed();

        long steadyStateTotal = 0;
        for (int i = WARMUP_COUNT; i < DOCUMENT_COUNT; i++) {
            steadyStateTotal += perDocNanos[i];
        }

        System.out.println("  iText benchmark complete.\n");

        return new BenchmarkResult(
                "iText pdfHTML",
                totalEnd - totalStart,
                warmupTimeNanos,
                steadyStateTotal,
                DOCUMENT_COUNT - WARMUP_COUNT,
                peakHeap,
                totalOutputBytes,
                DOCUMENT_COUNT,
                perDocNanos
        );
    }

    private Map<String, Object> buildFreemarkerModel(Map<String, Object> address, String qrDataUri) {
        Map<String, Object> model = new HashMap<>(address);
        model.put("date", "24. Februar 2026");
        model.put("subject", "Betreff: Wichtige Mitteilung");
        model.put("bodyText", "Sehr geehrte Damen und Herren, hiermit möchten wir Sie über wichtige Änderungen informieren. "
                + "Bitte lesen Sie dieses Schreiben sorgfältig durch und bewahren Sie es für Ihre Unterlagen auf.");
        model.put("showNotice", true);
        model.put("noticeText", "Hinweis: Bitte beachten Sie die beigefügten Unterlagen und senden Sie uns Ihre Rückmeldung bis zum 15.03.2026.");
        model.put("qrCodeDataUri", qrDataUri);
        model.put("senderName", "Dr. Anna Schmidt");
        model.put("termsText", "Es gelten die Allgemeinen Geschäftsbedingungen der ACME GmbH in der jeweils gültigen Fassung. "
                + "Änderungen bedürfen der Schriftform. Der Gerichtsstand ist München.");
        model.put("showDisclaimer", true);
        model.put("disclaimerText", "Haftungsausschluss: Dieses Dokument dient ausschließlich zu Informationszwecken. "
                + "Eine Haftung für die Richtigkeit und Vollständigkeit wird nicht übernommen.");
        model.put("contactInfo", "Kontakt: info@acme-gmbh.de | Tel: +49 89 123456-0 | Fax: +49 89 123456-99");
        return model;
    }

    private Map<String, Object> buildJasperParams(Map<String, Object> address, String qrBase64) {
        Map<String, Object> params = new HashMap<>(address);
        params.put("date", "24. Februar 2026");
        params.put("subject", "Betreff: Wichtige Mitteilung");
        params.put("bodyText", "Sehr geehrte Damen und Herren, hiermit möchten wir Sie über wichtige Änderungen informieren. "
                + "Bitte lesen Sie dieses Schreiben sorgfältig durch und bewahren Sie es für Ihre Unterlagen auf.");
        params.put("showNotice", Boolean.TRUE);
        params.put("noticeText", "Hinweis: Bitte beachten Sie die beigefügten Unterlagen und senden Sie uns Ihre Rückmeldung bis zum 15.03.2026.");
        params.put("qrCodeBase64", qrBase64);
        params.put("senderName", "Dr. Anna Schmidt");
        params.put("termsText", "Es gelten die Allgemeinen Geschäftsbedingungen der ACME GmbH in der jeweils gültigen Fassung. "
                + "Änderungen bedürfen der Schriftform. Der Gerichtsstand ist München.");
        params.put("showDisclaimer", Boolean.TRUE);
        params.put("disclaimerText", "Haftungsausschluss: Dieses Dokument dient ausschließlich zu Informationszwecken. "
                + "Eine Haftung für die Richtigkeit und Vollständigkeit wird nicht übernommen.");
        params.put("contactInfo", "Kontakt: info@acme-gmbh.de | Tel: +49 89 123456-0 | Fax: +49 89 123456-99");
        return params;
    }

    private void printComparisonTable(BenchmarkResult... results) {
        // Build format string dynamically based on number of results
        StringBuilder headerFmt = new StringBuilder("%-28s");
        for (int i = 0; i < results.length; i++) {
            headerFmt.append(" %20s");
        }
        String hfmt = headerFmt.toString();

        Object[] headerArgs = new Object[results.length + 1];
        headerArgs[0] = "";
        for (int i = 0; i < results.length; i++) {
            headerArgs[i + 1] = results[i].engineName();
        }

        String header = String.format(hfmt, headerArgs);
        String sep = "-".repeat(header.length());

        System.out.println(sep);
        System.out.println(header);
        System.out.println(sep);

        printRow(results, "Total time", r -> fmt("%.1fs", r.totalTimeSeconds()));
        printRow(results, "Throughput", r -> fmt("%.0f PDFs/s", r.throughputPerSecond()));
        printRow(results, "Avg time per PDF", r -> fmt("%.2fms", r.avgTimePerDocMillis()));
        printRow(results, "Warmup (first PDF)", r -> fmt("%.0fms", r.warmupTimeMillis()));
        printRow(results, "Steady-state throughput", r -> fmt("%.0f PDFs/s", r.steadyStateThroughput()));
        printRow(results, "Peak heap memory", r -> fmt("%.0f MB", r.peakHeapMB()));
        printRow(results, "Total output size", r -> fmt("%.0f MB", r.totalOutputMB()));
        printRow(results, "Avg PDF file size", r -> fmt("%.1f KB", r.avgFileSizeKB()));
        printRow(results, "P50 latency", r -> fmt("%.1fms", r.percentileMillis(50)));
        printRow(results, "P95 latency", r -> fmt("%.1fms", r.percentileMillis(95)));
        printRow(results, "P99 latency", r -> fmt("%.1fms", r.percentileMillis(99)));

        System.out.println(sep);
    }

    @FunctionalInterface
    private interface MetricFormatter {
        String format(BenchmarkResult result);
    }

    private void printRow(BenchmarkResult[] results, String label, MetricFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-28s", label));
        for (BenchmarkResult r : results) {
            sb.append(String.format(" %20s", formatter.format(r)));
        }
        System.out.println(sb);
    }

    private String fmt(String format, Object... args) {
        return String.format(format, args);
    }
}
