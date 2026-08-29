package com.xiang.xiangaicodemother.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.xiang.xiangaicodemother.config.properties.CodeDeployProperties;
import com.xiang.xiangaicodemother.config.properties.ScreenshotProperties;
import com.xiang.xiangaicodemother.exception.BusinessException;
import com.xiang.xiangaicodemother.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;

/** 使用独立的无头 Chrome 实例生成网页截图。 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebScreenshotUtils {

    private final ScreenshotProperties properties;
    private final CodeDeployProperties codeDeployProperties;

    public Path saveWebPageScreenshot(String webUrl) {
        URI targetUri = validateScreenshotUrl(webUrl);
        Path workDir = createWorkDirectory();
        Path originalImage = workDir.resolve("original.png");
        Path compressedImage = workDir.resolve("cover.jpg");
        WebDriver driver = null;
        try {
            driver = createChromeDriver();
            driver.get(targetUri.toString());
            validateScreenshotUrl(driver.getCurrentUrl());
            waitForPageLoad(driver);
            validateScreenshotUrl(driver.getCurrentUrl());
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(originalImage, screenshotBytes);
            ImgUtil.compress(originalImage.toFile(), compressedImage.toFile(), 0.3f);
            Files.deleteIfExists(originalImage);
            return compressedImage;
        } catch (BusinessException e) {
            cleanupDirectory(workDir);
            throw e;
        } catch (Exception e) {
            cleanupDirectory(workDir);
            log.error("生成应用封面截图失败，url={}", webUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成应用封面截图失败");
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    log.warn("关闭截图浏览器失败", e);
                }
            }
        }
    }

    /** 只允许访问平台自己的部署地址，避免将截图能力变成任意 URL 访问入口。 */
    public URI validateScreenshotUrl(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "截图网址不能为空");
        }
        try {
            URI target = URI.create(webUrl).normalize();
            URI deployHost = URI.create(codeDeployProperties.normalizedDeployHost()).normalize();
            boolean validScheme = "http".equalsIgnoreCase(target.getScheme())
                    || "https".equalsIgnoreCase(target.getScheme());
            boolean sameOrigin = validScheme
                    && target.getHost() != null
                    && target.getHost().equalsIgnoreCase(deployHost.getHost())
                    && effectivePort(target) == effectivePort(deployHost)
                    && target.getScheme().equalsIgnoreCase(deployHost.getScheme());
            if (!sameOrigin || target.getUserInfo() != null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只允许截取平台部署的应用");
            }
            return target;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "截图网址格式错误");
        }
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private Path createWorkDirectory() {
        try {
            Path root = Path.of(System.getProperty("user.dir"), "tmp", "screenshots")
                    .toAbsolutePath().normalize();
            Files.createDirectories(root);
            return Files.createDirectory(root.resolve(UUID.randomUUID().toString()));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "创建截图临时目录失败");
        }
    }

    private WebDriver createChromeDriver() {
        try {
            ChromeOptions options = new ChromeOptions();
            if (StrUtil.isNotBlank(properties.getBrowserPath())) {
                File browserFile = FileUtil.file(properties.getBrowserPath());
                if (!browserFile.isFile()) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "配置的 Chrome 浏览器不存在");
                }
                options.setBinary(browserFile);
            }
            options.addArguments("--headless=new", "--disable-gpu", "--disable-dev-shm-usage",
                    "--disable-extensions", "--hide-scrollbars",
                    String.format("--window-size=%d,%d", properties.getWidth(), properties.getHeight()));
            if (properties.isNoSandbox()) {
                options.addArguments("--no-sandbox");
            }
            ChromeDriver driver;
            if (StrUtil.isNotBlank(properties.getDriverPath())) {
                File driverFile = FileUtil.file(properties.getDriverPath());
                if (!driverFile.isFile()) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "配置的 ChromeDriver 不存在");
                }
                ChromeDriverService service = new ChromeDriverService.Builder()
                        .usingDriverExecutable(driverFile)
                        .build();
                driver = new ChromeDriver(service, options);
            } else {
                WebDriverManager.chromedriver().setup();
                driver = new ChromeDriver(options);
            }
            driver.manage().timeouts().pageLoadTimeout(
                    Duration.ofSeconds(properties.getPageLoadTimeoutSeconds()));
            return driver;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("初始化无头 Chrome 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化无头 Chrome 失败");
        }
    }

    private void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(
                    driver, Duration.ofSeconds(properties.getReadyTimeoutSeconds()));
            wait.until(current -> "complete".equals(
                    ((JavascriptExecutor) current).executeScript("return document.readyState")));
            if (properties.getRenderDelayMillis() > 0) {
                Thread.sleep(properties.getRenderDelayMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "截图任务已中断");
        } catch (Exception e) {
            log.warn("等待页面完全加载超时，将使用当前页面内容截图");
        }
    }

    private void cleanupDirectory(Path directory) {
        try {
            FileUtil.del(directory.toFile());
        } catch (Exception e) {
            log.warn("清理截图临时目录失败，path={}", directory, e);
        }
    }
}
