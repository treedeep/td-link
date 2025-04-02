package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备连接请求指令。
 * 用于设备向服务端发送连接请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReportDeviceConnectionRequest extends BaseFrame {

    @Override
    public byte getCommand() {
        return V1.REPORT_DEVICE_CONNECTION_REQUEST;
    }
}

