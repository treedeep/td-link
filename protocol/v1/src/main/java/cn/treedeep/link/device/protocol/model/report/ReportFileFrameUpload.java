package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件单帧上传指令。
 * 用于设备向服务端上传单帧文件数据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReportFileFrameUpload extends BaseFrame {

    private int frameSeq;       // 帧序号(4B)
    private ByteBuf frameData;  // 文件数据

    @Override
    public byte getCommand() {
        return V1.REPORT_FILE_FRAME_UPLOAD;
    }


    public ReportFileFrameUpload(int frameSeq, byte[] frameData) {
        this.frameSeq = frameSeq;
        // 创建一个ByteBuf并写入数据
        this.frameData = Unpooled.wrappedBuffer(frameData);
    }

    public byte[] getFrameDataBytes() {
        if (frameData == null) {
            return new byte[0];
        }

        byte[] bytes = new byte[frameData.readableBytes()];
        // 注意：这里只是读取数据，不会改变frameData的读索引
        frameData.getBytes(frameData.readerIndex(), bytes);
        return bytes;
    }

}
