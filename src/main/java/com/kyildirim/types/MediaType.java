package com.kyildirim.types;

public enum MediaType implements FileType {
    FLV("flv"),
    MP4("mp4");

    private String extension;

    MediaType(String extension) {
        this.extension = extension;
    }

    @Override
    public String getExtension() {
        return this.extension;
    }
}
