package cn.treedeep.link.service;

import cn.treedeep.link.annotation.Remark;
import cn.treedeep.link.model.DeviceInfo;

import java.util.List;

public interface DeviceService {

    /**
     * 获取所有连接的设备列表
     *
     * @return 设备信息列表
     */
    List<DeviceInfo> getConnectedDevices();

    /**
     * 强制断开设备连接
     *
     * @param deviceId 目标设备ID
     * @return 指令发送结果
     */
    CommandResult forceDisconnect(@Remark("deviceId") int deviceId);

}
