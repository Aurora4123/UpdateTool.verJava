package fun.aurora;

import fun.aurora.utils.DownloadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UpdateAgent {
    private static final Logger logger = LoggerFactory.getLogger(UpdateAgent.class);
    private static final String DEFAULT_UPDATE_URL = "https://web.nyauru.cn/update.json";
    public static void main(String[] args){
        logger.info("正在使用命令行模式，尝试拉起更新中...");
        try {
            Path jarPath = Paths.get(UpdateAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            logger.info("请注意，当前程序的工作目录为{}", jarPath);
        } catch (Exception e) {
            logger.error("获取当前工作目录失败！({})", e.getMessage());
        }
        if(args.length == 0){
            doUpdate(null);
        } else {
            if(urlCheck(args[0])) {
                doUpdate(args[0]);
            } else {
                logger.error("无效的URL: {}", args[0]);
                logger.warn("URL需要以http://或https://开头！你个⑨");
                System.exit(9);
            }
        }
    }

    public static void premain(String args, Instrumentation inst) {
        logger.info("正在使用agent模式，尝试拉起更新中");
//        doUpdate(args);
    }

    public static void agentmain(String args, Instrumentation inst) {
        logger.info("正在使用agent动态加载模式，启动更新中...");
        doUpdate(args);
    }

    private static void doUpdate(String url) {
        logger.info("==================================================");
        logger.info("UpdateTool log");
        logger.info("ver: {}", UpdateHandler.class.getPackage().getImplementationVersion());
        logger.info("Launch Time: {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        logger.info("==================================================");
        logger.warn("为避免对您的系统造成异常，请在minecraft版本目录下使用本工具");
        
        if(url == null){
            url = DEFAULT_UPDATE_URL;
            logger.warn("未指定更新地址，使用默认地址：{}", url);
        }
        if (DownloadUtil.downloadFromUrlWithAutoFilename(url) != 0) {
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
        logger.info("更新完成！程序将在5s后退出");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            logger.error("等待过程出现错误: {}", e.getMessage());
        }
    }
    public static boolean urlCheck(String url){
        if(!url.startsWith("https://")&&!url.startsWith("http://")){
            return false;
        }
        String regex =  "^(https?://)" +
                "(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*" +  // 域名部分
                "([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])" +  // 顶级域名
                "(:[0-9]+)?(/.*)?$";
        return url.matches(regex);
    }
}
