package cn.treedeep.link.device.protocol.codec;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.protocol.v1.BaseFrameDecoder;
import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.device.protocol.model.report.*;
import cn.treedeep.link.device.protocol.model.response.RespFrameError;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FrameDecoder extends BaseFrameDecoder {

    @Override
    protected BaseFrame parseByCommand(byte command, ByteBuf payload) {

        switch (command) {
            case V1.REPORT_DEVICE_CONNECTION_REQUEST:
                return new ReportDeviceConnectionRequest();

            case V1.REPORT_DEVICE_BIND_RESPONSE:
                return new ReportDeviceBindResponse(payload.readShort());

            case V1.REPORT_START_RECORDING_RESPONSE:
                return new ReportStartRecordingResponse(payload.readShort());

            case V1.REPORT_STOP_RECORDING_RESPONSE:
                return new ReportStopRecordingResponse(payload.readShort());

            case V1.REPORT_HEARTBEAT_PACKET:
                return new ReportHeartbeatPacket(payload.readByte(), payload.readByte());
            case V1.REPORT_KEYFRAME_MARK:
                return new ReportKeyframeMark(payload.readInt(), payload.readLong());

            case V1.REPORT_FILE_FRAME_UPLOAD:
                // 读取文件数据
                int frameSeq = payload.readInt();
                int dataLength = payload.readableBytes();
                byte[] frameData = new byte[dataLength];
                payload.readBytes(frameData);
                return new ReportFileFrameUpload(frameSeq, frameData);

            case V1.REPORT_FILE_UPLOAD_END:
                return new ReportFileUploadEnd(payload.readInt());

            default:
                log.warn("未支持的命令类型: 0x{}", Integer.toHexString(command & 0xFF));
                return null;
        }
    }

    @Override
    protected void sendErrorResponse(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new RespFrameError())
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("发送错误响应失败", future.cause());
                    }
                });
    }

}

