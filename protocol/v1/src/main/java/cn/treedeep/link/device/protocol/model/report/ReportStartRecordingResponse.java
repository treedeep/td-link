package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开始录制响应指令。
 * 用于设备响应服务端的开始录制请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReportStartRecordingResponse extends BaseFrame {

    private short status; // 2字节状态码

    @Override
    public byte getCommand() {
        return V1.REPORT_START_RECORDING_RESPONSE;
    }
}
