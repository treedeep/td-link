package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.BaseFrame;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备绑定响应指令。
 * 用于设备响应服务端的绑定请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReportDeviceBindResponse extends BaseFrame {

    private short status; // 2字节状态码（三种状态：绑定、解绑、错误）

    @Override
    public byte getCommand() {
        return V1.REPORT_DEVICE_BIND_RESPONSE;
    }
}
