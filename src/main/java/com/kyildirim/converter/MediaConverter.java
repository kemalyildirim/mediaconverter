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

    private final MediaType inType;
    private final MediaType outType;
    @Setter private File in;
    @Getter private File out;

    public void convert() {
        log.trace("convert() called.");
        log.trace("from type {} to type {} output to the file {}", inType, outType, in.getAbsolutePath());
        inType.checkFileType(in);
        out = new File(createOutputFile(outType.getExtension()));
        Remuxer remuxer = new Remuxer(in, out);
        try {
            remuxer.remux();
            log.info("{} is converted to mp4 and saved to: {}", in.getAbsolutePath(), out.getAbsolutePath());
        } catch (UnsupportedCodecException e) {
            log.error("Remuxing failed.", e);
        }
    }

    private String createOutputFile(String newExtension) {
        String inExtension = inType.getExtensionString(in);
        return in.getAbsolutePath().replace(inExtension, newExtension);
    }

}
