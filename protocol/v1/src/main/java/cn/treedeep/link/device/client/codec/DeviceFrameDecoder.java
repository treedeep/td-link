package cn.treedeep.link.device.client.codec;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.device.protocol.model.command.*;
import cn.treedeep.link.device.protocol.model.response.*;
import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.protocol.v1.BaseFrameDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeviceFrameDecoder extends BaseFrameDecoder {

    @Override
    protected BaseFrame parseByCommand(byte command, ByteBuf data) {

        // 根据指令类型构建协议对象
        switch (command) {
            case V1.RESP_DEVICE_CONNECTION:
                return new RespDeviceConnection(data.readLong());

            case V1.RESP_HEARTBEAT:
                return new RespHeartbeat(System.currentTimeMillis());

            case V1.RESP_KEYFRAME_MARK:
                return new RespKeyframeMark(data.readInt(), data.readByte());

            case V1.RESP_FILE_FRAME_UPLOAD:
                return new RespFileFrameUpload(data.readInt(), data.readByte());

            case V1.RESP_FILE_UPLOAD_END:
                int frameSeq = data.readInt();
                int dataLength = data.readableBytes();
                byte[] frameData = new byte[dataLength];
                data.readBytes(frameData);
                return new RespFileUploadEnd(frameSeq, frameData);

            case V1.CMD_DEVICE_BIND:
                return new CmdDeviceBind();

            case V1.CMD_DEVICE_UNBIND:
                return new CmdDeviceUnBind();

            case V1.CMD_START_RECORDING:
                return new CmdStartRecording();

            case V1.CMD_STOP_RECORDING:
                return new CmdStopRecording();

            case V1.CMD_HEARTBEAT:
                return new CmdHeartbeat();

            case V1.CMD_FORCE_DISCONNECT:
                return new CmdForceDisconnect();

            case V1.RESP_FRAME_EXCEPTION:
                log.warn("模拟器 => 收到异常帧");
                return new RespFrameError();

            default:
                log.warn("模拟器 => 未支持的命令类型: 0x{}", Integer.toHexString(command & 0xFF));
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
