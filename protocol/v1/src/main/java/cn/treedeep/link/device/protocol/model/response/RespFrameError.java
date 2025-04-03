package cn.treedeep.link.device.protocol.model.response;

import cn.treedeep.link.device.protocol.V1;
import cn.treedeep.link.protocol.v1.Pv1BaseFrame;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>异常帧响应</p>
 *
 * @author 周广明
 * @since 2025/4/3 10:45
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RespFrameError extends Pv1BaseFrame {

    @Override
    public byte getCommand() {
        return V1.RESP_FRAME_EXCEPTION;
    }
}
