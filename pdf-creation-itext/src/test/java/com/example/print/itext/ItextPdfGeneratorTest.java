package com.example.print.itext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class ItextPdfGeneratorTest {

    private static final byte[] PDF_MAGIC_BYTES = "%PDF".getBytes(StandardCharsets.US_ASCII);

    @Test
    @DisplayName("Simple HTML produces a valid PDF")
    void generatePdf_simpleHtml_returnsValidPdf() {
        var generator = new ItextPdfGenerator();
        var pdf = generator.generatePdf("<html><body><p>Hello World</p></body></html>");

        assertThat(pdf).isNotNull().isNotEmpty();
        assertThat(pdf).startsWith(PDF_MAGIC_BYTES);
    }

    @Test
    @DisplayName("German text with umlauts produces a valid PDF over 1KB")
    void generatePdf_withGermanText_returnsValidPdf() {
        var generator = new ItextPdfGenerator();
        var html = """
                <html><body style="font-family: DejaVuSans, sans-serif;">\
                <p>Königstraße — Müller — Größe — Übung</p></body></html>""";
        var pdf = generator.generatePdf(html);

        assertThat(pdf).isNotNull();
        assertThat(pdf).startsWith(PDF_MAGIC_BYTES);
        assertThat(pdf.length).as("PDF with German text must be > 1KB").isGreaterThan(1024);
    }

    @Test
    @DisplayName("Null HTML input throws NullPointerException")
    void generatePdf_nullHtml_throwsNpe() {
        var generator = new ItextPdfGenerator();
        assertThatThrownBy(() -> generator.generatePdf(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Constructor with base URI creates a working instance")
    void constructor_withBaseUri_createsInstance() {
        var generator = new ItextPdfGenerator("file:///tmp/");
        assertThat(generator).isNotNull();

        var pdf = generator.generatePdf("<html><body><p>Test</p></body></html>");
        assertThat(pdf).isNotNull().isNotEmpty();
    }
}
