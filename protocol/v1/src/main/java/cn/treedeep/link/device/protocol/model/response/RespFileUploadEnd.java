package cn.treedeep.link.device.protocol.model.response;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件上传结束响应指令。
 * 用于服务端响应设备的文件上传结束请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class RespFileUploadEnd extends Pv1BaseFrame {

    private int totalFrames;    // 总帧数(4B)
    private byte[] fileHash;    // 文件校验(16B)

    @Override
    public byte getCommand() {
        return V1.RESP_FILE_UPLOAD_END;
    }
}
