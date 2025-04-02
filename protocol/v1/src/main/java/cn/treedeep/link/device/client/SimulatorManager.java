package cn.treedeep.link.device.client;

import cn.treedeep.link.simulator.DeviceSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("p_v1_SimulatorManager")
public class SimulatorManager {

    @Value("${link.serverPort:9900}")
    private int serverPort;

    @Value("${link.serverHost:localhost}")
    private String serverHost;

    private final Map<Integer, DeviceSimulator> simulators = new ConcurrentHashMap<>();

    public DeviceSimulator createSimulator(int deviceId) {
        DeviceSimulator simulator = new Pv1Device(deviceId);
        simulators.put(deviceId, simulator);
        return simulator;
    }

    public List<DeviceSimulator> getAllSimulators() {
        return new ArrayList<>(simulators.values());
    }

    public void connectDevice(int deviceId) {
        DeviceSimulator simulator = simulators.get(deviceId);
        if (simulator != null) {
            simulator.connect(serverHost, serverPort);
        }
    }

    public void disconnectDevice(int deviceId) {
        DeviceSimulator simulator = simulators.get(deviceId);
        if (simulator != null) {
            simulator.disconnect();
        }
    }

    public void startFileUpload(int deviceId, String filePath) {
        DeviceSimulator simulator = simulators.get(deviceId);
        if (simulator != null) {
            simulator.uploadFile(filePath);
        }
    }

    public Map<String, Object> getSimulatorStatus(int deviceId) {
        DeviceSimulator simulator = simulators.get(deviceId);
        Map<String, Object> status = new HashMap<>();
        if (simulator != null) {
            status.put("deviceId", simulator.getDeviceId());
            status.put("sessionId", simulator.getSessionId());
            status.put("status", simulator.getStatus());
            status.put("connected", simulator.getChannel() != null && simulator.getChannel().isActive());
        }
        return status;
    }

    public List<DeviceSimulator> batchCreateSimulators(int startDeviceId, int count) {
        List<DeviceSimulator> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            created.add(createSimulator(startDeviceId + i));
        }
        return created;
    }

    public void removeSimulator(int deviceId) {
        DeviceSimulator simulator = simulators.remove(deviceId);
        if (simulator != null) {
            simulator.disconnect();
        }
    }

    public void removeAllSimulators() {
        simulators.values().forEach(DeviceSimulator::disconnect);
        simulators.clear();
    }
}