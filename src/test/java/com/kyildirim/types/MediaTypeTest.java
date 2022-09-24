package com.kyildirim.types;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import lombok.extern.java.Log;

@Log
class MediaTypeTest {
    File tmp;

    @Test
    void test_checkFileType_truePos() {
        try {
            tmp = File.createTempFile("test", ".mp4");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot create tmp file\n{}", e);
        }
        assertDoesNotThrow(() -> MediaType.MP4.checkFileType(tmp));
    }
    @Test
    void test_checkFileType_falseNeg() {
        try {
            tmp = File.createTempFile("test", ".flv");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot create tmp file\n{}", e);
        }
        assertThrows(IllegalStateException.class, () -> MediaType.MP4.checkFileType(tmp));
    }

    @Test
    void test_fileNoEx() {
        try {
            tmp = File.createTempFile("test", "test");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Cannot create tmp file\n{}", e);
        }
        assertThrows(IllegalArgumentException.class, () -> MediaType.MP4.checkFileType(tmp));
    }
}
