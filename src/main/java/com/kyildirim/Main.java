package com.kyildirim;

import java.io.File;
import java.util.logging.Level;

import com.kyildirim.converter.remux.MediaConverter;
import com.kyildirim.types.MediaType;

import lombok.extern.java.Log;

@Log
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            log.warning("Missing arguments. Usage\nmediaconverter <in>");
            System.exit(-1);
        }
        File in = new File(args[0]);
        if (!in.isFile() && !in.canRead()) {
            log.severe("Cannot read the input file. Check the file permissions and the file path.");
            System.exit(-1);
        }
        MediaConverter mCon = new MediaConverter(MediaType.FLV, MediaType.MP4);
        mCon.setIn(in);
        mCon.convert();
        log.log(Level.INFO, "{0} flv file converted to mp4 file", in.getName());
        mCon.save();
        log.log(Level.INFO, "converted mp4 file is saved file to: {0}", mCon.getOut().getAbsolutePath());
    }
}
