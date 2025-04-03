package cn.treedeep.link.device.protocol.codec;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.device.protocol.model.response.*;
import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.protocol.v1.BaseFrameEncoder;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FrameEncoder extends BaseFrameEncoder {

    @Override
    protected void writePayload(ByteBuf buf, BaseFrame frame) {

        switch (frame.getCommand()) {
            case V1.CMD_DEVICE_BIND:
                log.debug("设备绑定下发");
                break;

            case V1.CMD_START_RECORDING:
                log.warn("开始录制");
                break;

            case V1.CMD_STOP_RECORDING:
                log.warn("停止录制");
                break;

            case V1.CMD_HEARTBEAT:
                log.warn("心跳检测");
                break;

            case V1.CMD_FORCE_DISCONNECT:
                log.warn("强制设备「{}」下线", frame.getDeviceId());
                break;

            case V1.RESP_DEVICE_CONNECTION:
                log.debug("设备连接响应");
                RespDeviceConnection connResp = (RespDeviceConnection) frame;
                buf.writeLong(connResp.getServerTimestamp());    // 服务器时间戳(8B)
                break;

            case V1.RESP_HEARTBEAT:
                log.debug("心跳响应");
                RespHeartbeat heartbeat = (RespHeartbeat) frame;
                buf.writeLong(heartbeat.getServerTimestamp());   // 服务器时间戳(8B)
                break;

            case V1.RESP_KEYFRAME_MARK:
                log.debug("关键帧标记响应");
                RespKeyframeMark keyframe = (RespKeyframeMark) frame;
                buf.writeInt(keyframe.getFrameSeq());           // 帧序号(4B)
                buf.writeByte(keyframe.getAckStatus());         // 确认状态(1B)
                break;

            case V1.RESP_FILE_FRAME_UPLOAD:
                log.debug("文件帧上传响应");
                RespFileFrameUpload videoFrame = (RespFileFrameUpload) frame;
                buf.writeInt(videoFrame.getFrameSeq());         // 帧序号(4B)
                buf.writeByte(videoFrame.getReceiveStatus());   // 接收状态(1B)
                break;

            case V1.RESP_FILE_UPLOAD_END:
                log.debug("文件上传结束响应");
                RespFileUploadEnd uploadEnd = (RespFileUploadEnd) frame;
                buf.writeInt(uploadEnd.getTotalFrames());       // 总帧数(4B)
                byte[] fileHash = uploadEnd.getFileHash();
                if (fileHash != null && fileHash.length > 0) {
                    buf.writeBytes(fileHash);                   // 文件哈希值
                }
                break;

            case V1.RESP_FRAME_EXCEPTION:
                log.warn("帧异常！");
                break;

            default:
                log.warn("未知指令类型：0x{}", Integer.toHexString(frame.getCommand() & 0xFF));
        }
    }

}