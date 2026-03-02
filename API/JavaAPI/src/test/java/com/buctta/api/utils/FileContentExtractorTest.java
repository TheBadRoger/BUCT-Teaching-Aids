package com.buctta.api.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileContentExtractorTest {

    @Test
    void testExtractDocx_Success() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("Sample DOCX content".getBytes());
        String content = FileContentExtractor.parseDocxByIS(inputStream);
        assertThat(content).isNotEmpty();
    }

    @Test
    void testExtractPdf_Success() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("Sample PDF content".getBytes());
        String content = FileContentExtractor.parsePdfByIS(inputStream);
        assertThat(content).isNotEmpty();
    }

    @Test
    void testExtract_UnsupportedFormat() {
        File unsupportedFile = new File("unsupported.txt");
        assertThrows(IOException.class, () -> FileContentExtractor.extract(unsupportedFile));
    }
}
