package com.example.print.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class PdfGenerator {

    public byte[] generatePdf(String html, String baseUri) {
        File tempFontFile = null;
        try {
            tempFontFile = extractFontToTempFile();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(tempFontFile, "DejaVuSans");
            builder.withHtmlContent(html, baseUri);
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF", e);
        } finally {
            if (tempFontFile != null && tempFontFile.exists()) {
                tempFontFile.delete();
            }
        }
    }

    private File extractFontToTempFile() throws IOException {
        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/DejaVuSans.ttf")) {
            if (fontStream == null) {
                throw new IOException("Font file not found on classpath: fonts/DejaVuSans.ttf");
            }
            File tempFile = Files.createTempFile("DejaVuSans", ".ttf").toFile();
            tempFile.deleteOnExit();
            Files.copy(fontStream, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }
}
