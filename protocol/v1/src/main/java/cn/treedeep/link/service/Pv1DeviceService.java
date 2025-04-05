package cn.treedeep.link.service;

import cn.treedeep.link.annotation.Remark;

public interface Pv1DeviceService extends DeviceService {


    /**
     * 设备绑定指令
     *
     * @param deviceId 目标设备ID
     * @param taskId   任务ID
     * @return 指令发送结果
     */
    CommandResult deviceBind(@Remark("deviceId") int deviceId, @Remark("taskId") int taskId);

    /**
     * 设备解绑指令
     *
     * @param deviceId 目标设备ID
     * @param taskId   任务ID
     * @return 指令发送结果
     */
    CommandResult deviceUnBind(@Remark("deviceId") int deviceId, @Remark("taskId") int taskId);

    /**
     * 下发开始录制指令
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult startRecording(@Remark("deviceId") int deviceId);

    /**
     * 下发停止录制指令
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult stopRecording(@Remark("deviceId") int deviceId);

}