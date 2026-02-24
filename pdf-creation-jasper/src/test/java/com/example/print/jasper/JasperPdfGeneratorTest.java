package com.example.print.jasper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JasperPdfGeneratorTest {

    private static final byte[] PDF_MAGIC_BYTES = "%PDF".getBytes(StandardCharsets.US_ASCII);

    /**
     * Creates a minimal valid PNG using Java's ImageIO to ensure full compatibility
     * with the old iText/lowagie PNG decoder used by JasperReports.
     */
    private static String createMinimalPngBase64() throws IOException {
        var img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        img.setRGB(0, 0, 0xFFFFFF);
        var baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // HashMap required: JasperReports internally mutates the parameters map
    private static Map<String, Object> fullParameterMap() throws IOException {
        var params = new HashMap<String, Object>();
        params.put("recipientName", "Hans Müller");
        params.put("recipientStreet", "Königstraße 42");
        params.put("recipientCity", "80331 München");
        params.put("date", "24. Februar 2026");
        params.put("subject", "Betreff: Testbrief");
        params.put("bodyText", "Sehr geehrter Herr Müller, dies ist ein Testbrief.");
        params.put("showNotice", true);
        params.put("noticeText", "Hinweis: Bitte beachten.");
        params.put("qrCodeBase64", createMinimalPngBase64());
        params.put("senderName", "Dr. Anna Schmidt");
        params.put("termsText", "Es gelten die AGB.");
        params.put("showDisclaimer", true);
        params.put("disclaimerText", "Haftungsausschluss.");
        params.put("contactInfo", "info@acme-gmbh.de");
        return params;
    }

    @Test
    @DisplayName("All parameters produce a valid PDF with correct magic bytes")
    void generatePdf_withAllParameters_returnsValidPdf() throws IOException {
        var generator = new JasperPdfGenerator();
        var pdf = generator.generatePdf(fullParameterMap());

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(pdf).startsWith(PDF_MAGIC_BYTES);
    }

    @Test
    @DisplayName("Null parameters throws NullPointerException")
    void generatePdf_nullParameters_throwsNpe() {
        var generator = new JasperPdfGenerator();
        assertThatThrownBy(() -> generator.generatePdf(null))
                .isInstanceOf(NullPointerException.class);
    }
}
