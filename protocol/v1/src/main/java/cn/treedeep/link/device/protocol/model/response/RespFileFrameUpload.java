package cn.treedeep.link.device.protocol.model.response;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传单帧响应指令。
 * 用于服务端响应设备的单帧文件上传请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RespFileFrameUpload extends BaseFrame {

    private int frameSeq;       // 帧序号(4B)
    private byte receiveStatus; // 接收状态(1B)

    @Override
    public byte getCommand() {
        return V1.RESP_FILE_FRAME_UPLOAD;
    }
}
