package com.example.print.itext;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.styledxmlparser.css.media.MediaDeviceDescription;
import com.itextpdf.styledxmlparser.css.media.MediaType;
import com.itextpdf.layout.font.FontProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Generates PDF documents from HTML using iText pdfHTML (html2pdf).
 *
 * <p>The constructor extracts a bundled DejaVuSans font to a temporary file
 * (deleted on JVM exit) and configures the iText converter.</p>
 */
public class ItextPdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(ItextPdfGenerator.class);

    private static final String FONT_RESOURCE_PATH = "fonts/DejaVuSans.ttf";
    private static final int INITIAL_BUFFER_SIZE = 65_536;

    private final ConverterProperties converterProperties;

    public ItextPdfGenerator() {
        this(null);
    }

    /**
     * Creates a new generator with an optional base URI for resolving relative resources.
     *
     * @param baseUri base URI for CSS/image resolution, may be {@code null}
     */
    public ItextPdfGenerator(String baseUri) {
        var fontProvider = new FontProvider();
        fontProvider.addStandardPdfFonts();

        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream(FONT_RESOURCE_PATH)) {
            if (fontStream == null) {
                throw new IllegalStateException("Font file not found on classpath: " + FONT_RESOURCE_PATH);
            }
            var tempFont = Files.createTempFile("DejaVuSans", ".ttf");
            tempFont.toFile().deleteOnExit(); // acceptable for CLI/batch — leaks in long-running servers
            Files.copy(fontStream, tempFont, StandardCopyOption.REPLACE_EXISTING);
            fontProvider.addFont(tempFont.toString());
        } catch (IOException e) {
            log.error("Failed to load font", e);
            throw new UncheckedIOException("Failed to load font", e);
        }

        converterProperties = new ConverterProperties();
        converterProperties.setFontProvider(fontProvider);
        converterProperties.setMediaDeviceDescription(new MediaDeviceDescription(MediaType.PRINT));
        if (baseUri != null) {
            converterProperties.setBaseUri(baseUri);
        }
    }

    /**
     * Converts the given HTML string into a PDF byte array.
     *
     * @param html HTML content to convert; must not be {@code null}
     * @return the generated PDF as a byte array
     * @throws NullPointerException  if {@code html} is {@code null}
     * @throws IllegalStateException if PDF generation fails
     */
    public byte[] generatePdf(String html) {
        Objects.requireNonNull(html, "html must not be null");
        log.debug("Starting iText PDF generation");
        try {
            var os = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            HtmlConverter.convertToPdf(html, os, converterProperties);
            log.debug("iText PDF generation complete ({} bytes)", os.size());
            return os.toByteArray();
        } catch (RuntimeException e) {
            log.error("Failed to generate PDF", e);
            throw new IllegalStateException("Failed to generate PDF", e);
        }
    }
}
