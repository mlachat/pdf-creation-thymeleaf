package com.example.print.template;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FreemarkerRendererTest {

    private final FreemarkerRenderer renderer = new FreemarkerRenderer();

    @Test
    void render_withGermanUmlauts_containsRenderedValues() {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Max Müller");
        model.put("street", "Königstraße 42");
        model.put("city", "80331 München");
        model.put("heading", "Testbrief");
        model.put("message", "Dies ist ein Test.");
        model.put("title", "Test");

        String html = renderer.render("test-freemarker", model);

        assertTrue(html.contains("Max Müller"), "Rendered HTML must contain 'Max Müller'");
        assertTrue(html.contains("Königstraße 42"), "Rendered HTML must contain 'Königstraße 42'");
        assertTrue(html.contains("80331 München"), "Rendered HTML must contain '80331 München'");
        assertTrue(html.contains("Testbrief"), "Rendered HTML must contain 'Testbrief'");
        assertTrue(html.contains("Dies ist ein Test."), "Rendered HTML must contain message text");
        assertFalse(html.contains("<#"), "Rendered HTML must not contain FreeMarker directives");
    }

    @Test
    void render_withNullQrCode_omitsQrSection() {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Test");
        model.put("street", "Street");
        model.put("city", "City");
        model.put("heading", "Heading");
        model.put("message", "Message");
        model.put("title", "Title");
        model.put("qrCodeDataUri", null);

        String html = renderer.render("test-freemarker", model);

        assertFalse(html.contains("qr-section"), "QR section must not be present when qrCodeDataUri is null");
    }

    @Test
    void render_withQrCodeDataUri_includesImgTag() {
        Map<String, Object> model = new HashMap<>();
        model.put("name", "Test");
        model.put("street", "Street");
        model.put("city", "City");
        model.put("heading", "Heading");
        model.put("message", "Message");
        model.put("title", "Title");
        model.put("qrCodeDataUri", "data:image/png;base64,ABC");

        String html = renderer.render("test-freemarker", model);

        assertTrue(html.contains("qr-section"), "QR section must be present when qrCodeDataUri is set");
        assertTrue(html.contains("data:image/png;base64,ABC"), "Img src must contain the data URI");
    }
}
