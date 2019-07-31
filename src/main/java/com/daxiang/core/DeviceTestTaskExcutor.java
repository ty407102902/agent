package com.daxiang.core;

import com.daxiang.api.MasterApi;
import com.daxiang.core.javacompile.JavaCompiler;
import com.daxiang.core.testng.TestNGCodeConverter;
import com.daxiang.core.testng.TestNGRunner;
import com.daxiang.model.Device;
import com.daxiang.model.action.Action;
import com.daxiang.model.devicetesttask.DeviceTestTask;
import com.daxiang.utils.UUIDUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Created by jiangyitao.
 */
@Slf4j
public class DeviceTestTaskExcutor {
    /**
     * 执行测试任务队列
     */
    private final BlockingQueue<DeviceTestTask> testTaskQueue = new LinkedBlockingQueue<>();
    /**
     * 执行测试任务线程
     */
    private Thread excuteTestTaskThread;

    private MobileDevice mobileDevice;
    private Device device;
    private String deviceId;

    public DeviceTestTaskExcutor(MobileDevice mobileDevice) {
        this.mobileDevice = mobileDevice;
        this.device = mobileDevice.getDevice();
        this.deviceId = mobileDevice.getId();

        excuteTestTaskThread = new Thread(() -> {
            while (true) {
                DeviceTestTask deviceTestTask;
                try {
                    deviceTestTask = testTaskQueue.take(); // 没有测试任务，线程阻塞在此
                } catch (InterruptedException e) {
                    // 调用excuteTestTaskThread.interrupt()可以执行到这里
                    log.info("[{}][自动化测试]停止获取测试任务", deviceId);
                    break;
                }
                try {
                    excuteTestTask(deviceTestTask);
                } catch (Exception e) {
                    log.error("[{}][自动化测试]执行测试任务'{}'出错", deviceId, deviceTestTask.getTestTaskName(), e);
                }
            }
        });
        excuteTestTaskThread.start();
    }

    /**
     * 提交测试任务
     *
     * @param deviceTestTask
     */
    public void commitTestTask(DeviceTestTask deviceTestTask) {
        if (!testTaskQueue.offer(deviceTestTask)) {
            throw new RuntimeException("提交测试任务'" + deviceTestTask.getTestTaskName() + "'失败");
        }
    }

    /**
     * 执行测试任务
     *
     * @param deviceTestTask
     */
    private void excuteTestTask(DeviceTestTask deviceTestTask) throws Exception {
        log.info("[{}][自动化测试]开始执行测试任务: {}", deviceId, deviceTestTask.getTestTaskName());

        device.setStatus(Device.USING_STATUS);
        device.setUsername(deviceTestTask.getTestTaskName());
        MasterApi.getInstance().saveDevice(device);

        try {
            String className = "Test_" + UUIDUtil.getUUID();
            String code = new TestNGCodeConverter()
                    .setDeviceTestTaskId(deviceTestTask.getId())
                    .setDeviceId(deviceTestTask.getDeviceId())
                    .setGlobalVars(deviceTestTask.getGlobalVars())
                    .setBeforeClass(deviceTestTask.getBeforeClass())
                    .setAfterClass(deviceTestTask.getAfterClass())
                    .setBeforeMethod(deviceTestTask.getBeforeMethod())
                    .setAfterMethod(deviceTestTask.getAfterMethod())
                    .convert(className, deviceTestTask.getTestcases().stream().map(testcase -> {
                        Action action = new Action();
                        BeanUtils.copyProperties(testcase, action);
                        return action;
                    }).collect(Collectors.toList()), "/codetemplate", "mobile.ftl");
            log.info("[{}][自动化测试]转换代码：{}", deviceId, code);
            // todo 捕获到DynamicCompilerException即编译失败，通知master纠正用例，否则错误的用例会无限下发给agent执行
            Class clazz = JavaCompiler.compile(className, code);
            mobileDevice.freshDriver();
            TestNGRunner.runTestCases(new Class[]{clazz});
        } finally {
            if (mobileDevice.isConnected()) {
                device.setStatus(Device.IDLE_STATUS);
                MasterApi.getInstance().saveDevice(device);
            }
        }
    }
}