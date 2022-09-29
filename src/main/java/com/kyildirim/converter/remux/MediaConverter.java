package com.kyildirim.converter.remux;

import java.io.File;

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

    final MediaType inType;
    final MediaType oType;
    @Setter File in;
    @Getter File out;

    public void convert() {
        inType.checkFileType(in);
        out = new File(createOutputFile("mp4"));
        String inAbsolute = in.getAbsolutePath();
        String outAbsolute = out.getAbsolutePath();
        AVOutputFormat ofmt = null;
        AVFormatContext ifmt_ctx = new AVFormatContext(null);
        AVFormatContext ofmt_ctx = new AVFormatContext(null);
        AVPacket pkt = new AVPacket();
        int ret;
        int[] stream_mapping;
        int stream_index = 0;
        int stream_mapping_size = 0;
        AVInputFormat avInputFormat = new AVInputFormat(null);
        AVDictionary avDictionary = new AVDictionary(null);
        if ((ret = avformat_open_input(ifmt_ctx, inAbsolute, avInputFormat, avDictionary)) < 0) {
            log.error("Could not open input file {}", inAbsolute);
        }
        av_dict_free(avDictionary);

        // Read packets of a media file to get stream information
        if ((ret = avformat_find_stream_info(ifmt_ctx, (PointerPointer) null)) < 0) {
            throw new IllegalStateException("avformat_find_stream_info() error:\tFailed to retrieve input stream information");
        }
        av_dump_format(ifmt_ctx, 0, inAbsolute, 0);

        if ((ret = avformat_alloc_output_context2(ofmt_ctx, null, null, outAbsolute)) < 0) {
            throw new IllegalStateException("avformat_alloc_output_context2() error:\tCould not create output context\n");
        }
        stream_mapping_size = ifmt_ctx.nb_streams();
        stream_mapping = new int[stream_mapping_size];

        ofmt = ofmt_ctx.oformat();

        for (int stream_idx = 0; stream_idx < stream_mapping_size; stream_idx++) {
            AVStream out_stream;
            AVStream in_stream = ifmt_ctx.streams(stream_idx);

            AVCodecParameters in_codedpar = in_stream.codecpar();

            if (in_codedpar.codec_type() != AVMEDIA_TYPE_AUDIO &&
                    in_codedpar.codec_type() != AVMEDIA_TYPE_VIDEO &&
                    in_codedpar.codec_type() != AVMEDIA_TYPE_SUBTITLE) {
                stream_mapping[stream_idx] = -1;
                continue;
            }

            stream_mapping[stream_idx] = stream_index++;

            out_stream = avformat_new_stream(ofmt_ctx, null);

            ret = avcodec_parameters_copy(out_stream.codecpar(), in_codedpar);
            if (ret < 0) {
                log.error("Failed to copy codec parameters");
            }
            out_stream.codecpar().codec_tag(0);
        }
        av_dump_format(ofmt_ctx, 0, outAbsolute, 1);

        if ((ofmt.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext pb = new AVIOContext(null);
            ret = avio_open(pb, outAbsolute, AVIO_FLAG_WRITE);
            if (ret < 0) {
                throw new IllegalStateException("avio_open() error:\tCould not open output file '%s'" + outAbsolute);
            }
            ofmt_ctx.pb(pb);
        }
        AVDictionary avOutDict = new AVDictionary(null);
        ret = avformat_write_header(ofmt_ctx, avOutDict);
        if (ret < 0) {
            log.error("Error occurred when opening output file");
        }
        while (true) {
            AVStream in_stream, out_stream;
            // Return the next frame of a stream.
            if ((ret = av_read_frame(ifmt_ctx, pkt)) < 0) {
                break;
            }

            in_stream = ifmt_ctx.streams(pkt.stream_index());
            if (pkt.stream_index() >= stream_mapping_size ||
                    stream_mapping[pkt.stream_index()] < 0) {
                av_packet_unref(pkt);
                continue;
            }

            pkt.stream_index(stream_mapping[pkt.stream_index()]);
            out_stream = ofmt_ctx.streams(pkt.stream_index());
            // log_packet

            pkt.pts(av_rescale_q_rnd(pkt.pts(), in_stream.time_base(), out_stream.time_base(),
                    AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), out_stream.time_base(),
                    AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            pkt.duration(av_rescale_q(pkt.duration(), in_stream.time_base(), out_stream.time_base()));
            pkt.pos(-1);

            synchronized (ofmt_ctx) {
                ret = av_interleaved_write_frame(ofmt_ctx, pkt);
                if (ret < 0) {
                    throw new IllegalStateException("av_write_frame() error:\tWhile muxing packet\n");
                }
            }

            av_packet_unref(pkt);

        }
        av_write_trailer(ofmt_ctx);

        avformat_close_input(ifmt_ctx);

        if (!ofmt_ctx.isNull() && (ofmt.flags() & AVFMT_NOFILE) == 0) {
            avio_closep(ofmt_ctx.pb());
        }

        avformat_free_context(ofmt_ctx);

    }

    private String createOutputFile(String newExtension) {
        String inExtension = inType.getExtensionString(in);
        return in.getAbsolutePath().replace(inExtension, newExtension);
    }

    public void save() {
        log.info("Save called.");
    }

}
