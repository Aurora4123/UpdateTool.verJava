package fun.aurora.utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class DownloadUtil {
    private static final Logger logger = LoggerFactory.getLogger(DownloadUtil.class);

    //指定文件名并下载到默认路径
    public static int download(String url, String fileName) {
        try{
            Path jarDir = Paths.get(DownloadUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File save = jarDir.resolve(fileName).toFile();
            downloadFromURL(url, save);
        } catch (Exception e) {
            if(e instanceof URISyntaxException) {
                logger.error("当前工作目录获取失败！");
            }
            if(e instanceof IOException) {
                logger.error("文件下载失败，请检查网络连接");
            }
            return -1;
        }
        logger.info("下载完成！");
        return 0;
    }

    // 从URL中提取文件名并下载的新方法
    public static int downloadFromUrlWithAutoFilename(String url) {
        try {
            String fileName = extractFileNameFromUrl(url);
            if (fileName == null || fileName.isEmpty()) {
                logger.error("无法从URL中提取文件名: {}", url);
                return -1;
            } else {
                logger.info("下载的文件名：{}", fileName);
            }
            return download(url, fileName);
        } catch (Exception e) {
            logger.error("从URL自动提取文件名并下载时发生错误: ", e);
            return -1;
        }
    }

    // 支持指定完整文件路径的下载方法
    public static int downloadToPath(String url, String filePath, String expectedSha256) {
        try {
            Path jarDir = Paths.get(DownloadUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File saveFile = jarDir.resolve(filePath).toFile();
            // 确保目标目录存在
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    logger.error("无法创建目标目录: {}", parentDir.getAbsolutePath());
                    return -1;
                }
            }
            if(saveFile.exists()){
                logger.info("文件{}已存在，正在检查文件sha256", saveFile.getAbsolutePath());
                if(verifyFileWithSha256(saveFile, expectedSha256)){
                    logger.info("文件{}已存在，无需下载，跳过", saveFile.getAbsolutePath());
                    return 0;
                } else {
                    logger.info("文件已存在，但SHA256不匹配，正在重新下载");
                }
            }
            downloadFromURL(url, saveFile);
            logger.info("文件已下载到: {}", filePath);
            logger.info("正在检查文件sha256");
            if (!verifyFileWithSha256(saveFile, expectedSha256)) {
                logger.warn("文件SHA256校验失败！");
                if (!saveFile.delete()) {
                    logger.warn("无法删除文件: {}", saveFile.getAbsolutePath());
                }
                return -1;
            }
            return 0;
        } catch (Exception e) {
            logger.error("下载文件到指定路径时发生错误: ", e);
            return -1;
        }
    }

    // 从URL中提取文件名的辅助方法
    private static String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        try {
            URL urlObj = new URL(url);
            String path = urlObj.getPath();
            
            // 获取路径中最后一个斜杠后的部分作为文件名
            int lastSlashIndex = path.lastIndexOf('/');
            if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
                return path.substring(lastSlashIndex + 1);
            }
            
            // 如果没有斜杠或路径为空，返回null
            return null;
        } catch (Exception e) {
            logger.error("解析URL时出错: ", e);
            return null;
        }
    }

    public static boolean verifyFileWithSha256(File file, String expectedSha256) {
        if (!file.exists()) {
            logger.warn("文件不存在: {}", file.getAbsolutePath());
            return false;
        }
        
        String actualSha256 = calculateSHA256(file);
        if (actualSha256 == null) {
            logger.error("无法计算文件的SHA256值: {}", file.getAbsolutePath());
            return false;
        } else {
            logger.info("SHA256: {}", actualSha256);
        }
        
        boolean match = actualSha256.equalsIgnoreCase(expectedSha256);
        if (match) {
            logger.info("文件SHA256校验成功: {}", file.getAbsolutePath());
        } else {
            logger.warn("文件SHA256校验失败: {}，期望值: {}，实际值: {}", 
                file.getAbsolutePath(), expectedSha256, actualSha256);
        }
        
        return match;
    }

    private static void downloadFromURL(String url, File saveFile) throws IOException {
        logger.info("下载文件:{}...", saveFile.getName());
        URL fileURL = new URL(url);
        logger.info("正在连接到下载地址: {}", url);
        URLConnection conn = fileURL.openConnection();
        conn.setUseCaches(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try(InputStream inputStream = conn.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(saveFile)
        ) {
            logger.info("正在下载文件: {}", saveFile.getName());
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
    
    // 获取远程文件信息（最后修改时间等）
    private static String calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("计算文件SHA256时出错: {}", e.getMessage());
            return null;
        }
    }
}
