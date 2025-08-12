package fun.aurora.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;


public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    public static void deleteFile(String filePath){
        File file = new File(filePath);
        logger.info("尝试删除文件{}", filePath);
        if(file.exists()) {
            if(file.delete()) {
                logger.info("删除文件成功: {}", filePath);
            } else {
                logger.warn("无法删除当前文件！");
            }
        } else {
            logger.warn("文件不存在！");
        }

    }

    public static void deleteFile(File file){
        deleteFile(file.getAbsolutePath());
    }
    public static void copyFile(String srcFilePath, String destFilePath) throws IOException {
        File srcFile = new File(srcFilePath);
        File destFile = new File(destFilePath);
        logger.info("尝试复制文件{} -> {}", srcFilePath, destFilePath);
        if(srcFile.exists()) {
            destFile.getParentFile().mkdirs();
            try(FileChannel src = new FileInputStream(srcFile).getChannel();
                FileChannel dest = new FileOutputStream(destFile).getChannel()
            ) {
                dest.transferFrom(src, 0, src.size());
            }
        } else {
            logger.warn("源文件不存在！");
        }

    }
}
