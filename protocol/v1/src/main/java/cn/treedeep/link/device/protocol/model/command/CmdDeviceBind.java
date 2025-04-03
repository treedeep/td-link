package cn.treedeep.link.device.protocol.model.command;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备绑定指令。
 * 用于服务端向设备发送绑定请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CmdDeviceBind extends BaseFrame {

    @Override
    public byte getCommand() {
        return V1.CMD_DEVICE_BIND;
    }
}
