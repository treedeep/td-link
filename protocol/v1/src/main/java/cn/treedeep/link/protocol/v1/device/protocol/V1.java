package cn.treedeep.link.protocol.v1.device.protocol;

import cn.treedeep.link.core.protocol.v1.Protocol;

public interface V1 extends Protocol {

    // 服务端下发指令（设备：接收，服务：下发）
    byte CMD_DEVICE_BIND = 0x01;        // 设备绑定
    byte CMD_DEVICE_UNBIND = 0x02;      // 设备解绑
    byte CMD_START_RECORDING = 0x03;    // 开始录制
    byte CMD_STOP_RECORDING = 0x04;     // 停止录制
    byte CMD_HEARTBEAT = 0x05;          // 心跳检测
    byte CMD_FORCE_DISCONNECT = 0x06;   // 强制断开

    // 服务端响应指令（设备：接收，服务：下发）
    byte RESP_DEVICE_CONNECTION = 0x11;     // 设备连接响应
    byte RESP_HEARTBEAT = 0x12;             // 心跳检测响应
    byte RESP_KEYFRAME_MARK = 0x13;         // 关键帧标记响应
    byte RESP_FILE_FRAME_UPLOAD = 0x14;     // 文件上传单帧响应
    byte RESP_FILE_UPLOAD_END = 0x15;       // 文件上传结束响应
    byte RESP_FRAME_EXCEPTION = 0x16;       // 帧解码异常响应

    // 设备上报指令（设备：上传，服务：接收）
    byte REPORT_DEVICE_CONNECTION_REQUEST = 0x21;   // 设备连接请求
    byte REPORT_DEVICE_BIND_RESPONSE = 0x22;        // 设备绑定解/绑定响应
    byte REPORT_START_RECORDING_RESPONSE = 0x23;    // 开始录制响应
    byte REPORT_STOP_RECORDING_RESPONSE = 0x24;     // 停止录制响应
    byte REPORT_HEARTBEAT_PACKET = 0x25;            // 心跳包
    byte REPORT_KEYFRAME_MARK = 0x26;               // 关键帧标记
    byte REPORT_FILE_FRAME_UPLOAD = 0x27;           // 文件单帧上传
    byte REPORT_FILE_UPLOAD_END = 0x28;             // 文件结束上传

}
