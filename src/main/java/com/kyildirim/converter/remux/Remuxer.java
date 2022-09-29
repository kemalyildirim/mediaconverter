package com.kyildirim.converter.remux;

import com.kyildirim.converter.remux.exception.UnsupportedCodecException;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.*;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.PointerPointer;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

import java.io.File;
import java.util.Arrays;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Remuxer {

    @Setter
    private File in;
    @Getter
    private File out;

    //src: https://github.com/ant-media/Ant-Media-Server/blob/master/src/main/java/io/antmedia/muxer/Mp4Muxer.java#L77
    private static int[] MP4_SUPPORTED_CODECS = {
            AV_CODEC_ID_MOV_TEXT     ,
            AV_CODEC_ID_MPEG4        ,
            AV_CODEC_ID_H264         ,
            AV_CODEC_ID_HEVC         ,
            AV_CODEC_ID_AAC          ,
            AV_CODEC_ID_MP4ALS       , /* 14496-3 ALS */
            AV_CODEC_ID_MPEG2VIDEO  , /* MPEG-2 Main */
            AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 Simple */
            AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 SNR */
            AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 Spatial */
            AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 High */
            AV_CODEC_ID_MPEG2VIDEO   , /* MPEG-2 422 */
            AV_CODEC_ID_AAC          , /* MPEG-2 AAC Main */
            AV_CODEC_ID_MP3          , /* 13818-3 */
            AV_CODEC_ID_MP2          , /* 11172-3 */
            AV_CODEC_ID_MPEG1VIDEO   , /* 11172-2 */
            AV_CODEC_ID_MP3          , /* 11172-3 */
            AV_CODEC_ID_MJPEG        , /* 10918-1 */
            AV_CODEC_ID_PNG          ,
            AV_CODEC_ID_JPEG2000     , /* 15444-1 */
            AV_CODEC_ID_VC1          ,
            AV_CODEC_ID_DIRAC        ,
            AV_CODEC_ID_AC3          ,
            AV_CODEC_ID_EAC3         ,
            AV_CODEC_ID_DTS          , /* mp4ra.org */
            AV_CODEC_ID_VP9          , /* nonstandard, update when there is a standard value */
            AV_CODEC_ID_TSCC2        , /* nonstandard, camtasia uses it */
            AV_CODEC_ID_VORBIS       , /* nonstandard, gpac uses it */
            AV_CODEC_ID_DVD_SUBTITLE , /* nonstandard, see unsupported-embedded-subs-2.mp4 */
            AV_CODEC_ID_QCELP        ,
            AV_CODEC_ID_MPEG4SYSTEMS ,
            AV_CODEC_ID_MPEG4SYSTEMS ,
            AV_CODEC_ID_NONE
    };

    // src: https://trac.ffmpeg.org/wiki/SupportedMediaTypesInFormats
    private static int[] FLV_SUPPORTED_CODECS = {
            // video
            AV_CODEC_ID_FLV1,
            AV_CODEC_ID_H263,
            AV_CODEC_ID_MPEG4,
            AV_CODEC_ID_FLASHSV,
            AV_CODEC_ID_FLASHSV2,
            AV_CODEC_ID_VP6,
            AV_CODEC_ID_VP6A,
            AV_CODEC_ID_H264,
            // audio
            AV_CODEC_ID_MP3,
            AV_CODEC_ID_PCM_U8,
            AV_CODEC_ID_PCM_S16BE,
            AV_CODEC_ID_PCM_S16LE,
            AV_CODEC_ID_ADPCM_SWF,
            AV_CODEC_ID_AAC,
            AV_CODEC_ID_NELLYMOSER,
            AV_CODEC_ID_SPEEX
    };
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

    public void remux() throws UnsupportedCodecException {
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
            log.trace("Current codec type: {} Current codec name: {}", () -> avutil.av_get_media_type_string(inputCodecParams.codec_type()).getString(),
                    () -> avcodec.avcodec_get_name(inputCodecParams.codec_id()).getString());
            checkCodecCompatibility(inputCodecParams);
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

    private static void checkCodecCompatibility(AVCodecParameters inputCodecParams) throws UnsupportedCodecException {
        if (Arrays.stream(MP4_SUPPORTED_CODECS).noneMatch(codecId -> codecId == inputCodecParams.codec_id())) {
            throw new UnsupportedCodecException("Cannot re-mux the stream with codec " + avcodec.avcodec_get_name(inputCodecParams.codec_id()).getString());
        }
    }

}
