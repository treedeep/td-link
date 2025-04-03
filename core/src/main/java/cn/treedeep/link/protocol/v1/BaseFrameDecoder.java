package cn.treedeep.link.protocol.v1;

import cn.treedeep.link.util.CRC;
import cn.treedeep.link.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static cn.treedeep.link.protocol.v1.Protocol.END_FLAG;
import static cn.treedeep.link.protocol.v1.Protocol.START_FLAG;

/**
 * 协议解码器（设备上传数据解析）
 * <br>
 * 功能：处理TCP粘包/半包，校验协议完整性，解析为业务对象
 * <br>
 * 协议格式：
 * <pre>
 * ┌─────────┬───────┬──────┬──────┬─────────┬───────────────┬──────┬──────┐
 * │ 起始符   │ 版本  │ 长度  │ 指令  │ 设备ID   │ 数据域(变长)   │ CRC  │ 结束符 │
 * │ 2字节    │ 1字节 │ 2字节 │ 1字节 │ 4字节    │ N字节         │ 2字节 │ 2字节 │
 * └─────────┴───────┴──────┴──────┴─────────┴───────────────┴──────┴──────┘
 * </pre>
 */
@Slf4j
public abstract class BaseFrameDecoder extends ByteToMessageDecoder {
    // 协议头固定长度：起始符(2) + 版本(1) + 长度(2) + 指令(1)
    private static final int HEADER_FIXED_LEN = 6;
    // 基础数据域固定长度：设备ID(4) + 会话ID(2) + 任务ID(4)
    private static final int PAYLOAD_FIXED_LEN = 10;
    // 协议尾固定长度：CRC(2) + 结束符(2)
    private static final int TAIL_FIXED_LEN = 4;
    // 最小有效帧长度
    private static final int MIN_FRAME_LEN = HEADER_FIXED_LEN + PAYLOAD_FIXED_LEN + TAIL_FIXED_LEN;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 1. 滑动窗口查找起始符
        while (in.readableBytes() >= MIN_FRAME_LEN) {
            in.markReaderIndex();

            int startIndex = findStartFlag(in);
            if (startIndex == -1) {
                log.warn("未找到起始符，可用数据: {} bytes", in.readableBytes());
                return;
            }

            // 2. 解析协议头
            FrameHeader header = parseHeader(in);
            if (header == null) {
                log.warn("协议头解析失败");
                return;
            }

            // 3. 校验数据长度
            if (!validateFrameLength(in, header.totalLength)) {
                sendErrorResponse(ctx);
                return;
            }

            // 4. 提取数据域
            ByteBuf payload = extractPayload(in, header.totalLength);

            // 5. 校验协议尾
            if (!validateFrameTail(in, header.totalLength)) {
                payload.release();
                sendErrorResponse(ctx);
                return;
            }

            // 6. 解析业务对象
            parseBusinessFrame(header.command, payload, out);
        }
    }

    // 查找起始符0xAA55，返回起始位置
    private int findStartFlag(ByteBuf in) {
        while (in.readableBytes() >= 2) {
            int readerIndex = in.readerIndex();
            if (in.getShort(readerIndex) == START_FLAG) {
                return readerIndex;
            }
            in.skipBytes(1); // 滑动窗口
        }
        return -1;
    }

    // 解析协议头
    private FrameHeader parseHeader(ByteBuf in) {
        try {
            FrameHeader header = new FrameHeader(in.readShort(), in.readByte(), in.readUnsignedShort(), in.readByte());
            log.debug("解析协议头: startFlag=0x{}, version={}, length={}, cmd=0x{}",
                    Integer.toHexString(header.startFlag & 0xFFFF),
                    header.version,
                    header.totalLength,
                    Integer.toHexString(header.command & 0xFF));

            return header;
        } catch (Exception e) {
            log.error("协议头解析异常", e);
            return null;
        }
    }

    // 校验帧长度有效性
    private boolean validateFrameLength(ByteBuf in, int totalLength) {
        if (totalLength < MIN_FRAME_LEN) {
            log.error("无效帧长度: {} (最小要求: {})", totalLength, MIN_FRAME_LEN);
            return false;
        }
        if (in.readableBytes() < totalLength - HEADER_FIXED_LEN) {
            log.warn("数据不完整，需要: {} 实际: {}", totalLength, in.readableBytes() + HEADER_FIXED_LEN);
            in.resetReaderIndex();
            return false;
        }
        return true;
    }

    // 提取数据域（包含基础字段和扩展数据）
    private ByteBuf extractPayload(ByteBuf in, int totalLength) {
        int payloadLength = totalLength - HEADER_FIXED_LEN - TAIL_FIXED_LEN;
        ByteBuf payload = in.readSlice(payloadLength).retain(); // 需要手动释放

        log.debug("提取数据域: length={}, data={}", payloadLength, HexUtil.formatHexString(payload.duplicate()));

        return payload;
    }

    // 校验CRC和结束符
    private boolean validateFrameTail(ByteBuf in, int totalLength) {
        // 计算CRC校验范围（从起始符到数据域结束）
        int crcStart = in.readerIndex() - totalLength + TAIL_FIXED_LEN;
        ByteBuf crcRange = in.slice(crcStart, totalLength - TAIL_FIXED_LEN);

        long crcCalculated = CRC.calculateCRC(CRC.Parameters.CCITT, crcRange.nioBuffer());
        long crcReceived = in.readUnsignedShort();

        // 校验结束符
        short endFlag = in.readShort();

        log.debug("CRC校验: 计算=0x{}, 接收=0x{}, 结束符=0x{}",
                Long.toHexString(crcCalculated),
                Long.toHexString(crcReceived),
                Integer.toHexString(endFlag & 0xFFFF));

        if (crcCalculated != crcReceived) {
            log.error("CRC校验失败");
            return false;
        }
        if (endFlag != END_FLAG) {
            log.error("无效结束符");
            return false;
        }
        return true;
    }

    // 解析业务对象
    private void parseBusinessFrame(byte cmdType, ByteBuf payload, List<Object> out) {
        try {
            // 读取基础字段
            int deviceId = payload.readInt();
            short sessionId = payload.readShort();
            int taskId = payload.readInt();

            // 解析指令特定数据
            BaseFrame frame = parseByCommand(cmdType, payload);
            if (frame != null) {
                frame.setDeviceId(deviceId);
                frame.setSessionId(sessionId);
                frame.setTaskId(taskId);
                out.add(frame);
                log.debug("成功解析帧: cmd=0x{}, deviceId={}", Integer.toHexString(cmdType & 0xFF), deviceId);
            }

        } catch (Exception e) {
            log.error("业务数据解析异常", e);
        } finally {
            payload.release(); // 确保释放资源
        }
    }

    // 根据指令类型解析具体业务数据
    protected abstract BaseFrame parseByCommand(byte command, ByteBuf payload);

    // 发送错误响应
    protected abstract void sendErrorResponse(ChannelHandlerContext ctx);

    /**
     * Copyright © 深圳市树深计算机系统有限公司 版权所有
     *
     * <p>协议头</p>
     *
     * @author 周广明
     * @since 2025/4/2 22:35
     */
    private record FrameHeader(short startFlag, byte version, int totalLength, byte command) {
    }
}