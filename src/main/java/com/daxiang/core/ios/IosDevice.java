package com.daxiang.core.ios;

import com.daxiang.core.MobileDevice;
import com.daxiang.core.appium.AppiumServer;
import com.daxiang.core.appium.IosDriverBuilder;
import com.daxiang.core.appium.IosNativePageSourceHandler;
import com.daxiang.model.Device;
import com.daxiang.utils.Terminal;
import com.daxiang.utils.UUIDUtil;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.IOSStartScreenRecordingOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FileUtils;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;

/**
 * Created by jiangyitao.
 */
@Slf4j
public class IosDevice extends MobileDevice {

    /**
     * iproxy localPort remotePort deviceId
     */
    private static final String IPROXY = "iproxy %d %d %s";
    private ExecuteWatchdog mjpegServerIproxyWatchdog;

    public IosDevice(Device device, AppiumServer appiumServer) {
        super(device, appiumServer);
    }

    @Override
    public AppiumDriver initAppiumDriver() {
        AppiumDriver driver = new IosDriverBuilder().build(this);
        setAppiumDriver(driver);
        return driver;
    }

    @Override
    public void installApp(File app) throws IOException {
        IosUtil.installIpa(app.getAbsolutePath(), getId());
    }

    @Override
    public String dump() throws IOException, DocumentException {
        return new IosNativePageSourceHandler(getAppiumDriver()).getPageSource();
    }

    @Override
    public void startRecordingScreen() {
        IOSStartScreenRecordingOptions iosOptions = new IOSStartScreenRecordingOptions();
        // The maximum value is 30 minutes.
        iosOptions.withTimeLimit(Duration.ofMinutes(30));
        iosOptions.withFps(10); // default 10
        iosOptions.withVideoQuality(IOSStartScreenRecordingOptions.VideoQuality.LOW);
        iosOptions.withVideoType("libx264");
        ((IOSDriver) getAppiumDriver()).startRecordingScreen(iosOptions);
    }

    @Override
    public File stopRecordingScreen() throws IOException {
        File videoFile = new File(UUIDUtil.getUUID() + ".mp4");
        String base64Video = ((IOSDriver) getAppiumDriver()).stopRecordingScreen();
        FileUtils.writeByteArrayToFile(videoFile, Base64.getDecoder().decode(base64Video), false);
        return videoFile;
    }

    @Override
    public void installApp(String appDownloadUrl) throws IOException {
        File app = downloadApp(appDownloadUrl);
        try {
            IosUtil.installIpa(app.getAbsolutePath(), getId());
        } finally {
            FileUtils.deleteQuietly(app);
        }
    }

    public int getMjpegServerPort() {
        Object mjpegServerPort = getAppiumDriver().getCapabilities().asMap().get("mjpegServerPort");
        return (int) ((long) mjpegServerPort);
    }

    public void startMjpegServerIproxy() throws IOException {
        int mjpegServerPort = getMjpegServerPort();
        String cmd = String.format(IPROXY, mjpegServerPort, mjpegServerPort, getId());
        log.info("[ios][{}]mjpegServer: {}", getId(), cmd);
        mjpegServerIproxyWatchdog = Terminal.executeAsyncAndGetWatchdog(cmd);
    }

    public void stopMjpegServerIproxy() {
        if (mjpegServerIproxyWatchdog != null) {
            log.info("[ios][{}]mjpegServer iproxy stop", getId());
            mjpegServerIproxyWatchdog.destroyProcess();
        }
    }
}
