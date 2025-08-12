package fun.aurora.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fun.aurora.utils.json.UpdateList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class JSONUtil {
    private static final Logger logger = LoggerFactory.getLogger(JSONUtil.class);
    public static UpdateList parseUpdateList(File jsonFile) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonFile, UpdateList.class);
        } catch (Exception e) {
            logger.error("JSON解析失败: {}", e.getMessage());
            return null;
        }
    }
}
