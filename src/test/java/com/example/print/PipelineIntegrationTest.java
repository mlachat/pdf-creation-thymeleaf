package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.qr.QrCodeGenerator;
import com.example.print.template.ThymeleafRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PipelineIntegrationTest {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("test-pipeline.pdf");

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    @Test
    void testFullPipeline() throws IOException {
        // 1. Generate QR code
        String qrDataUri = QrCodeGenerator.generateDataUri("https://example.com/doc/12345", 300);
        assertNotNull(qrDataUri);
        assertTrue(qrDataUri.startsWith("data:image/png;base64,"));

        // 2. Build model
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Hans Müller");
        model.put("street", "Königstraße 42");
        model.put("city", "80331 München");
        model.put("heading", "Testbrief");
        model.put("title", "Pipeline Test");
        model.put("message", "Sehr geehrter Herr Müller, dies ist ein automatisch generierter Testbrief.");
        model.put("qrCodeDataUri", qrDataUri);

        // 3. Render template
        ThymeleafRenderer renderer = new ThymeleafRenderer();
        String renderedHtml = renderer.render("test-thymeleaf", model);
        assertFalse(renderedHtml.contains("th:text"), "Rendered HTML must not contain Thymeleaf attributes");
        assertTrue(renderedHtml.contains("Hans Müller"), "Rendered HTML must contain dynamic data");

        // 4. Compute base URI (root of test-classes for CSS resolution)
        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();

        // 5. Generate PDF
        PdfGenerator pdfGenerator = new PdfGenerator();
        byte[] pdfBytes = pdfGenerator.generatePdf(renderedHtml, baseUri);

        // 6. Write to file
        Files.write(OUTPUT_FILE, pdfBytes);

        // 7. Assertions
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "PDF must start with %PDF magic bytes");
        assertTrue(pdfBytes.length > 5 * 1024, "PDF must be > 5KB (contains font subset + QR image), actual: " + pdfBytes.length);
        assertTrue(Files.exists(OUTPUT_FILE), "test-pipeline.pdf must exist");
        assertTrue(Files.isReadable(OUTPUT_FILE), "test-pipeline.pdf must be readable");
    }

    @Test
    void testPdfContainsExpectedText() throws IOException {
        // Generate the PDF first (same as testFullPipeline)
        String qrDataUri = QrCodeGenerator.generateDataUri("https://example.com/doc/12345", 300);

        Map<String, Object> model = new HashMap<>();
        model.put("name", "Hans Müller");
        model.put("street", "Königstraße 42");
        model.put("city", "80331 München");
        model.put("heading", "Testbrief");
        model.put("title", "Pipeline Test");
        model.put("message", "Sehr geehrter Herr Müller, dies ist ein automatisch generierter Testbrief.");
        model.put("qrCodeDataUri", qrDataUri);

        ThymeleafRenderer renderer = new ThymeleafRenderer();
        String renderedHtml = renderer.render("test-thymeleaf", model);
        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();

        PdfGenerator pdfGenerator = new PdfGenerator();
        byte[] pdfBytes = pdfGenerator.generatePdf(renderedHtml, baseUri);

        // Extract text with PDFBox and verify umlauts
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertTrue(text.contains("Hans Müller"), "PDF text must contain 'Hans Müller', actual: " + text);
            assertTrue(text.contains("Königstraße"), "PDF text must contain 'Königstraße', actual: " + text);
            assertTrue(text.contains("München"), "PDF text must contain 'München', actual: " + text);
        }
    }
}
