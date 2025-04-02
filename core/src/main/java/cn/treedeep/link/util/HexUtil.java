package cn.treedeep.link.util;

import io.netty.buffer.ByteBuf;

import java.nio.ByteBuffer;

public class HexUtil {

    public static String formatHexString(ByteBuf buf) {
        if (buf == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int readerIndex = buf.readerIndex();
        for (int i = 0; i < buf.readableBytes(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", buf.getByte(readerIndex + i)));
        }
        return sb.toString();
    }

    public static String formatHexString(ByteBuffer buffer) {
        if (buffer == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int position = buffer.position();
        for (int i = 0; i < buffer.remaining(); i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", buffer.get(position + i)));
        }
        return sb.toString();
    }
}
