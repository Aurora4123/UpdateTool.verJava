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

public class UpdateHandler {
    private static final UpdateList updateList;
    private static final Logger logger = LoggerFactory.getLogger(UpdateHandler.class);
    static {
        updateList = JSONUtil.parseUpdateList(new File("update.json"));
    }
    public static void downloadUpdate() {
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
        for(fun.aurora.utils.json.CopyItem item : updateList.copy) {
            FileUtil.copyFile(item.file, item.path);
        }
    }
    public static void deleteUpdate() {
        for(fun.aurora.utils.json.DeleteItem item : updateList.delete) {
            FileUtil.deleteFile(item.deletefile);
        }
    }
}
