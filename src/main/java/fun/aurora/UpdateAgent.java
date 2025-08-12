package fun.aurora;

import fun.aurora.utils.DownloadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateAgent {
    private static final Logger logger = LoggerFactory.getLogger(UpdateAgent.class);
    public static void main(String[] args){
        System.out.println("正在使用命令行模式，尝试拉起更新中...");
        logger.warn("为避免对您的系统造成异常，请在minecraft版本目录下使用本工具");
        doUpdate();
    }

    public static void premain(String args, Instrumentation inst) {
        System.out.println("正在使用agent模式，尝试拉起更新中");
        doUpdate();
    }

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("加载中...");
        doUpdate();
    }

    private static void doUpdate() {
        if (DownloadUtil.downloadFromUrlWithAutoFilename("https://web.nyauru.cn/update.json") != 0) {
            logger.error("获取更新列表失败，请检查网络连接！");
        }
        logger.info("获取更新列表成功！");
        logger.info("正在下载更新...");
        UpdateHandler.downloadUpdate();
        logger.info("正在复制更新...");
        try {
            UpdateHandler.copyUpdate();
        } catch (IOException e) {
            logger.error("复制更新失败: {}", e.getMessage());
        }
        logger.info("正在删除旧文件...");
        UpdateHandler.deleteUpdate();
        logger.info("更新完成！");


    }
//    private static void setLoggerPath() throws URISyntaxException {
//        Path jarDir = Paths.get(UpdateAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
//        System.setProperty("JAR_DIR", jarDir.toAbsolutePath().toString());
//    }
//    static {
//        try {
//            Path jarDir = Paths.get(UpdateAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
//            System.setProperty("JAR_DIR", jarDir.toAbsolutePath().toString());
//        } catch (URISyntaxException e) {
//            System.out.println("获取JAR目录失败！");
//        }
//    }
}
