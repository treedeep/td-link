package cn.treedeep.link.device.protocol.model.command;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开始录制指令。
 * 用于服务端向设备发送开始录制的请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CmdStartRecording extends Pv1BaseFrame {

    @Override
    public byte getCommand() {
        return V1.CMD_START_RECORDING;
    }
}
