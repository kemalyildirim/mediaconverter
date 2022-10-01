package com.kyildirim;

import java.io.File;

import com.kyildirim.converter.MediaConverter;
import com.kyildirim.types.MediaType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

    private static MediaType createInputMediaType(File f) {
        int exStart = f.getName().lastIndexOf(".");
        if (exStart == -1) {
            throw new IllegalArgumentException("No file extension for file:" + f.getName());
        }
        String extension = f.getName().substring(exStart + 1).toLowerCase();
        if (extension.equalsIgnoreCase("flv"))
                return MediaType.FLV;
        else // assuming there are only flv and mp4 types.
                return MediaType.MP4;
    }
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
        MediaConverter mCon;
        MediaType inType = createInputMediaType(in);
        if (inType.equals(MediaType.FLV)) {
            log.trace("from flv to mp4");
            mCon = new MediaConverter(MediaType.FLV, MediaType.MP4);
        } else { // assuming there are only flv and mp4 types.
            log.trace("from mp4 to flv");
            mCon = new MediaConverter(MediaType.MP4, MediaType.FLV);
        }
        mCon.setIn(in);
        mCon.convert();
    }
}
