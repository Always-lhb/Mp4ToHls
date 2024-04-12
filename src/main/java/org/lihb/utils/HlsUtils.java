package org.lihb.utils;

/**
 * @author lihb
 */
public class HlsUtils {

    // hls转换后目标文件位置
    public static final String hlsEncDir = "/home/shared/hls/";

    // 获取密钥的uri
    public static final String keyUri = "http://127.0.0.1:8900/api/admin/key";



    public static String buildKeyFilepath() {
        return hlsEncDir + "enc.key";
    }

    public static String buildKeyinfoFilepath() {
        return hlsEncDir + "enc.keyinfo";
    }

    public static String buildTSFilepath() {
        return hlsEncDir + "%3d.ts";
    }

    public static String buildM3U8Filepath() {
        return hlsEncDir + "result.m3u8";
    }

    public static String buildTmpFilepath() {
        return hlsEncDir + "result.tmp";
    }
}
