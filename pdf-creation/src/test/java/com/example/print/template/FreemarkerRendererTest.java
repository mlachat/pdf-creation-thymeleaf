package com.example.print.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class FreemarkerRendererTest {

    private final FreemarkerRenderer renderer = new FreemarkerRenderer();

    @Test
    @DisplayName("Renders German umlauts correctly in output HTML")
    void render_withGermanUmlauts_containsRenderedValues() {
        Map<String, Object> model = Map.ofEntries(
                Map.entry("name", "Max Müller"),
                Map.entry("street", "Königstraße 42"),
                Map.entry("city", "80331 München"),
                Map.entry("heading", "Testbrief"),
                Map.entry("message", "Dies ist ein Test."),
                Map.entry("title", "Test"),
                Map.entry("senderLine", "Absender GmbH | Musterstraße 1 | 12345 Berlin")
        );

        var html = renderer.render("test-freemarker", model);

        assertThat(html)
                .contains("Max Müller", "Königstraße 42", "80331 München", "Testbrief", "Dies ist ein Test.")
                .doesNotContain("<#");
    }

    @Test
    @DisplayName("Null QR code data URI omits QR section from output")
    void render_withNullQrCode_omitsQrSection() {
        // HashMap required: Map.ofEntries() rejects null values
        var model = new HashMap<String, Object>();
        model.put("name", "Test");
        model.put("street", "Street");
        model.put("city", "City");
        model.put("heading", "Heading");
        model.put("message", "Message");
        model.put("title", "Title");
        model.put("qrCodeDataUri", null);
        model.put("senderLine", "Sender");

        var html = renderer.render("test-freemarker", model);

        assertThat(html).doesNotContain("qr-section");
    }

    @Test
    @DisplayName("QR code data URI includes img tag in output")
    void render_withQrCodeDataUri_includesImgTag() {
        Map<String, Object> model = Map.ofEntries(
                Map.entry("name", "Test"),
                Map.entry("street", "Street"),
                Map.entry("city", "City"),
                Map.entry("heading", "Heading"),
                Map.entry("message", "Message"),
                Map.entry("title", "Title"),
                Map.entry("qrCodeDataUri", "data:image/png;base64,ABC"),
                Map.entry("senderLine", "Sender")
        );

        var html = renderer.render("test-freemarker", model);

        assertThat(html).contains("qr-section", "data:image/png;base64,ABC");
    }

    @Test
    @DisplayName("Null template name throws NullPointerException")
    void render_nullTemplateName_throwsNpe() {
        assertThatThrownBy(() -> renderer.render(null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Null variables map throws NullPointerException")
    void render_nullVariables_throwsNpe() {
        assertThatThrownBy(() -> renderer.render("test-freemarker", null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Non-existent template throws IllegalStateException")
    void render_nonExistentTemplate_throwsException() {
        assertThatThrownBy(() -> renderer.render("does-not-exist", Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
