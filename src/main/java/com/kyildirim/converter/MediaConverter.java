package com.kyildirim.converter;

import java.io.File;

import com.kyildirim.converter.remux.Remuxer;
import com.kyildirim.types.MediaType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.bytedeco.javacpp.*;
import org.bytedeco.ffmpeg.avcodec.*;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.*;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

@RequiredArgsConstructor
@Log4j2
public class MediaConverter {

    private final MediaType inType;
    private final MediaType oType;
    @Setter private File in;
    @Getter private File out;

    public void convert() {
        log.trace("convert() called.");
        log.trace("from type {} to type {} the file {}", inType, oType, in.getAbsolutePath());
        inType.checkFileType(in);
        out = new File(createOutputFile("mp4"));
        Remuxer remuxer = new Remuxer(in, out);
        remuxer.remux();
    }

    private String createOutputFile(String newExtension) {
        String inExtension = inType.getExtensionString(in);
        return in.getAbsolutePath().replace(inExtension, newExtension);
    }

}
