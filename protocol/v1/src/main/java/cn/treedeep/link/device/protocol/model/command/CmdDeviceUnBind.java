package cn.treedeep.link.device.protocol.model.command;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 设备解绑指令。
 * 用于服务端向设备发送解绑请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CmdDeviceUnBind extends Pv1BaseFrame {

    @Override
    public byte getCommand() {
        return V1.CMD_DEVICE_UNBIND;
    }
}
