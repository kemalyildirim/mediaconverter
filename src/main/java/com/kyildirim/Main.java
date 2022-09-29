package com.kyildirim;

import java.io.File;

import com.kyildirim.converter.remux.MediaConverter;
import com.kyildirim.types.MediaType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            log.warn("Missing arguments. Usage\nmediaconverter <in>");
            System.exit(-1);
        }
        File in = new File(args[0]);
        log.debug("Input file is {}", in::getAbsolutePath);
        if (!in.isFile() || !in.canRead()) {
            log.error("Cannot read the input file. Check the file permissions and the file path.");
            System.exit(-1);
        }
        MediaConverter mCon = new MediaConverter(MediaType.FLV, MediaType.MP4);
        mCon.setIn(in);
        mCon.convert();
        log.info("{} flv file converted to mp4 file", in.getName());
        mCon.save();
        log.info("converted mp4 file is saved file to: {}", mCon.getOut().getAbsolutePath());
    }
}
