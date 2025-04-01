package cn.treedeep.link.protocol.v1.device.client;

import cn.treedeep.link.core.protocol.v1.BaseFrame;
import cn.treedeep.link.core.util.CRC;
import cn.treedeep.link.core.util.HexUtil;
import cn.treedeep.link.protocol.v1.device.protocol.V1;
import cn.treedeep.link.protocol.v1.device.protocol.model.report.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

import static cn.treedeep.link.core.protocol.v1.Protocol.END_FLAG;
import static cn.treedeep.link.core.protocol.v1.Protocol.START_FLAG;

@Slf4j
public class DeviceFrameEncoder extends MessageToByteEncoder<BaseFrame> {
    @Override
    protected void encode(ChannelHandlerContext ctx, BaseFrame msg, ByteBuf out) throws Exception {
        // 创建临时缓冲区计算长度
        ByteBuf dataBuf = ctx.alloc().buffer();
        try {
            // 写入协议头
            dataBuf.writeShort(START_FLAG);
            dataBuf.writeByte(msg.getVersion());

            // 占位协议总长度（后面计算）
            dataBuf.writeShort(0);

            dataBuf.writeByte(msg.getCommand());

            // 写入数据域
            writeDataContent(msg, dataBuf);

            // 计算总长度并回填
            int totalLength = dataBuf.readableBytes() + 4; // +4是CRC和结束符的长度
            dataBuf.setShort(3, totalLength); // 设置协议总长度

            // 计算CRC (从起始符到数据域结束)
            byte[] crcData = new byte[dataBuf.readableBytes()];
            dataBuf.getBytes(0, crcData);
            long ccittCrc = CRC.calculateCRC(CRC.Parameters.CCITT, crcData);

            log.debug("设备：CRC计算范围：{}", HexUtil.formatHexString(ByteBuffer.wrap(crcData)));

            // 写入CRC
            dataBuf.writeShort((short) ccittCrc);

            // 写入结束符
            dataBuf.writeShort(END_FLAG);

            // 打印完整帧日志
            log.debug("设备：发送帧:【设备ID：{}, 会话ID：{}, 指令类型：0x{}, 总长度：{} 字节】", msg.getDeviceId(), msg.getSessionId(), String.format("%02X", msg.getCommand()), totalLength);
            log.debug("设备：{}", HexUtil.formatHexString(dataBuf));

            // 写入输出缓冲区
            out.writeBytes(dataBuf);
        } finally {
            dataBuf.release();
        }
    }

    private void writeDataContent(BaseFrame frame, ByteBuf buf) {
        // 写入基础字段
        buf.writeInt(frame.getDeviceId());      // 设备ID(4B)
        buf.writeShort(frame.getSessionId());   // 会话ID(2B)
        buf.writeInt(frame.getTaskId());        // 任务ID(4B)

        switch (frame.getCommand()) {
            case V1.REPORT_DEVICE_CONNECTION_REQUEST:
                var connectionRequest = (ReportDeviceConnectionRequest) frame;
                break;
            case V1.REPORT_DEVICE_BIND_RESPONSE:
                var bindResponse = (ReportDeviceBindResponse) frame;
                buf.writeShort(bindResponse.getStatus());
                break;
            case V1.REPORT_START_RECORDING_RESPONSE:
                var startRecordingResponse = (ReportStartRecordingResponse) frame;
                buf.writeShort(startRecordingResponse.getStatus());
                break;
            case V1.REPORT_STOP_RECORDING_RESPONSE:
                var stopRecordingResponse = (ReportStopRecordingResponse) frame;
                buf.writeShort(stopRecordingResponse.getStatus());
                break;
            case V1.REPORT_HEARTBEAT_PACKET:
                var heartbeatPacket = (ReportHeartbeatPacket) frame;
                buf.writeByte(heartbeatPacket.getBattery());
                buf.writeByte(heartbeatPacket.getStatus());
                break;
            case V1.REPORT_KEYFRAME_MARK:
                var keyframeMark = (ReportKeyframeMark) frame;
                buf.writeInt(keyframeMark.getFrameSeq());
                buf.writeLong(keyframeMark.getTimestamp());
                break;
            case V1.REPORT_FILE_FRAME_UPLOAD:
                var fileFrameUpload = (ReportFileFrameUpload) frame;
                buf.writeInt(fileFrameUpload.getFrameSeq());
                // buf.writeBytes(fileFrameUpload.getFrameData());

                // 直接写入ByteBuf数据
                ByteBuf frameData = fileFrameUpload.getFrameData();
                if (frameData != null) {
                    // 保存当前读索引
                    int readerIndex = frameData.readerIndex();
                    // 写入数据
                    buf.writeBytes(frameData, frameData.readerIndex(), frameData.readableBytes());
                    // 恢复读索引，确保数据可以被多次读取
                    frameData.readerIndex(readerIndex);
                }

                break;
            case V1.REPORT_FILE_UPLOAD_END:
                var fileUploadEnd = (ReportFileUploadEnd) frame;
                buf.writeInt(fileUploadEnd.getTotalFrames());
                break;
            default:
                log.warn("设备：未支持的命令类型: 0x{}", Integer.toHexString(frame.getCommand() & 0xFF).toUpperCase());
        }
    }
}
