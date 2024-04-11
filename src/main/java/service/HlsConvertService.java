package service;

import data.FfmpegCmd;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.AESUtils;

import javax.annotation.PostConstruct;
import java.io.*;

/**
 * @author lihb
 */
public class HlsConvertService {

    private static final Logger LOG = LoggerFactory.getLogger(HlsConvertService.class);

    private static final String DELIMITER = "/";

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_FAILED = -1;

    // 源mp4文件位置
    private static final String mp4FileDir = "/home/shared/mp4/";

    // hls转换后目标文件位置
    private static final String hlsEncDir = "/home/shared/hls/";

    // 获取密钥的uri
    private static final String keyUri = "http://127.0.0.1:8900/m3u8/key";


    @PostConstruct
    public void init() {
        File encFilePathDir = new File(hlsEncDir);
        if (!encFilePathDir.exists()) {
            boolean res = encFilePathDir.mkdir();
            if (res) {
                LOG.info("succeed to create hls enc dir: {}", hlsEncDir);
            }
        }
    }

    /**
     * 入口方法，将某个课时的mp4进行hls转换
     *
     * @param episodeId 课时id
     */
    public void convert(int episodeId) {
        // 检查源mp4文件必须存在
        String mp4Filepath = buildMp4FilePath(episodeId);
        File mp4File = new File(mp4Filepath);
        if (!mp4File.exists()) {
            LOG.warn("mp4 file is not exist. m4pFilepath: {}", mp4Filepath);
            return;
        }

        // 创建文件输出目录
        if (!mkOutputDir(episodeId)) {
            return;
        }

        // 生成密钥文件enc.key
        try {
            String keyFilePath = generateKeyFile(episodeId);
            LOG.info("succeed to generate key file: {}", keyFilePath);
        } catch (IOException e) {
            LOG.error("found exception to generate key file. episodeId: {}, error: {}", episodeId
                    , e.getMessage());
            return;
        }

        // 生成密钥信息文件enc.keyinfo
        try {
            String keyinfoFilePath = generateKeyinfoFile(episodeId);
            LOG.info("succeed to generate keyinfo file: {}", keyinfoFilePath);
        } catch (IOException e) {
            LOG.error("found exception to generate keyinfo file. episodeId: {}, error: {}",
                    episodeId, e.getMessage());
            return;
        }

        // 执行ffmpeg命令，生成.m3u8文件和.ts文件
        int code = exec(buildCmd(episodeId));
        if (CODE_SUCCESS != code) {
            LOG.error("failed to enc, episodeId: {}, code: {}", episodeId, code);
            return;
        }
        LOG.info("succeed to enc, episodeId: {}", episodeId);
    }

    private boolean mkOutputDir(int episodeId) {
        try {
            File outputDir = new File(buildOutputDir(episodeId));
            FileUtils.deleteQuietly(outputDir);
            outputDir.mkdirs();
        } catch (Throwable throwable) {
            LOG.error("found exception to make output dir, episodeId: {}, error: {}", episodeId,
                    throwable.getMessage());
            return false;
        }
        return true;
    }

    private String generateKeyFile(int episodeId) throws IOException {
        String keyFilePath = buildKeyFilepath(episodeId);
        File keyFile = new File(keyFilePath);
        if (keyFile.exists()) {
            keyFile.delete();
        }

        try (BufferedOutputStream outputStream =
                     new BufferedOutputStream(new FileOutputStream(keyFile))) {
            byte[] key = AESUtils.genAES128Key();
            outputStream.write(key);
            outputStream.flush();
        }
        return keyFilePath;
    }

    private String generateKeyinfoFile(int episodeId) throws IOException {
        String keyInfoFilePath = buildKeyinfoFilepath(episodeId);
        File keyinfoFile = new File(keyInfoFilePath);
        if (keyinfoFile.exists()) {
            keyinfoFile.delete();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(keyinfoFile))) {
            // 密匙URI
            writer.write(keyUri);
            writer.newLine();
            // 密钥文件地址
            writer.write(buildKeyFilepath(episodeId));
            writer.flush();
        }
        return keyInfoFilePath;
    }

    /**
     * 执行ffmpeg命令
     *
     * @param cmd the ffmpeg cmd
     * @return process exit code
     */
    public int exec(String cmd) {
        int code;
        FfmpegCmd ffmpegCmd = new FfmpegCmd();

        InputStream errorStream;
        try {
            ffmpegCmd.execute(false, true, cmd);
            errorStream = ffmpegCmd.getErrorStream();

            StringBuilder sb = new StringBuilder();
            int len;
            while ((len = errorStream.read()) != -1) {
                sb.append((char) len);
            }
            LOG.info(sb.toString());

            code = ffmpegCmd.getProcessExitCode();
        } catch (IOException e) {
            LOG.error("found exception to exec cmd: {}", e.getMessage());
            return CODE_FAILED;
        } finally {
            ffmpegCmd.close();
        }
        return code;
    }

    private String buildMp4FilePath(int episodeId) {
        return mp4FileDir + episodeId + ".mp4";
    }

    private String buildKeyFilepath(int episodeId) {
        return buildOutputDir(episodeId) + DELIMITER + episodeId + "_enc.key";
    }

    private String buildKeyinfoFilepath(int episodeId) {
        return buildOutputDir(episodeId) + DELIMITER + episodeId + "_enc.keyinfo";
    }

    private String buildTSFilepath(int episodeId) {
        return buildOutputDir(episodeId) + DELIMITER + episodeId + "_%3d.ts";
    }

    private String buildM3U8Filepath(int episodeId) {
        return buildOutputDir(episodeId) + DELIMITER + episodeId + ".m3u8";
    }

    private String buildOutputDir(int episodeId) {
        return hlsEncDir + episodeId;
    }

    private String buildCmd(int episodeId) {
        StringBuilder stringBuilder = new StringBuilder(" -y");
        stringBuilder.append(" -i ").append(buildMp4FilePath(episodeId))
                .append(" -hls_time 10")
                .append(" -hls_key_info_file ").append(buildKeyinfoFilepath(episodeId))
                .append(" -hls_playlist_type vod")
                .append(" -hls_segment_filename ").append(buildTSFilepath(episodeId))
                .append(" ").append(buildM3U8Filepath(episodeId));
        return stringBuilder.toString();
    }

    private String buildCosKey(int episodeId, String fileName) {
        return episodeId + DELIMITER + fileName;
    }
}
