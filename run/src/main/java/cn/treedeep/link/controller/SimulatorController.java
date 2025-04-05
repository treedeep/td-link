package cn.treedeep.link.controller;

import cn.treedeep.link.device.client.SimulatorManager;
import cn.treedeep.link.service.Pv1DeviceService;
import cn.treedeep.link.simulator.DeviceSimulator;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Copyright © 深圳市树深计算机系统有限公司 版权所有
 *
 * <p>设备模拟器</p>
 *
 * @author 周广明
 * @since 2025/3/30 11:42
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    @Resource
    private Pv1DeviceService commandService;

    @Resource
    private SimulatorManager simulatorManager;


    /**
     * 创建新的设备模拟器
     */
    @GetMapping("/create")
    public ResponseEntity<DeviceSimulator> createSimulator(@RequestParam int deviceId) {
        DeviceSimulator simulator = simulatorManager.createSimulator(deviceId);
        return ResponseEntity.ok(simulator);
    }

    /**
     * 获取所有模拟器列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<DeviceSimulator>> listSimulators() {
        return ResponseEntity.ok(simulatorManager.getAllSimulators());
    }

    /**
     * 连接设备到服务器
     */
    @GetMapping("/{deviceId}/connect")
    public ResponseEntity<String> connectDevice(@PathVariable int deviceId) {
        simulatorManager.connectDevice(deviceId);
        return ResponseEntity.ok("设备连接指令已发送");
    }

    /**
     * 断开设备连接
     */
    @GetMapping("/{deviceId}/disconnect")
    public ResponseEntity<String> disconnectDevice(@PathVariable int deviceId) {
        simulatorManager.disconnectDevice(deviceId);
        return ResponseEntity.ok("设备断开指令已发送");
    }

    /**
     * 触发文件上传
     */
    @GetMapping("/{deviceId}/upload")
    public ResponseEntity<String> uploadFile(@PathVariable int deviceId, @RequestParam String filePath) {
        simulatorManager.startFileUpload(deviceId, filePath);
        return ResponseEntity.ok("文件上传指令已发送");
    }

    /**
     * 获取模拟器状态
     */
    @GetMapping("/{deviceId}/status")
    public ResponseEntity<Map<String, Object>> getSimulatorStatus(@PathVariable int deviceId) {
        return ResponseEntity.ok(simulatorManager.getSimulatorStatus(deviceId));
    }

    /**
     * 批量创建模拟器
     */
    @PostMapping("/batch-create")
    public ResponseEntity<List<DeviceSimulator>> batchCreateSimulators(
            @RequestParam int startDeviceId,
            @RequestParam int count) {
        List<DeviceSimulator> simulators = simulatorManager.batchCreateSimulators(startDeviceId, count);
        return ResponseEntity.ok(simulators);
    }

    /**
     * 删除模拟器
     */
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<String> removeSimulator(@PathVariable int deviceId) {
        simulatorManager.removeSimulator(deviceId);
        return ResponseEntity.ok("模拟器已删除");
    }

    /**
     * 删除所有模拟器
     */
    @DeleteMapping("/all")
    public ResponseEntity<String> removeAllSimulators() {
        simulatorManager.removeAllSimulators();
        return ResponseEntity.ok("所有模拟器已删除");
    }

    /**
     * 四合一功能：创建设备、获取列表、连接设备、绑定任务
     *
     * @param count 要创建的设备数量
     * @return 创建的设备和绑定的任务信息
     */
    @GetMapping("/setup-all")
    public ResponseEntity<Map<String, Object>> setupAllDevices(@RequestParam int count)  {

        List<Map<String, Object>> deviceInfoList = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 生成随机设备ID的起始值
        int startDeviceId = random.nextInt(1000, 10000);

        // 批量创建设备
        List<DeviceSimulator> simulators = simulatorManager.batchCreateSimulators(startDeviceId, count);

        // 连接设备并绑定任务
        for (DeviceSimulator simulator : simulators) {
            int deviceId = simulator.getDeviceId();

            // 连接设备
            simulatorManager.connectDevice(deviceId);

            // 随机生成任务ID
            int taskId = random.nextInt(1, 100 + 1);

            // 绑定任务
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            commandService.deviceBind(deviceId, taskId);

            // 收集设备信息
            Map<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("deviceId", deviceId);
            deviceInfo.put("taskId", taskId);
            deviceInfo.put("status", simulator.getStatus());

            deviceInfoList.add(deviceInfo);
        }

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("totalDevices", count);
        result.put("devices", deviceInfoList);

        return ResponseEntity.ok(result);
    }

}
