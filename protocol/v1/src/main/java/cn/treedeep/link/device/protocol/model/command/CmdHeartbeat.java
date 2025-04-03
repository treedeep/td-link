package cn.treedeep.link.device.protocol.model.command;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 心跳检测指令。
 * 用于服务端向设备发送心跳检测请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CmdHeartbeat extends Pv1BaseFrame {

    @Override
    public byte getCommand() {
        return V1.CMD_HEARTBEAT;
    }
}
