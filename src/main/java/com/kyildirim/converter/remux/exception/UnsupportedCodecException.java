package com.kyildirim.converter.remux.exception;

public class UnsupportedCodecException extends Exception {
    public UnsupportedCodecException(String message) {
        super(message);
    }
    public UnsupportedCodecException(String message, Throwable err) {
        super(message, err);
    }
}
