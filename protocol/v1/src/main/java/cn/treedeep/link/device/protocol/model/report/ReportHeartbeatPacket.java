package cn.treedeep.link.device.protocol.model.report;

import cn.treedeep.link.protocol.v1.BaseFrame;
import cn.treedeep.link.device.protocol.V1;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 心跳包指令。
 * 用于设备向服务端发送心跳包，以维持连接。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
public class ReportHeartbeatPacket extends BaseFrame {

    private byte battery;   // 1字节电池
    private byte status;    // 1字节状态码

    @Override
    public byte getCommand() {
        return V1.REPORT_HEARTBEAT_PACKET;
    }
}
