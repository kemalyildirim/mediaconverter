package com.kyildirim.converter;

import java.io.File;

import com.kyildirim.converter.remux.Remuxer;
import com.kyildirim.converter.remux.exception.UnsupportedCodecException;
import com.kyildirim.types.MediaType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
public class MediaConverter {

    @Getter private final MediaType inType;
    @Getter private final MediaType outType;
    @Setter @Getter private File in;
    @Setter @Getter private File out;

    public void convert() {
        log.trace("convert() called.");
        inType.checkFileType(in);
        out = new File(createOutputFilename(outType.getExtension()));
        log.debug("Converting the file {} from {} to {} and saving converted file at {}", in, inType, outType, out.getAbsolutePath());
        Remuxer remuxer = new Remuxer(this);
        try {
            remuxer.remux();
            log.info("{} is converted to {} and saved at: {}", in.getAbsolutePath(), outType, out.getAbsolutePath());
        } catch (UnsupportedCodecException e) {
            log.error("Remuxing failed.", e);
        }
    }

    private String createOutputFilename(String newExtension) {
        String inExtension = inType.getExtensionString(in);
        return in.getAbsolutePath().replace(inExtension, newExtension);
    }

}
