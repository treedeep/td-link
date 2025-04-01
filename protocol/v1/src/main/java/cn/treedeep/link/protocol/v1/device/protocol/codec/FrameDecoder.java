package cn.treedeep.link.protocol.v1.device.protocol.codec;

import cn.treedeep.link.core.protocol.v1.BaseFrame;
import cn.treedeep.link.core.util.CRC;
import cn.treedeep.link.core.util.HexUtil;
import cn.treedeep.link.protocol.v1.device.protocol.V1;
import cn.treedeep.link.protocol.v1.device.protocol.model.report.*;
import cn.treedeep.link.protocol.v1.device.protocol.model.response.RespFrameError;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.List;

import static cn.treedeep.link.core.protocol.v1.Protocol.END_FLAG;
import static cn.treedeep.link.core.protocol.v1.Protocol.START_FLAG;


/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>协议解码器（设备上传）</p>
 *
 * @author 周广明
 * @since 2025/3/29 18:56
 */
@Slf4j
public class FrameDecoder extends ByteToMessageDecoder {
    private static final int HEADER_SIZE = 2 + 1 + 2 + 1;   // 起始符(2B)+版本(1B)+长度(2B)+指令类型(1B)
    private static final int TAIL_SIZE = 2 + 2;             // CRC16(2B)+结束符(2B)

    private static final int DEFAULT_MAX_FRAME_SIZE = 2 * 1024 * 1024; // 默认最大帧大小2MB
    private final int maxFrameSize;

    public FrameDecoder() {
        this(DEFAULT_MAX_FRAME_SIZE);
    }

    public FrameDecoder(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        log.debug("接收：{}", HexUtil.formatHexString(in));

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
                log.error("无效的帧长度: {}", totalLength);

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

            // 读取指令类型
            byte cmdType = in.readByte();

            // 读取数据域
            int dataLength = totalLength - HEADER_SIZE - TAIL_SIZE;
            ByteBuf data = in.readSlice(dataLength).copy();

            // 读取CRC校验
            long crcReceived = in.readShort() & 0xFFFFL;

            // 读取结束符
            short endFlag = in.readShort();
            if (endFlag != END_FLAG) {
                log.error("无效的结束符: 0x{}", Integer.toHexString(endFlag).toUpperCase());

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
            log.debug("CRC计算范围：{}", HexUtil.formatHexString(ByteBuffer.wrap(crcData)));

            long ccittCrc = CRC.calculateCRC(CRC.Parameters.CCITT, crcData);

            if (crcReceived != ccittCrc) {
                log.error("CRC校验失败! 接收: 0x{} ≠ 计算: 0x{}",
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
            log.debug("帧信息:【总长度：{} 字节, 版本号：{}, 指令类型：0x{}】", totalLength, version, Integer.toHexString(cmdType & 0xFF).toUpperCase());
            log.debug("帧数据：{}", HexUtil.formatHexString(data));

            // 解析协议对象
            try {
                int extLength = totalLength - (HEADER_SIZE + TAIL_SIZE + 10);
                BaseFrame frame = parseFrame(cmdType, data, extLength);
                out.add(frame);
            } catch (Exception e) {
                log.error("解析帧异常", e);
            } finally {
                data.release(); // 确保在所有情况下都释放data资源
            }
        }
    }

    private BaseFrame parseFrame(byte cmdType, ByteBuf data, int extLength) {
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
            switch (cmdType) {
                case V1.REPORT_DEVICE_CONNECTION_REQUEST:
                    frame = new ReportDeviceConnectionRequest();
                    break;
                case V1.REPORT_DEVICE_BIND_RESPONSE:
                    frame = new ReportDeviceBindResponse(extData != null ? extData.readShort() : 0);
                    break;
                case V1.REPORT_START_RECORDING_RESPONSE:
                    frame = new ReportStartRecordingResponse(extData != null ? extData.readShort() : 0);
                    break;
                case V1.REPORT_STOP_RECORDING_RESPONSE:
                    frame = new ReportStopRecordingResponse(extData != null ? extData.readShort() : 0);
                    break;
                case V1.REPORT_HEARTBEAT_PACKET:
                    if (extData != null) {
                        frame = new ReportHeartbeatPacket(extData.readByte(), extData.readByte());
                    } else {
                        frame = new ReportHeartbeatPacket((byte) 0, (byte) 0);
                    }
                    break;
                case V1.REPORT_KEYFRAME_MARK:
                    if (extData != null) {
                        frame = new ReportKeyframeMark(extData.readInt(), extData.readLong());
                    } else {
                        frame = new ReportKeyframeMark(0, 0);
                    }
                    break;
                case V1.REPORT_FILE_FRAME_UPLOAD:
                    // 读取文件数据
                    if (extData != null) {
                        int frameSeq = extData.readInt();
                        int dataLength = extData.readableBytes();
                        byte[] frameData = new byte[dataLength];
                        extData.readBytes(frameData);
                        frame = new ReportFileFrameUpload(frameSeq, frameData);
                    } else {
                        frame = new ReportFileFrameUpload(0, new byte[0]);
                    }
                    break;
                case V1.REPORT_FILE_UPLOAD_END:
                    frame = new ReportFileUploadEnd(extData != null ? extData.readInt() : 0);
                    break;
                default:
                    log.warn("未支持的命令类型: 0x{}", Integer.toHexString(cmdType & 0xFF).toUpperCase());
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

