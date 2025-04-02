package cn.treedeep.link.protocol.v1;

import cn.treedeep.link.util.CRC;
import cn.treedeep.link.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import static cn.treedeep.link.protocol.v1.Protocol.END_FLAG;
import static cn.treedeep.link.protocol.v1.Protocol.START_FLAG;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>协议编码器（服务器下发）</p>
 *
 * @author 周广明
 * @since 2025/3/29 15:21
 */
@Slf4j
public abstract class BaseFrameEncoder extends MessageToByteEncoder<BaseFrame> {

    @Override
    protected void encode(ChannelHandlerContext ctx, BaseFrame msg, ByteBuf out) {
        // 1. 分配临时缓冲区（自动释放）
        ByteBuf buffer = ctx.alloc().compositeBuffer();
        try {
            // 2. 构建协议头（起始符+版本+占位长度+指令）
            writeHeader(buffer, msg);

            // 3. 写入数据域（设备ID、会话ID等基础字段+指令特定字段）

            // 写入基础字段
            buffer.writeInt(msg.getDeviceId()).writeShort(msg.getSessionId()).writeInt(msg.getTaskId());
            writePayload(buffer, msg);

            // 4. 回填总长度字段
            updateTotalLength(buffer);

            // 5. 计算并写入CRC
            writeCRC(buffer);

            // 6. 写入结束符
            buffer.writeShort(END_FLAG);

            // 7. 输出日志和最终数据
            log.debug("发送帧：{}", HexUtil.formatHexString(buffer));
            out.writeBytes(buffer);
        } finally {
            buffer.release();
        }
    }

    // 写入协议头（起始符+版本+占位长度+指令）
    private void writeHeader(ByteBuf buf, BaseFrame msg) {
        buf.writeShort(START_FLAG)
                .writeByte(msg.getVersion())
                .writeShort(0); // 占位总长度字段
        buf.writeByte(msg.getCommand());
    }

    // 回填总长度字段
    private void updateTotalLength(ByteBuf buf) {
        int totalLength = buf.readableBytes() + 4; // +4=CRC(2)+结束符(2)
        buf.setShort(3, totalLength); // 在偏移量3处写入长度
    }

    // 计算并写入CRC（从起始符到数据域结束）
    private void writeCRC(ByteBuf buf) {
        ByteBuf crcRange = buf.duplicate().readerIndex(0);
        long crc = CRC.calculateCRC(CRC.Parameters.CCITT, crcRange.nioBuffer());
        buf.writeShort((short) crc);
    }

    // 写入数据域内容
    protected abstract void writePayload(ByteBuf buf, BaseFrame frame);
}
