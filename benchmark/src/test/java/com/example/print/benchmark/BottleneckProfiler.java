package com.example.print.benchmark;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BottleneckProfiler {

    private static final int DOCUMENT_COUNT = 500;

    @Test
    void profileBottleneck() throws Exception {
        System.out.println("\n=== Bottleneck Profiler: " + DOCUMENT_COUNT + " Documents ===\n");

        // Pre-generate addresses
        AddressGenerator addressGen = new AddressGenerator(42);
        List<Map<String, Object>> addresses = new ArrayList<>(DOCUMENT_COUNT);
        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            addresses.add(addressGen.generateAddress());
        }

        // Load QR image once
        String qrDataUri;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("images/qr-sample.png")) {
            if (is == null) {
                throw new RuntimeException("qr-sample.png not found on classpath");
            }
            byte[] qrBytes = is.readAllBytes();
            qrDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(qrBytes);
        }

        // Build all models upfront so model-building time is not included
        List<Map<String, Object>> models = new ArrayList<>(DOCUMENT_COUNT);
        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            models.add(buildFreemarkerModel(addresses.get(i), qrDataUri));
        }

        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();

        // --- Time PdfGenerator constructor (font loading) ---
        long ctorStart = System.nanoTime();
        PdfGenerator pdfGenerator = new PdfGenerator();
        long ctorNanos = System.nanoTime() - ctorStart;

        // --- Time FreemarkerRenderer constructor ---
        long rendererCtorStart = System.nanoTime();
        FreemarkerRenderer renderer = new FreemarkerRenderer();
        long rendererCtorNanos = System.nanoTime() - rendererCtorStart;

        // --- Main loop: time render and PDF generation separately ---
        long[] renderNanos = new long[DOCUMENT_COUNT];
        long[] pdfGenNanos = new long[DOCUMENT_COUNT];

        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            Map<String, Object> model = models.get(i);

            long t0 = System.nanoTime();
            String html = renderer.render("template-a", model);
            long t1 = System.nanoTime();
            pdfGenerator.generatePdf(html, baseUri);
            long t2 = System.nanoTime();

            renderNanos[i] = t1 - t0;
            pdfGenNanos[i] = t2 - t1;

            if ((i + 1) % 100 == 0) {
                System.out.println("  " + (i + 1) + "/" + DOCUMENT_COUNT + " done");
            }
        }

        // --- Compute totals ---
        long totalRenderNanos = 0;
        long totalPdfGenNanos = 0;
        for (int i = 0; i < DOCUMENT_COUNT; i++) {
            totalRenderNanos += renderNanos[i];
            totalPdfGenNanos += pdfGenNanos[i];
        }

        long totalCombined = totalRenderNanos + totalPdfGenNanos;
        double renderPct = 100.0 * totalRenderNanos / totalCombined;
        double pdfGenPct = 100.0 * totalPdfGenNanos / totalCombined;

        double totalRenderMs = totalRenderNanos / 1_000_000.0;
        double totalPdfGenMs = totalPdfGenNanos / 1_000_000.0;
        double totalCombinedMs = totalCombined / 1_000_000.0;

        double avgRenderMs = totalRenderMs / DOCUMENT_COUNT;
        double avgPdfGenMs = totalPdfGenMs / DOCUMENT_COUNT;
        double avgCombinedMs = totalCombinedMs / DOCUMENT_COUNT;

        // --- Print results ---
        System.out.println();
        String sep = "=".repeat(72);
        System.out.println(sep);
        System.out.println("  BOTTLENECK BREAKDOWN (" + DOCUMENT_COUNT + " documents)");
        System.out.println(sep);
        System.out.println();

        System.out.printf("  %-30s %12s %12s %8s%n", "Phase", "Total (ms)", "Avg/doc (ms)", "Share");
        System.out.println("  " + "-".repeat(66));

        System.out.printf("  %-30s %12.1f %12.3f %7.1f%%%n",
                "FreeMarker rendering", totalRenderMs, avgRenderMs, renderPct);
        System.out.printf("  %-30s %12.1f %12.3f %7.1f%%%n",
                "OpenHTMLtoPDF PDF generation", totalPdfGenMs, avgPdfGenMs, pdfGenPct);

        System.out.println("  " + "-".repeat(66));
        System.out.printf("  %-30s %12.1f %12.3f %7.1f%%%n",
                "Combined", totalCombinedMs, avgCombinedMs, 100.0);

        System.out.println();
        System.out.println("  Initialization costs (one-time):");
        System.out.printf("    PdfGenerator constructor:      %10.1f ms%n", ctorNanos / 1_000_000.0);
        System.out.printf("    FreemarkerRenderer constructor: %10.1f ms%n", rendererCtorNanos / 1_000_000.0);

        System.out.println();
        System.out.println(sep);
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
}
