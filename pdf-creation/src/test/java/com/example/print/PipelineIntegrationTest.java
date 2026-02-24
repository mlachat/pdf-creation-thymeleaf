package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class PipelineIntegrationTest {

    private static final Path OUTPUT_FILE = TestHelper.outputFile("test-pipeline.pdf");

    @BeforeAll
    static void setUp() throws IOException {
        TestHelper.ensureOutputDir();
    }

    @Test
    @DisplayName("Full pipeline: render FreeMarker template and generate PDF file")
    void testFullPipeline() throws IOException {
        // 1. Load static QR image
        var qrDataUri = TestHelper.loadQrImageAsDataUri();
        assertThat(qrDataUri).isNotNull();
        assertThat(qrDataUri).startsWith("data:image/png;base64,");

        // 2. Build model
        Map<String, Object> model = Map.ofEntries(
                Map.entry("name", "Hans Müller"),
                Map.entry("street", "Königstraße 42"),
                Map.entry("city", "80331 München"),
                Map.entry("heading", "Testbrief"),
                Map.entry("title", "Pipeline Test"),
                Map.entry("message", "Sehr geehrter Herr Müller, dies ist ein automatisch generierter Testbrief."),
                Map.entry("qrCodeDataUri", qrDataUri),
                Map.entry("senderLine", "ACME GmbH | Innovationsweg 10 | 80339 München")
        );

        // 3. Render template
        var renderer = new FreemarkerRenderer();
        var renderedHtml = renderer.render("test-freemarker", model);
        assertThat(renderedHtml).doesNotContain("<#");
        assertThat(renderedHtml).contains("Hans Müller");

        // 4. Compute base URI
        var baseUri = TestHelper.testClasspathBaseUri(getClass());

        // 5. Generate PDF
        var pdfGenerator = new PdfGenerator();
        var pdfBytes = pdfGenerator.generatePdf(renderedHtml, baseUri);

        // 6. Write to file
        Files.write(OUTPUT_FILE, pdfBytes);

        // 7. Assertions
        TestHelper.assertValidPdf(pdfBytes, 5);
        assertThat(OUTPUT_FILE).exists().isReadable();
    }

    @Test
    @DisplayName("Generated PDF contains expected German text")
    void testPdfContainsExpectedText() throws IOException {
        var qrDataUri = TestHelper.loadQrImageAsDataUri();

        Map<String, Object> model = Map.ofEntries(
                Map.entry("name", "Hans Müller"),
                Map.entry("street", "Königstraße 42"),
                Map.entry("city", "80331 München"),
                Map.entry("heading", "Testbrief"),
                Map.entry("title", "Pipeline Test"),
                Map.entry("message", "Sehr geehrter Herr Müller, dies ist ein automatisch generierter Testbrief."),
                Map.entry("qrCodeDataUri", qrDataUri),
                Map.entry("senderLine", "ACME GmbH | Innovationsweg 10 | 80339 München")
        );

        var renderer = new FreemarkerRenderer();
        var renderedHtml = renderer.render("test-freemarker", model);
        var baseUri = TestHelper.testClasspathBaseUri(getClass());

        var pdfGenerator = new PdfGenerator();
        var pdfBytes = pdfGenerator.generatePdf(renderedHtml, baseUri);

        var text = TestHelper.extractPdfText(pdfBytes);

        assertThat(text).contains("Hans Müller", "Königstraße", "München");
    }
}
