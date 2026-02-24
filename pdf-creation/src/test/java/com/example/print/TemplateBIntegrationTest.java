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

class TemplateBIntegrationTest {

    private static final Path OUTPUT_FILE = TestHelper.outputFile("template-b.pdf");

    @BeforeAll
    static void setUp() throws IOException {
        TestHelper.ensureOutputDir();
    }

    private byte[] generateTemplateBPdf() throws IOException {
        Map<String, Object> model = Map.ofEntries(
                Map.entry("title", "Produktinformation"),
                Map.entry("subtitle", "Premium Dienstleistungspaket"),
                Map.entry("productName", "Premium Dienstleistungspaket"),
                Map.entry("productDescription", "Unser umfassendes Dienstleistungspaket bietet Ihnen maßgeschneiderte Lösungen."),
                Map.entry("price", "EUR 2.499,00 zzgl. MwSt."),
                Map.entry("availability", "Sofort verfügbar"),
                Map.entry("contactPerson", "Thomas Müller"),
                Map.entry("phone", "+49 89 123456-10"),
                Map.entry("email", "vertrieb@acme-gmbh.de"),
                Map.entry("companyAddress", "ACME GmbH, Innovationsweg 10, 80339 München"),
                Map.entry("showSpecialOffer", true),
                Map.entry("specialOfferText", "Sonderangebot: 15% Rabatt bei Bestellung bis zum 31.03.2026!"),
                Map.entry("footerText", "Alle Preise verstehen sich zzgl. der gesetzlichen Mehrwertsteuer.")
        );

        var renderer = new FreemarkerRenderer();
        var renderedHtml = renderer.render("template-b", model);
        var baseUri = TestHelper.testClasspathBaseUri(getClass());

        var pdfGenerator = new PdfGenerator();
        return pdfGenerator.generatePdf(renderedHtml, baseUri);
    }

    @Test
    @DisplayName("Template B generates a valid PDF file")
    void testGenerateTemplateBPdf() throws IOException {
        var pdfBytes = generateTemplateBPdf();
        Files.write(OUTPUT_FILE, pdfBytes);

        assertThat(OUTPUT_FILE).exists();
        TestHelper.assertValidPdf(pdfBytes, 5);
    }

    @Test
    @DisplayName("Template B PDF contains expected content")
    void testTemplateBContentAssertions() throws IOException {
        var pdfBytes = generateTemplateBPdf();
        var text = TestHelper.extractPdfText(pdfBytes);

        assertThat(text).contains("Thomas Müller", "Produktinformation", "EUR 2.499", "ACME GmbH", "Sonderangebot");
    }
}
