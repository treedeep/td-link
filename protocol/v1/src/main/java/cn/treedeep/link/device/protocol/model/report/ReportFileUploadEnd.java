package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件结束上传指令。
 * 用于设备向服务端发送文件上传结束的通知。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReportFileUploadEnd extends BaseFrame {

    private int totalFrames;    // 总帧数(4B)

    @Override
    public byte getCommand() {
        return V1.REPORT_FILE_UPLOAD_END;
    }

}
