package cn.treedeep.link.device.protocol.model.response;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RespFrameError extends BaseFrame {

    @Override
    public byte getCommand() {
        return V1.RESP_FRAME_EXCEPTION;
    }
}
