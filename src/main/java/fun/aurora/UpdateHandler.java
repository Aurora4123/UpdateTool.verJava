package fun.aurora;

import fun.aurora.utils.DownloadUtil;
import fun.aurora.utils.FileUtil;
import fun.aurora.utils.JSONUtil;
import fun.aurora.utils.json.DownloadItem;
import fun.aurora.utils.json.UpdateList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateHandler {
    private static UpdateList updateList;
    private static final Logger logger = LoggerFactory.getLogger(UpdateHandler.class);
    private static Path pathPrefix;
    static {

        try {
            Path jarPath = Paths.get(UpdateAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            pathPrefix = jarPath;
            updateList = JSONUtil.parseUpdateList(jarPath.resolve("update.json").toFile());
        } catch (URISyntaxException e) {
            logger.error("无法获取Jar路径");
        }

    }
    public static void downloadUpdate(){
        boolean success = true;
        if(updateList == null){
            logger.error("无法获取更新列表");
            System.exit(5);
        }
        for(DownloadItem item : updateList.download) {
            int result = DownloadUtil.downloadToPath(item.url, item.path, item.sha256);
            if(result != 0) {
                success = false;
            }
        }
        if(!success) {
            logger.warn("更新完成，但是有错误");
        } else {
            logger.info("更新完成！");
        }
    }
    public static void copyUpdate() throws IOException {
        if (updateList.copy == null) {
            logger.warn("没有需要复制的文件!");
            return;
        }
        for(fun.aurora.utils.json.CopyItem item : updateList.copy) {
            FileUtil.copyFile(pathPrefix.resolve(item.file).toString(), pathPrefix.resolve(item.path).toString());
        }
    }
    public static void deleteUpdate() {
        if (updateList.delete == null) {
            logger.warn("没有需要删除的文件!");
            return;
        }
        for(fun.aurora.utils.json.DeleteItem item : updateList.delete) {
            FileUtil.deleteFile(pathPrefix.resolve(item.deletefile).toString());
        }
    }
}
