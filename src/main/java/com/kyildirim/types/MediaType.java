package com.kyildirim.types;

import java.io.File;

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

    public void checkFileType(File f) {
        int exStart = f.getName().lastIndexOf(".");
        if (exStart == -1) {
            throw new IllegalArgumentException("No file extension for file:" + f.getName());
        }
        String fileExt = f.getName().substring(exStart + 1).toLowerCase();
        boolean matches = this.extension.equalsIgnoreCase(fileExt);
        if (!matches) {
            throw new IllegalStateException("Input file is " + fileExt + " not " + this.extension);
        }
    }
}
