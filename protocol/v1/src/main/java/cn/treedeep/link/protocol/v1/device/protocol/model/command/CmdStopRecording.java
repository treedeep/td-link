package cn.treedeep.link.protocol.v1.device.protocol.model.command;

import cn.treedeep.link.protocol.v1.device.protocol.V1;
import cn.treedeep.link.core.protocol.v1.BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 停止录制指令。
 * 用于服务端向设备发送停止录制的请求。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CmdStopRecording extends BaseFrame {

    @Override
    public byte getCommand() {
        return V1.CMD_STOP_RECORDING;
    }
}
