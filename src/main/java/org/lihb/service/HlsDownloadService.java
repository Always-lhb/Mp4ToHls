package org.lihb.service;

import org.apache.commons.io.FileUtils;
import org.lihb.utils.HlsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * hls下载服务，用于下载hls相关文件
 *
 * @author lihb
 */
@Service
public class HlsDownloadService {

    private static final Logger LOG = LoggerFactory.getLogger(HlsDownloadService.class);

    private static final String KEY_PREFIX = "#EXT-X-KEY";


    @PostConstruct
    public void init() {
        File tmpFilePathDir = new File(HlsUtils.hlsEncDir);
        if (!tmpFilePathDir.exists()) {
            boolean res = tmpFilePathDir.mkdir();
            if (res) {
                LOG.info("succeed to create tmp file dir: {}", HlsUtils.hlsEncDir);
            }
        }
    }

    public byte[] getM3U8FileWithToken(String token) throws IOException {
        String m3u8Filepath = HlsUtils.buildM3U8Filepath();
        File m3u8File = new File(m3u8Filepath);

        List<String> lines = new ArrayList<>();
        for (String line : FileUtils.readLines(m3u8File)) {
            if (line.startsWith(KEY_PREFIX)) {
                // 动态修改m3u8文件，把token加入到获取密钥的uri
                String keyUriWithToken = HlsUtils.keyUri + "?hlsToken=" + token;
                line = line.replace(HlsUtils.keyUri, keyUriWithToken);
            }
            lines.add(line);
        }

        // 临时文件，存储动态修改后的m3u8文件
        String tmpFilepath = HlsUtils.buildTmpFilepath();
        File tmpFile = new File(tmpFilepath);
        FileUtils.writeLines(tmpFile, lines);

        byte[] result = FileUtils.readFileToByteArray(tmpFile);

        FileUtils.deleteQuietly(tmpFile);
        return result;
    }

    public byte[] getEncKeyFile() {
        String keyFilePath = HlsUtils.buildKeyFilepath();
        try {
            return FileUtils.readFileToByteArray(new File(keyFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getTsFile(String tsFileName) {
        String tsFilePath = HlsUtils.hlsEncDir + tsFileName;
        try {
            return FileUtils.readFileToByteArray(new File(tsFilePath));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
