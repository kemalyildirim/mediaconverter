package com.kyildirim.converter.transmuxing;

import java.io.File;

import com.kyildirim.types.MediaType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class MediaConverter {

    final MediaType inType;
    final MediaType oType;
    @Setter File in;
    @Getter File out;

    public void convert() {
        inType.checkFileType(in);
    }

    public void save() {}

}
