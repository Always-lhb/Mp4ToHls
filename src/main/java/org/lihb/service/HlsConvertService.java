package org.lihb.service;

import org.lihb.data.FfmpegCmd;
import org.lihb.utils.AESUtils;
import org.lihb.utils.HlsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;

/**
 * hls转换服务，负责把mp4转换成hls格式
 *
 * @author lihb
 */
@Service
public class HlsConvertService {

    private static final Logger LOG = LoggerFactory.getLogger(HlsConvertService.class);

    private static final int CODE_SUCCESS = 0;
    private static final int CODE_FAILED = -1;


    @PostConstruct
    public void init() {
        File encFilePathDir = new File(HlsUtils.hlsEncDir);
        if (!encFilePathDir.exists()) {
            boolean res = encFilePathDir.mkdir();
            if (res) {
                LOG.info("succeed to create hls enc dir: {}", HlsUtils.hlsEncDir);
            }
        }
    }

    /**
     * 入口方法，将某个mp4文件进行hls转换
     *
     * @param mp4FilePath 源mp4文件路径
     */
    public void convert(String mp4FilePath) {
        // 检查源mp4文件必须存在
        File mp4File = new File(mp4FilePath);
        if (!mp4File.exists()) {
            LOG.warn("mp4 file is not exist. m4pFilepath: {}", mp4FilePath);
            return;
        }

        // 生成密钥文件enc.key
        try {
            String keyFilePath = generateKeyFile();
            LOG.info("succeed to generate key file: {}", keyFilePath);
        } catch (IOException e) {
            LOG.error("found exception to generate key file. error: {}", e.getMessage());
            return;
        }

        // 生成密钥信息文件enc.keyinfo
        try {
            String keyinfoFilePath = generateKeyinfoFile();
            LOG.info("succeed to generate keyinfo file: {}", keyinfoFilePath);
        } catch (IOException e) {
            LOG.error("found exception to generate keyinfo file. error: {}", e.getMessage());
            return;
        }

        // 执行ffmpeg命令，生成.m3u8文件和.ts文件
        int code = exec(buildCmd(mp4FilePath));
        if (CODE_SUCCESS != code) {
            LOG.error("failed to enc, code: {}", code);
            return;
        }
        LOG.info("succeed to enc mp4 file: {}", mp4FilePath);
    }

    private String generateKeyFile() throws IOException {
        String keyFilePath = HlsUtils.buildKeyFilepath();
        File keyFile = new File(keyFilePath);
        if (keyFile.exists()) {
            keyFile.delete();
        }

        try (BufferedOutputStream outputStream =
                     new BufferedOutputStream(new FileOutputStream(keyFile))) {
            // 生成aes128密钥
            byte[] key = AESUtils.genAES128Key();
            outputStream.write(key);
            outputStream.flush();
        }
        return keyFilePath;
    }

    private String generateKeyinfoFile() throws IOException {
        String keyInfoFilePath = HlsUtils.buildKeyinfoFilepath();
        File keyinfoFile = new File(keyInfoFilePath);
        if (keyinfoFile.exists()) {
            keyinfoFile.delete();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(keyinfoFile))) {
            // 第一行写入：密匙URI
            writer.write(HlsUtils.keyUri);
            writer.newLine();
            // 第二行写入：密钥文件地址
            writer.write(HlsUtils.buildKeyFilepath());
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

            // 打印过程
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
            // 关闭资源
            ffmpegCmd.close();
        }
        return code;
    }

    /**
     * 拼接ffmpeg命令
     *
     * @param mp4FilePath 源mp4文件路径
     * @return the generated ffmpeg cmd
     */
    private String buildCmd(String mp4FilePath) {
        StringBuilder stringBuilder = new StringBuilder(" -y");
        stringBuilder.append(" -i ").append(mp4FilePath)
                .append(" -hls_time 10")
                .append(" -hls_key_info_file ").append(HlsUtils.buildKeyinfoFilepath())
                .append(" -hls_playlist_type vod")
                .append(" -hls_segment_filename ").append(HlsUtils.buildTSFilepath())
                .append(" ").append(HlsUtils.buildM3U8Filepath());
        return stringBuilder.toString();
    }
}
