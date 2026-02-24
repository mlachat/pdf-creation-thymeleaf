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

class TemplateAIntegrationTest {

    private static final Path OUTPUT_FILE = TestHelper.outputFile("template-a.pdf");

    @BeforeAll
    static void setUp() throws IOException {
        TestHelper.ensureOutputDir();
    }

    private byte[] generateTemplateAPdf() throws IOException {
        var qrDataUri = TestHelper.loadQrImageAsDataUri();

        Map<String, Object> model = Map.ofEntries(
                Map.entry("recipientName", "Hans Müller"),
                Map.entry("recipientStreet", "Königstraße 42"),
                Map.entry("recipientCity", "80331 München"),
                Map.entry("date", "18. Februar 2026"),
                Map.entry("subject", "Betreff: Wichtige Mitteilung"),
                Map.entry("bodyText", "Sehr geehrter Herr Müller, hiermit möchten wir Sie über wichtige Änderungen informieren."),
                Map.entry("showNotice", true),
                Map.entry("noticeText", "Hinweis: Bitte beachten Sie die beigefügten Unterlagen."),
                Map.entry("qrCodeDataUri", qrDataUri),
                Map.entry("senderName", "Dr. Anna Schmidt"),
                Map.entry("termsText", "Es gelten die Allgemeinen Geschäftsbedingungen der ACME GmbH."),
                Map.entry("showDisclaimer", true),
                Map.entry("disclaimerText", "Haftungsausschluss: Dieses Dokument dient ausschließlich zu Informationszwecken."),
                Map.entry("contactInfo", "Kontakt: info@acme-gmbh.de | Tel: +49 89 123456-0"),
                Map.entry("senderLine", "ACME GmbH | Innovationsweg 10 | 80339 München")
        );

        var renderer = new FreemarkerRenderer();
        var renderedHtml = renderer.render("template-a", model);
        var baseUri = TestHelper.testClasspathBaseUri(getClass());

        var pdfGenerator = new PdfGenerator();
        return pdfGenerator.generatePdf(renderedHtml, baseUri);
    }

    @Test
    @DisplayName("Template A generates a valid PDF file")
    void testGenerateTemplateAPdf() throws IOException {
        var pdfBytes = generateTemplateAPdf();
        Files.write(OUTPUT_FILE, pdfBytes);

        assertThat(OUTPUT_FILE).exists();
        TestHelper.assertValidPdf(pdfBytes, 10);
    }

    @Test
    @DisplayName("Template A PDF contains expected content")
    void testTemplateAContentAssertions() throws IOException {
        var pdfBytes = generateTemplateAPdf();
        var text = TestHelper.extractPdfText(pdfBytes);

        assertThat(text).contains("Hans Müller", "Dr. Anna Schmidt", "ACME GmbH", "Wichtige Mitteilung");
    }
}
