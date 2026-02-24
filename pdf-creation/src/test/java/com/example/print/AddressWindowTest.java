package com.example.print;

import com.example.print.pdf.PdfGenerator;
import com.example.print.template.FreemarkerRenderer;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class AddressWindowTest {

    private static final FreemarkerRenderer RENDERER = new FreemarkerRenderer();

    @BeforeAll
    static void setUp() throws IOException {
        TestHelper.ensureOutputDir();
    }

    private Map<String, Object> buildModel(String senderLine, String recipientName,
                                            String recipientStreet, String recipientCity) throws IOException {
        var qrDataUri = TestHelper.loadQrImageAsDataUri();
        var model = new HashMap<String, Object>();
        model.put("senderLine", senderLine);
        model.put("recipientName", recipientName);
        model.put("recipientStreet", recipientStreet);
        model.put("recipientCity", recipientCity);
        model.put("date", "24. Februar 2026");
        model.put("subject", "Betreff: Adressfenster-Test");
        model.put("bodyText", "Testinhalt.");
        model.put("showNotice", false);
        model.put("noticeText", "");
        model.put("qrCodeDataUri", qrDataUri);
        model.put("senderName", "Test Sender");
        model.put("termsText", "AGB gelten.");
        model.put("showDisclaimer", false);
        model.put("disclaimerText", "");
        model.put("contactInfo", "kontakt@test.de");
        return model;
    }

    private byte[] generatePdf(Map<String, Object> model) throws IOException {
        var html = RENDERER.render("template-a", model);
        var baseUri = TestHelper.testClasspathBaseUri(getClass());
        return new PdfGenerator().generatePdf(html, baseUri);
    }

    private void assertPageCount(byte[] pdfBytes, int expected) throws IOException {
        try (var document = Loader.loadPDF(pdfBytes)) {
            assertThat(document.getNumberOfPages()).isEqualTo(expected);
        }
    }

    @Test
    @DisplayName("Very long recipient name does not break layout")
    void veryLongRecipientName() throws IOException {
        var model = buildModel("ACME GmbH | Innovationsweg 10 | 80339 München",
                "Prof. Dr. Dr. h.c. mult. Maximilian Alexander Friedrich Wilhelm von und zu Hohenstaufen-Schwarzenberg",
                "Königstraße 42", "80331 München");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-long-name.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains("Prof. Dr. Dr. h.c. mult. Maximilian");
        assertThat(text).contains("Hohenstaufen-Schwarzenberg");
        assertPageCount(pdfBytes, 2);
    }

    @Test
    @DisplayName("Very long street name does not break layout")
    void veryLongStreetName() throws IOException {
        var model = buildModel("ACME GmbH | Innovationsweg 10 | 80339 München",
                "Hans Müller",
                "Obere Königsberger Landstraße am Waldfriedhof gegenüber dem Rathaus 142a",
                "80331 München");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-long-street.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains("Obere Königsberger Landstraße");
        assertThat(text).contains("142a");
        assertPageCount(pdfBytes, 2);
    }

    @Test
    @DisplayName("Very long city name does not break layout")
    void veryLongCity() throws IOException {
        var model = buildModel("ACME GmbH | Innovationsweg 10 | 80339 München",
                "Hans Müller", "Königstraße 42",
                "01234 Obertshausen-Waldschmidtheim im Landkreis Ludwigshafen am Rhein, Rheinland-Pfalz");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-long-city.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains("01234 Obertshausen-Waldschmidtheim");
        assertThat(text).contains("Rheinland-Pfalz");
        assertPageCount(pdfBytes, 2);
    }

    @Test
    @DisplayName("Maximum 6 address lines all present in PDF")
    void maximumAddressLines() throws IOException {
        var model = buildModel("ACME GmbH | Innovationsweg 10 | 80339 München",
                "Hans Müller", "Königstraße 42", "80331 München");
        model.put("recipientCompany", "Deutsche Muster-Konzern AG");
        model.put("recipientDepartment", "Abteilung Einkauf und Beschaffung");
        model.put("recipientExtra", "z. Hd. Frau Dr. Schmidt");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-max-lines.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains(
                "Deutsche Muster-Konzern AG",
                "Abteilung Einkauf und Beschaffung",
                "z. Hd. Frau Dr. Schmidt",
                "Hans Müller",
                "Königstraße 42",
                "80331 München"
        );
        assertPageCount(pdfBytes, 2);
    }

    @Test
    @DisplayName("Minimal address with only name generates valid PDF")
    void minimalAddressOnlyName() throws IOException {
        var model = buildModel("ACME GmbH", "Hans Müller", "", "");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-minimal.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains("Hans Müller");
        assertPageCount(pdfBytes, 2);
    }

    @Test
    @DisplayName("Special characters and German umlauts render correctly")
    void specialCharactersAndUmlauts() throws IOException {
        var model = buildModel("Müller und Söhne GbR | Straße 1 | 01234 Lübeck",
                "Ärztekammer Österreich", "Überseeallee 42ß", "80331 München-Süd");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-umlauts.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains("Ärztekammer", "Österreich", "Überseeallee", "München-Süd");
    }

    @Test
    @DisplayName("Sender return line text is present in generated PDF")
    void senderReturnLinePresent() throws IOException {
        var senderLine = "ACME GmbH | Innovationsweg 10 | 80339 München";
        var model = buildModel(senderLine,
                "Hans Müller", "Königstraße 42", "80331 München");

        var pdfBytes = generatePdf(model);
        Files.write(TestHelper.outputFile("address-window-sender-line.pdf"), pdfBytes);

        var text = TestHelper.extractPdfText(pdfBytes);
        assertThat(text).contains("ACME GmbH");
        assertThat(text).contains("Innovationsweg 10");
    }
}
