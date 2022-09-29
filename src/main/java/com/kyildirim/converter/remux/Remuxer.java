package com.kyildirim.converter.remux;

import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_parameters_copy;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

import java.io.File;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Remuxer {

    @Setter
    private File in;
    @Getter
    private File out;

    private AVInputFormat inputFormat = new AVInputFormat(null);
    private AVFormatContext inputFormatContext = new AVFormatContext(null);
    private AVFormatContext outputFormatContext = new AVFormatContext(null);
    private AVDictionary inputDict = new AVDictionary(null);

    public Remuxer(File in, File out) {
        this.in = in;
        this.out = out;
    }

    private void openMediaFile() {
        if (avformat_open_input(inputFormatContext, in.getAbsolutePath(), inputFormat, inputDict) < 0) {
            log.error("Could not open input file {}", in.getAbsolutePath());
            return;
        }
        av_dict_free(inputDict);
    }

    public void remux() {
        AVOutputFormat outputFormat;
        AVPacket packet = new AVPacket();
        int[] streamMapping;
        int streamIndex = 0;
        int streamMappingSize = 0;
        openMediaFile();
        if (avformat_find_stream_info(inputFormatContext, (PointerPointer) null) < 0) {
            throw new IllegalStateException("avformat_find_stream_info() error:\tFailed to retrieve input stream information");
        }
        // TODO: Why its printing to stderr?
        av_dump_format(inputFormatContext, 0, in.getAbsolutePath(), 0);
        streamMappingSize = inputFormatContext.nb_streams();
        streamMapping = new int[streamMappingSize];

        // TODO: Can get the format name from MediaType enum and give it in here.
        if (avformat_alloc_output_context2(outputFormatContext, null, null, out.getAbsolutePath()) < 0) {
            throw new IllegalStateException("avformat_alloc_output_context2() error:\tCould not create output context\n");
        }
        outputFormat = outputFormatContext.oformat();

        for (int streamId = 0; streamId < streamMappingSize; streamId++) {
            AVStream outStream;
            AVStream inStream = inputFormatContext.streams(streamId);

            AVCodecParameters inputCodecParams = inStream.codecpar();

            // If the current stream is not Audio, Video or Subtitle, mark that streamId with -1.
            if (inputCodecParams.codec_type() != AVMEDIA_TYPE_AUDIO
                    && inputCodecParams.codec_type() != AVMEDIA_TYPE_VIDEO
                    && inputCodecParams.codec_type() != AVMEDIA_TYPE_SUBTITLE) {
                streamMapping[streamId] = -1;
                continue;
            }

            streamMapping[streamId] = streamIndex++;

            outStream = avformat_new_stream(outputFormatContext, null);

            if (avcodec_parameters_copy(outStream.codecpar(), inputCodecParams) < 0) {
                throw new IllegalStateException("Failed to copy codec parameters");
            }
            outStream.codecpar().codec_tag(0);
        }
        // TODO: Why its printing to stderr?
        av_dump_format(outputFormatContext, 0, out.getAbsolutePath(), 1);

        if ((outputFormat.flags() & AVFMT_NOFILE) == 0) {
            AVIOContext pb = new AVIOContext(null);
            if (avio_open(pb, out.getAbsolutePath(), AVIO_FLAG_WRITE) < 0) {
                throw new IllegalStateException("avio_open() error:\tCould not open output file " + out.getAbsolutePath());
            }
            outputFormatContext.pb(pb);
        }
        AVDictionary outputDict = new AVDictionary(null);
        if (avformat_write_header(outputFormatContext, outputDict) < 0) {
            log.error("Error occurred while avformat_write_header() for output file {}", out.getAbsolutePath());
        }
        // "packet" is the next frame of a stream.
        while (av_read_frame(inputFormatContext, packet) >= 0) { // while end of file is reached
            AVStream inStream;
            AVStream outStream;

            inStream = inputFormatContext.streams(packet.stream_index());
            if (packet.stream_index() >= streamMappingSize ||
                    streamMapping[packet.stream_index()] < 0) {
                av_packet_unref(packet);
                continue;
            }

            packet.stream_index(streamMapping[packet.stream_index()]);
            outStream = outputFormatContext.streams(packet.stream_index());

            packet.pts(av_rescale_q_rnd(packet.pts(), inStream.time_base(), outStream.time_base(),
                    AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            packet.dts(av_rescale_q_rnd(packet.dts(), inStream.time_base(), outStream.time_base(),
                    AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
            packet.duration(av_rescale_q(packet.duration(), inStream.time_base(), outStream.time_base()));
            packet.pos(-1);

            synchronized (outputFormatContext) {
                if (av_interleaved_write_frame(outputFormatContext, packet) < 0) {
                    throw new IllegalStateException("av_write_frame() error:\tWhile muxing packet\n");
                }
            }
            av_packet_unref(packet);
        }
        if (av_write_trailer(outputFormatContext) < 0) {
            throw new IllegalStateException("Cannot write to: " + out.getAbsolutePath());
        }
        avformat_close_input(inputFormatContext);

        if (!outputFormatContext.isNull() && (outputFormat.flags() & AVFMT_NOFILE) == 0) {
            avio_closep(outputFormatContext.pb());
        }

        avformat_free_context(outputFormatContext);
    }

}
