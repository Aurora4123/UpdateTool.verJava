package fun.aurora.utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;


public class DownloadUtil {
    private static final Logger logger = LoggerFactory.getLogger(DownloadUtil.class);
    @Deprecated
    public static int download(String url, String fileName) {
        try{
            Path jarDir = Paths.get(DownloadUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            System.setProperty("JAR_DIR", jarDir.toAbsolutePath().toString());
            logger.info("当前工作目录为：{}", jarDir);
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

    // 改进的支持智能更新检查的下载方法
    public static int downloadWithIntelligentCheck(String url, String fileName) {
        try{
            Path jarDir = Paths.get(DownloadUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            System.setProperty("JAR_DIR", jarDir.toAbsolutePath().toString());
            logger.info("当前工作目录为：{}", jarDir);
            File saveFile = jarDir.resolve(fileName).toFile();
            File metaFile = jarDir.resolve(fileName + ".meta").toFile();
            
            return downloadWithIntelligentCheckToPath(url, saveFile, metaFile);
        } catch (Exception e) {
            logger.error("下载过程中发生错误: ", e);
            return -1;
        }
    }
    
    // 可指定下载路径的智能更新检查下载方法
    public static int downloadWithIntelligentCheckToPath(String url, File saveFile, File metaFile) {
        LocalFileInfo localInfo = null;
        try{
            logger.info("目标文件路径: {}", saveFile.getAbsolutePath());
            
            // 获取远程文件信息
            RemoteFileInfo remoteInfo = getRemoteFileInfo(url);
            if (remoteInfo == null) {
                logger.error("无法获取远程文件信息");
                return -1;
            }
            
            // 检查本地文件是否存在
            if (saveFile.exists()) {
                // 计算现有文件的SHA256值
                String currentSha256 = calculateSHA256(saveFile);
                if (currentSha256 == null) {
                    logger.error("无法计算本地文件的SHA256值");
                    return -1;
                }
                
                // 检查是否存在元数据文件
                if (metaFile.exists()) {
                    localInfo = readLocalFileInfo(metaFile);
                    if (localInfo != null) {
                        boolean shouldSkip = false;
                        
                        // 首先尝试通过时间戳判断
                        if (remoteInfo.lastModified > 0 && localInfo.lastModified > 0) {
                            if (localInfo.lastModified >= remoteInfo.lastModified) {
                                if(currentSha256.equalsIgnoreCase(localInfo.sha256)){
                                    shouldSkip = true;
                                    logger.info("根据最后修改时间判断，文件未更新，且本地文件未被更改，跳过下载");
                                } else {
                                    logger.info("根据最后修改时间判断，文件未更新，但本地文件已被更改，需要重新下载");
                                }


                            } else {
                                logger.info("根据最后修改时间判断，文件已更新");
                            }
                        }
                        
                        // 如果时间戳无法判断或者判断结果为需要更新，则通过SHA256比较
                        if (!shouldSkip && localInfo.sha256 != null) {
                            if (currentSha256.equalsIgnoreCase(localInfo.sha256)) {
                                shouldSkip = true;
                                logger.info("根据SHA256校验，文件未更新，跳过下载");
                            } else {
                                logger.info("根据SHA256校验，文件已更新或本地文件损坏");
                            }
                        }



                        if (shouldSkip) {
                            return 0;
                        }
                    }
                } else {
                    // 没有元数据文件，这是初次使用智能检查
                    // 我们可以尝试通过远程文件大小和修改时间进行粗略判断
                    // 或者直接重新下载以确保文件是最新的
                    logger.info("未找到元数据文件，将重新下载以确保文件是最新版本");
                }
            }
            
            // 确保目标目录存在
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    logger.error("无法创建目标目录: {}", parentDir.getAbsolutePath());
                    return -1;
                }
            }
            
            // 下载文件前确保目标文件不存在或删除已存在的文件
            if (saveFile.exists()) {
                logger.info("删除已存在的文件: {}", saveFile.getName());
                if (!saveFile.delete()) {
                    logger.warn("无法删除已存在的文件: {}", saveFile.getName());
                }
            }
            
            // 下载文件
            logger.info("开始下载文件...");
//            downloadFromURL(url, saveFile);
//
//            // 下载完成后计算SHA256值并保存元数据
//            String newSha256 = calculateSHA256(saveFile);
//            if (newSha256 != null) {
//                // 确保元数据文件的目录也存在
//                File metaParentDir = metaFile.getParentFile();
//                if (metaParentDir != null && !metaParentDir.exists()) {
//                    metaParentDir.mkdirs();
//                }
//
//                writeLocalFileInfo(metaFile, new LocalFileInfo(remoteInfo.lastModified, newSha256));
//                logger.info("文件下载完成，元数据已保存");
//            } else {
//                logger.error("计算下载文件的SHA256值失败");
//                return -1;
//            }
            if (localInfo != null) {
                downloadWithSha256Verification(url, saveFile, metaFile, localInfo.sha256);
            }

        } catch (Exception e) {
            logger.error("下载过程中发生错误: ", e);
            return -1;
        }
        return 0;
    }
    
    // 带SHA256校验的下载方法，下载完成后进行sha256校验失败后直接删除文件，并跳过该文件
    public static int downloadWithSha256Verification(String url, File saveFile, File metaFile, String expectedSha256) {
        try {
            logger.info("目标文件路径: {}", saveFile.getAbsolutePath());
            
            // 获取远程文件信息
            RemoteFileInfo remoteInfo = getRemoteFileInfo(url);
            if (remoteInfo == null) {
                logger.error("无法获取远程文件信息");
                return -1;
            }
            
            // 检查本地文件是否存在
            if (saveFile.exists()) {
                // 使用verifyFileWithSha256校验文件
                if (verifyFileWithSha256(saveFile, expectedSha256)) {
                    // 文件校验通过，检查元数据
                    if (metaFile.exists()) {
                        LocalFileInfo localInfo = readLocalFileInfo(metaFile);
                        if (localInfo != null && localInfo.sha256 != null && 
                            localInfo.sha256.equalsIgnoreCase(expectedSha256)) {
                            logger.info("文件和元数据SHA256校验均通过，跳过下载");
                            return 0;
                        }
                    }
                    
                    // 文件校验通过但元数据不匹配或不存在
                    logger.info("文件SHA256校验通过，但元数据不存在或不匹配");
                } else {
                    // 文件校验失败，删除文件并跳过
                    logger.info("文件SHA256校验失败，删除文件并跳过下载");
                    if (!saveFile.delete()) {
                        logger.warn("无法删除校验失败的文件: {}", saveFile.getName());
                    }
                    return 0; // 跳过下载
                }
            }
            
            // 确保目标目录存在
            File parentDir = saveFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    logger.error("无法创建目标目录: {}", parentDir.getAbsolutePath());
                    return -1;
                }
            }
            
            // 下载文件
            logger.info("开始下载文件...");
            downloadFromURL(url, saveFile);
            
            // 下载完成后校验SHA256
            if (!verifyFileWithSha256(saveFile, expectedSha256)) {
                // 校验失败，删除文件并跳过
                logger.error("下载完成后SHA256校验失败，删除文件并跳过");
                if (!saveFile.delete()) {
                    logger.warn("无法删除校验失败的文件: {}", saveFile.getName());
                }
                return 0; // 跳过下载（虽然文件已被删除，但我们视为"跳过"而不是"失败"）
            }
            
            // 保存元数据
            // 确保元数据文件的目录也存在
            File metaParentDir = metaFile.getParentFile();
            if (metaParentDir != null && !metaParentDir.exists()) {
                metaParentDir.mkdirs();
            }
            
            writeLocalFileInfo(metaFile, new LocalFileInfo(remoteInfo.lastModified, expectedSha256));
            logger.info("文件下载完成且SHA256校验通过，元数据已保存");
            
        } catch (Exception e) {
            logger.error("下载过程中发生错误: ", e);
            return -1;
        }
        logger.info("下载完成！");
        return 0;
    }
    
    // 通过传入的SHA256与本地文件进行比对的方法
    public static boolean verifyFileWithSha256(File file, String expectedSha256) {
        if (!file.exists()) {
            logger.warn("文件不存在: {}", file.getAbsolutePath());
            return false;
        }
        
        String actualSha256 = calculateSHA256(file);
        if (actualSha256 == null) {
            logger.error("无法计算文件的SHA256值: {}", file.getAbsolutePath());
            return false;
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

//    // 通过传入的SHA256与本地元数据进行比对的方法
//    public static CheckResult verifyFileWithSha256AndMetadata(File saveFile, File metaFile, String expectedSha256) {
//        // 首先检查文件是否存在
//        if (!saveFile.exists()) {
//            logger.warn("文件不存在: {}", saveFile.getAbsolutePath());
//            return new CheckResult(false, false, "文件不存在", null);
//        }
//
//        // 计算文件实际SHA256值
//        String actualSha256 = calculateSHA256(saveFile);
//        if (actualSha256 == null) {
//            logger.error("无法计算文件的SHA256值: {}", saveFile.getAbsolutePath());
//            return new CheckResult(false, false, "无法计算文件SHA256值", null);
//        }
//
//        // 与传入的SHA256进行比较
//        boolean fileMatch = actualSha256.equalsIgnoreCase(expectedSha256);
//
//        // 检查元数据是否存在
//        boolean metadataExists = metaFile.exists();
//        boolean metadataMatch = false;
//        LocalFileInfo localInfo = null;
//
//        if (metadataExists) {
//            localInfo = readLocalFileInfo(metaFile);
//            if (localInfo != null && localInfo.sha256 != null) {
//                metadataMatch = localInfo.sha256.equalsIgnoreCase(expectedSha256);
//            }
//        }
//
//        String message;
//        if (fileMatch && metadataMatch) {
//            message = "文件和元数据SHA256校验均通过";
//        } else if (fileMatch) {
//            message = "文件SHA256校验通过，但元数据不存在或不匹配";
//        } else if (metadataMatch) {
//            message = "元数据SHA256校验通过，但文件不匹配";
//        } else {
//            message = "文件和元数据SHA256校验均未通过";
//        }
//
//        logger.info("校验结果 - 文件匹配: {}, 元数据存在: {}, 元数据匹配: {}, 信息: {}",
//            fileMatch, metadataExists, metadataMatch, message);
//
//        return new CheckResult(fileMatch, metadataExists && metadataMatch, message, localInfo);
//    }

    private static void downloadFromURL(String url, File saveFile) throws IOException {
        logger.info("正在启动下载...");
        URL fileURL = new URL(url);
        URLConnection conn = fileURL.openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try(InputStream inputStream = conn.getInputStream();
            FileOutputStream outputStream = new FileOutputStream(saveFile)
        ) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }
    
    // 获取远程文件信息（最后修改时间等）
    private static RemoteFileInfo getRemoteFileInfo(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            long lastModified = connection.getLastModified();
            String eTag = connection.getHeaderField("ETag");
            int contentLength = connection.getContentLength();
            
            connection.disconnect();
            
            logger.info("远程文件信息 - 最后修改时间: {}, ETag: {}, 大小: {} bytes", 
                lastModified > 0 ? new Date(lastModified) : "未知", 
                eTag != null ? eTag : "未知",
                contentLength > 0 ? contentLength : "未知");
            
            return new RemoteFileInfo(lastModified, eTag, contentLength);
        } catch (Exception e) {
            logger.error("获取远程文件信息失败: " + e.getMessage());
            return new RemoteFileInfo(0, null, -1);
        }
    }
    
    // 计算文件的SHA256值
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
            logger.error("计算文件SHA256时出错: " + e.getMessage());
            return null;
        }
    }
    
    // 从文件读取本地文件信息
    private static LocalFileInfo readLocalFileInfo(File metaFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    long lastModified = Long.parseLong(parts[0]);
                    String sha256 = parts[1];
                    return new LocalFileInfo(lastModified, sha256);
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.error("读取本地文件信息失败: " + e.getMessage());
        }
        return null;
    }
    
    // 将文件信息写入元数据文件
    private static void writeLocalFileInfo(File metaFile, LocalFileInfo info) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metaFile))) {
            writer.write(info.lastModified + "|" + info.sha256);
        } catch (IOException e) {
            logger.error("写入本地文件信息失败: " + e.getMessage());
        }
    }
    
    // 校验结果类
//    public static class CheckResult {
//        public final boolean fileMatch;
//        public final boolean metadataMatch;
//        public final String message;
//        public final LocalFileInfo localInfo;
//
//        public CheckResult(boolean fileMatch, boolean metadataMatch, String message, LocalFileInfo localInfo) {
//            this.fileMatch = fileMatch;
//            this.metadataMatch = metadataMatch;
//            this.message = message;
//            this.localInfo = localInfo;
//        }
//    }

    // 远程文件信息类
    private static class RemoteFileInfo {
        final long lastModified;
        final String eTag;
        final int contentLength;
        
        RemoteFileInfo(long lastModified, String eTag, int contentLength) {
            this.lastModified = lastModified;
            this.eTag = eTag;
            this.contentLength = contentLength;
        }
    }
    
    // 本地文件信息类
    private static class LocalFileInfo {
        final long lastModified;
        final String sha256;
        
        LocalFileInfo(long lastModified, String sha256) {
            this.lastModified = lastModified;
            this.sha256 = sha256;
        }
    }
}
