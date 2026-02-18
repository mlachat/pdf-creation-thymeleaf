package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.qr.QrCodeGenerator;
import com.example.print.template.ThymeleafRenderer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateAIntegrationTest {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("template-a.pdf");
    private static final String QR_URL = "https://acme-gmbh.de/doc/A-2026-001";

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    private byte[] generateTemplateAPdf() throws IOException {
        String qrDataUri = QrCodeGenerator.generateDataUri(QR_URL, 300);

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

        ThymeleafRenderer renderer = new ThymeleafRenderer();
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

    @Test
    void testTemplateAQrCodeScannable() throws Exception {
        byte[] pdfBytes = generateTemplateAPdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage pageImage = pdfRenderer.renderImageWithDPI(0, 150);

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(pageImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            Result result = reader.decode(bitmap);

            assertEquals(QR_URL, result.getText(),
                    "QR code must decode to the expected URL");
        }
    }
}
