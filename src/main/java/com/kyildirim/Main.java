package com.kyildirim;

import java.io.File;

import com.kyildirim.converter.MediaConverter;
import com.kyildirim.types.MediaType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            log.warn("Missing arguments. Provide input flv file.\nexample: java -jar mediaconverter.jar myfile.flv");
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
        log.info("{} is converted to mp4 and saved to: {}", in.getAbsolutePath(), mCon.getOut().getAbsolutePath());
    }
}
