package cn.treedeep.link.device.protocol.model.command;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 强制断开指令。
 * 用于服务端强制断开与设备的连接。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CmdForceDisconnect extends BaseFrame {

    @Override
    public byte getCommand() {
        return V1.CMD_FORCE_DISCONNECT;
    }
}
