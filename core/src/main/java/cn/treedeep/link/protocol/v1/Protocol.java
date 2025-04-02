package cn.treedeep.link.protocol.v1;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public interface Protocol {
    byte PROTOCOL_VERSION = 0x01;
    short START_FLAG = (short) 0xAA55;
    short END_FLAG = (short) 0x55AA;


    static LengthFieldBasedFrameDecoder lengthFieldBasedFrameDecoder() {
        return new LengthFieldBasedFrameDecoder(
                65535,  // maxFrameLength (2字节无符号最大值为0xFFFF)
                3,                     // lengthFieldOffset = 起始符(2B) + 版本(1B) 后
                2,                     // lengthFieldLength = 协议总长度字段占2字节
                -5,                    // lengthAdjustment = -(lengthFieldOffset + lengthFieldLength) = -(3+2)
                0                      // 不剥离头部，后续需要校验起始符/结束符
        );
    }
}
