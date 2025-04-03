package cn.treedeep.link.device.client.codec;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.device.protocol.model.report.*;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import cn.treedeep.link.protocol.v1.Pv1FrameEncoder;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeviceFrameEncoder extends Pv1FrameEncoder {

    @Override
    protected void writePayload(ByteBuf buf, Pv1BaseFrame frame) {

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
                log.warn("模拟器 => 未支持的命令类型: 0x{}", Integer.toHexString(frame.getCommand() & 0xFF));
        }
    }
}
