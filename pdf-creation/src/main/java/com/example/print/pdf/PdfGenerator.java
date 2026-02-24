package com.example.print.pdf;

import com.openhtmltopdf.extend.FSCacheEx;
import com.openhtmltopdf.extend.FSCacheValue;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.extend.impl.FSDefaultCacheStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Generates PDF documents from XHTML using OpenHTMLtoPDF.
 *
 * <p>The constructor extracts a bundled DejaVuSans font to a temporary file
 * (deleted on JVM exit) and caches font metrics for reuse across calls.</p>
 */
public class PdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(PdfGenerator.class);

    private static final String FONT_RESOURCE_PATH = "fonts/DejaVuSans.ttf";
    private static final String FONT_FAMILY_NAME = "DejaVuSans";
    private static final int INITIAL_BUFFER_SIZE = 65_536;

    private final File fontFile;
    private final FSCacheEx<String, FSCacheValue> fontMetricsCache;

    public PdfGenerator() {
        this.fontFile = extractFontToTempFile();
        this.fontMetricsCache = new FSDefaultCacheStore();
    }

    /**
     * Renders the given XHTML string into a PDF byte array.
     *
     * @param html    well-formed XHTML content; must not be {@code null}
     * @param baseUri base URI for resolving relative resources (CSS, images), may be {@code null}
     * @return the generated PDF as a byte array
     * @throws NullPointerException  if {@code html} is {@code null}
     * @throws UncheckedIOException  if PDF generation fails due to an I/O error
     */
    public byte[] generatePdf(String html, String baseUri) {
        Objects.requireNonNull(html, "html must not be null");
        log.debug("Starting PDF generation (baseUri={})", baseUri);
        try {
            var os = new ByteArrayOutputStream(INITIAL_BUFFER_SIZE);
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useCacheStore(PdfRendererBuilder.CacheStore.PDF_FONT_METRICS, fontMetricsCache);
            builder.useFont(fontFile, FONT_FAMILY_NAME);
            builder.withHtmlContent(html, baseUri);
            builder.toStream(os);
            builder.run();
            log.debug("PDF generation complete ({} bytes)", os.size());
            return os.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF", e);
            throw new UncheckedIOException("Failed to generate PDF", e);
        }
    }

    private File extractFontToTempFile() {
        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream(FONT_RESOURCE_PATH)) {
            if (fontStream == null) {
                throw new IllegalStateException("Font file not found on classpath: " + FONT_RESOURCE_PATH);
            }
            var tempFile = Files.createTempFile("DejaVuSans", ".ttf").toFile();
            tempFile.deleteOnExit(); // acceptable for CLI/batch — leaks in long-running servers
            Files.copy(fontStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (IOException e) {
            log.error("Failed to extract font file", e);
            throw new UncheckedIOException("Failed to extract font file", e);
        }
    }
}
