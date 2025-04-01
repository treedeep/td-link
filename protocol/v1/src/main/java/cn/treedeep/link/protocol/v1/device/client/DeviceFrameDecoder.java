package cn.treedeep.link.protocol.v1.device.client;

import cn.treedeep.link.core.protocol.v1.BaseFrame;
import cn.treedeep.link.core.util.CRC;
import cn.treedeep.link.core.util.HexUtil;
import cn.treedeep.link.protocol.v1.device.protocol.V1;
import cn.treedeep.link.protocol.v1.device.protocol.model.command.*;
import cn.treedeep.link.protocol.v1.device.protocol.model.response.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.List;

import static cn.treedeep.link.core.protocol.v1.Protocol.END_FLAG;
import static cn.treedeep.link.core.protocol.v1.Protocol.START_FLAG;

@Slf4j
public class DeviceFrameDecoder extends ByteToMessageDecoder {
    private static final int HEADER_SIZE = 2 + 1 + 2 + 1;   // 起始符(2B)+版本(1B)+长度(2B)+指令类型(1B)
    private static final int TAIL_SIZE = 2 + 2;             // CRC16(2B)+结束符(2B)

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        log.debug("设备：{}", HexUtil.formatHexString(in));

        while (in.readableBytes() >= HEADER_SIZE + TAIL_SIZE) {
            // 标记当前读指针位置
            in.markReaderIndex();

            // 查找起始符
            boolean foundStartFlag = false;
            while (in.readableBytes() >= 2) {
                int readerIndex = in.readerIndex();
                short possibleStartFlag = in.getShort(readerIndex);
                if (possibleStartFlag == START_FLAG) {
                    foundStartFlag = true;
                    break;
                }
                in.skipBytes(1);
            }

            if (!foundStartFlag || in.readableBytes() < HEADER_SIZE) {
                return;
            }

            // 读取起始符
            short startFlag = in.readShort();

            // 读取协议版本
            byte version = in.readByte();

            // 读取协议总长度
            int totalLength = in.readShort() & 0xFFFF;

            // 基本长度检查
            if (totalLength < HEADER_SIZE + TAIL_SIZE) {
                log.error("设备：无效的帧长度: {}", totalLength);

                // 清空整个ByteBuf
                in.clear();
                // 发送错误响应
                ctx.channel().writeAndFlush(new RespFrameError());
                return;
            }

            // 检查是否有足够的数据进行解析
            if (in.readableBytes() < totalLength - 6) { // 6 = 起始符(2) + 版本(1) + 长度(2) + 已读取的部分
                in.resetReaderIndex();
                return;
            }

            // 读取指令
            byte command = in.readByte();

            // 读取数据域
            int dataLength = totalLength - HEADER_SIZE - TAIL_SIZE;
            ByteBuf data = in.readSlice(dataLength).copy();

            // 读取CRC校验
            long crcReceived = in.readShort() & 0xFFFFL;

            // 读取结束符
            short endFlag = in.readShort();
            if (endFlag != END_FLAG) {
                log.error("设备：无效的结束符: 0x{}", Integer.toHexString(endFlag).toUpperCase());

                // 清空整个ByteBuf
                in.clear();
                // 发送错误响应
                ctx.channel().writeAndFlush(new RespFrameError());
                data.release(); // 释放data资源
                return;
            }

            // 计算CRC校验
            byte[] crcData = new byte[totalLength - TAIL_SIZE];
            in.resetReaderIndex();
            in.readBytes(crcData, 0, totalLength - TAIL_SIZE);
            log.debug("设备：CRC计算范围：{}", HexUtil.formatHexString(ByteBuffer.wrap(crcData)));

            long ccittCrc = CRC.calculateCRC(CRC.Parameters.CCITT, crcData);

            if (crcReceived != ccittCrc) {
                log.error("设备：CRC校验失败! 接收: 0x{} ≠ 计算: 0x{}",
                        Long.toHexString(crcReceived).toUpperCase(),
                        Long.toHexString(ccittCrc).toUpperCase());

                // 清空整个ByteBuf
                in.clear();
                // 发送错误响应
                ctx.channel().writeAndFlush(new RespFrameError());
                return;
            }

            // 移动读指针到帧结束位置，确保完整读取一帧
            in.resetReaderIndex();
            in.skipBytes(totalLength);

            // 打印详细的日志
            log.info("设备：帧信息:【总长度：{} 字节, 版本号：{}, 指令类型：0x{}】", totalLength, version, Integer.toHexString(command & 0xFF).toUpperCase());
            log.debug("设备：数据：{}", HexUtil.formatHexString(data));

            // 解析协议对象
            try {
                int extLength = totalLength - (HEADER_SIZE + TAIL_SIZE + 10);
                BaseFrame frame = parseFrame(command, data, extLength);
                out.add(frame);
            } catch (Exception e) {
                log.error("设备：解析帧异常", e);
            } finally {
                data.release(); // 确保在所有情况下都释放data资源
            }
        }
    }

    private BaseFrame parseFrame(byte command, ByteBuf data, int extLength) {
        // 读取基础字段
        int deviceId = data.readInt();
        short sessionId = data.readShort();
        int taskId = data.readInt();

        // 读取扩展数据
        ByteBuf extData = null;
        BaseFrame frame = null;

        try {
            if (extLength > 0) {
                extData = data.readSlice(extLength).copy();
            }

            // 根据指令类型构建协议对象
            switch (command) {
                case V1.RESP_DEVICE_CONNECTION:
                    frame = new RespDeviceConnection(extData != null ? extData.readLong() : 0);
                    break;
                case V1.RESP_HEARTBEAT:
                    frame = new RespHeartbeat(System.currentTimeMillis());
                    break;
                case V1.RESP_KEYFRAME_MARK:
                    if (extData != null) {
                        frame = new RespKeyframeMark(extData.readInt(), extData.readByte());
                    } else {
                        frame = new RespKeyframeMark(0, (byte) 0);
                    }
                    break;
                case V1.RESP_FILE_FRAME_UPLOAD:
                    if (extData != null) {
                        frame = new RespFileFrameUpload(extData.readInt(), extData.readByte());
                    } else {
                        frame = new RespFileFrameUpload(0, (byte) 0);
                    }
                    break;
                case V1.RESP_FILE_UPLOAD_END:
                    if (extData != null) {
                        int frameSeq = extData.readInt();
                        int dataLength = extData.readableBytes();
                        byte[] frameData = new byte[dataLength];
                        extData.readBytes(frameData);
                        frame = new RespFileUploadEnd(frameSeq, frameData);
                    } else {
                        frame = new RespFileUploadEnd(0, new byte[0]);
                    }
                    break;
                case V1.CMD_DEVICE_BIND:
                    frame = new CmdDeviceBind();
                    break;
                case V1.CMD_DEVICE_UNBIND:
                    frame = new CmdDeviceUnBind();
                    break;
                case V1.CMD_START_RECORDING:
                    frame = new CmdStartRecording();
                    break;
                case V1.CMD_STOP_RECORDING:
                    frame = new CmdStopRecording();
                    break;
                case V1.CMD_HEARTBEAT:
                    frame = new CmdHeartbeat();
                    break;
                case V1.CMD_FORCE_DISCONNECT:
                    frame = new CmdForceDisconnect();
                    break;
                case V1.RESP_FRAME_EXCEPTION:
                    log.warn("设备：收到异常帧");
                    frame = new RespFrameError();
                    break;
                default:
                    log.warn("设备：未支持的命令类型: 0x{}", Integer.toHexString(command & 0xFF).toUpperCase());
            }

            // 设置基础字段
            if (frame != null) {
                frame.setDeviceId(deviceId);
                frame.setSessionId(sessionId);
                frame.setTaskId(taskId);
            }

            return frame;
        } finally {
            // 确保释放extData
            if (extData != null) {
                extData.release();
            }
        }
    }
}
