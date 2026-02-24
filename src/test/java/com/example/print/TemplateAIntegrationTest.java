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

class TemplateAIntegrationTest {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("template-a.pdf");

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    private static String loadQrImageAsDataUri() throws IOException {
        try (InputStream is = TemplateAIntegrationTest.class.getClassLoader()
                .getResourceAsStream("images/qr-sample.png")) {
            assertNotNull(is, "qr-sample.png must exist on classpath");
            byte[] bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }

    private byte[] generateTemplateAPdf() throws IOException {
        String qrDataUri = loadQrImageAsDataUri();

        Map<String, Object> model = new HashMap<>();
        model.put("recipientName", "Hans Müller");
        model.put("recipientStreet", "Königstraße 42");
        model.put("recipientCity", "80331 München");
        model.put("date", "18. Februar 2026");
        model.put("subject", "Betreff: Wichtige Mitteilung");
        model.put("bodyText", "Sehr geehrter Herr Müller, hiermit möchten wir Sie über wichtige Änderungen informieren.");
        model.put("showNotice", true);
        model.put("noticeText", "Hinweis: Bitte beachten Sie die beigefügten Unterlagen.");
        model.put("qrCodeDataUri", qrDataUri);
        model.put("senderName", "Dr. Anna Schmidt");
        model.put("termsText", "Es gelten die Allgemeinen Geschäftsbedingungen der ACME GmbH.");
        model.put("showDisclaimer", true);
        model.put("disclaimerText", "Haftungsausschluss: Dieses Dokument dient ausschließlich zu Informationszwecken.");
        model.put("contactInfo", "Kontakt: info@acme-gmbh.de | Tel: +49 89 123456-0");

        FreemarkerRenderer renderer = new FreemarkerRenderer();
        String renderedHtml = renderer.render("template-a", model);
        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();

        PdfGenerator pdfGenerator = new PdfGenerator();
        return pdfGenerator.generatePdf(renderedHtml, baseUri);
    }

    @Test
    void testGenerateTemplateAPdf() throws IOException {
        byte[] pdfBytes = generateTemplateAPdf();
        Files.write(OUTPUT_FILE, pdfBytes);

        assertTrue(Files.exists(OUTPUT_FILE), "template-a.pdf must exist");
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "PDF must start with %PDF magic bytes");
        assertTrue(pdfBytes.length > 10 * 1024,
                "PDF must be > 10KB (contains font subset + QR image), actual: " + pdfBytes.length);
    }

    @Test
    void testTemplateAContentAssertions() throws IOException {
        byte[] pdfBytes = generateTemplateAPdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertTrue(text.contains("Hans Müller"), "PDF must contain 'Hans Müller', actual text: " + text);
            assertTrue(text.contains("Dr. Anna Schmidt"), "PDF must contain 'Dr. Anna Schmidt'");
            assertTrue(text.contains("ACME GmbH"), "PDF must contain 'ACME GmbH'");
            assertTrue(text.contains("Wichtige Mitteilung"), "PDF must contain 'Wichtige Mitteilung'");
        }
    }
}
