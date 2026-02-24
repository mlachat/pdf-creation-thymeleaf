package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
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

    private static String loadQrImageAsDataUri() throws IOException {
        try (InputStream is = PipelineIntegrationTest.class.getClassLoader()
                .getResourceAsStream("images/qr-sample.png")) {
            assertNotNull(is, "qr-sample.png must exist on classpath");
            byte[] bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }

    @Test
    void testFullPipeline() throws IOException {
        // 1. Load static QR image
        String qrDataUri = loadQrImageAsDataUri();
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
        FreemarkerRenderer renderer = new FreemarkerRenderer();
        String renderedHtml = renderer.render("test-freemarker", model);
        assertFalse(renderedHtml.contains("<#"), "Rendered HTML must not contain FreeMarker directives");
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
        // Generate the PDF
        String qrDataUri = loadQrImageAsDataUri();

        Map<String, Object> model = new HashMap<>();
        model.put("name", "Hans Müller");
        model.put("street", "Königstraße 42");
        model.put("city", "80331 München");
        model.put("heading", "Testbrief");
        model.put("title", "Pipeline Test");
        model.put("message", "Sehr geehrter Herr Müller, dies ist ein automatisch generierter Testbrief.");
        model.put("qrCodeDataUri", qrDataUri);

        FreemarkerRenderer renderer = new FreemarkerRenderer();
        String renderedHtml = renderer.render("test-freemarker", model);
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
