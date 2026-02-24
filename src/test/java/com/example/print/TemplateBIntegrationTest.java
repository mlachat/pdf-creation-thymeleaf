package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
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

class TemplateBIntegrationTest {

    private static final Path OUTPUT_DIR = Paths.get("target/test-output");
    private static final Path OUTPUT_FILE = OUTPUT_DIR.resolve("template-b.pdf");

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(OUTPUT_DIR);
    }

    private byte[] generateTemplateBPdf() throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("title", "Produktinformation");
        model.put("subtitle", "Premium Dienstleistungspaket");
        model.put("productName", "Premium Dienstleistungspaket");
        model.put("productDescription", "Unser umfassendes Dienstleistungspaket bietet Ihnen maßgeschneiderte Lösungen.");
        model.put("price", "EUR 2.499,00 zzgl. MwSt.");
        model.put("availability", "Sofort verfügbar");
        model.put("contactPerson", "Thomas Müller");
        model.put("phone", "+49 89 123456-10");
        model.put("email", "vertrieb@acme-gmbh.de");
        model.put("companyAddress", "ACME GmbH, Innovationsweg 10, 80339 München");
        model.put("showSpecialOffer", true);
        model.put("specialOfferText", "Sonderangebot: 15% Rabatt bei Bestellung bis zum 31.03.2026!");
        model.put("footerText", "Alle Preise verstehen sich zzgl. der gesetzlichen Mehrwertsteuer.");

        FreemarkerRenderer renderer = new FreemarkerRenderer();
        String renderedHtml = renderer.render("template-b", model);
        String baseUri = getClass().getClassLoader().getResource("").toExternalForm();

        PdfGenerator pdfGenerator = new PdfGenerator();
        return pdfGenerator.generatePdf(renderedHtml, baseUri);
    }

    @Test
    void testGenerateTemplateBPdf() throws IOException {
        byte[] pdfBytes = generateTemplateBPdf();
        Files.write(OUTPUT_FILE, pdfBytes);

        assertTrue(Files.exists(OUTPUT_FILE), "template-b.pdf must exist");
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF"), "PDF must start with %PDF magic bytes");
        assertTrue(pdfBytes.length > 5 * 1024,
                "PDF must be > 5KB (contains font subset), actual: " + pdfBytes.length);
    }

    @Test
    void testTemplateBContentAssertions() throws IOException {
        byte[] pdfBytes = generateTemplateBPdf();

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            assertTrue(text.contains("Thomas Müller"), "PDF must contain 'Thomas Müller', actual text: " + text);
            assertTrue(text.contains("Produktinformation"), "PDF must contain 'Produktinformation'");
            assertTrue(text.contains("EUR 2.499"), "PDF must contain 'EUR 2.499'");
            assertTrue(text.contains("ACME GmbH"), "PDF must contain 'ACME GmbH'");
            assertTrue(text.contains("Sonderangebot"), "PDF must contain 'Sonderangebot'");
        }
    }
}
