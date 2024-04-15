package org.lihb.controller;

import org.lihb.data.Token;
import org.lihb.data.TokenUtils;
import org.lihb.service.HlsConvertService;
import org.lihb.service.HlsDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 测试流程：
 * 1. 调用convert接口，把源mp4文件转hls。会生成enc.key, enc.keyinfo, .m3u8, .ts 文件
 * 2. 请求token接口，获取token
 * 3. 请求m3u8文件，要求用户携带token，处理过程中会动态修改m3u8文件，把token写入获取密钥的uri
 * 4. 浏览器/播放器会自动解析m3u8文件，循环请求获取密钥和ts文件，并利用密钥解析ts文件进行播放
 *
 * @author lihb
 */
@Controller
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);

    // token过期时间，24小时
    private static final int EXPIRE = 24 * 60 * 60 * 1000;

    @Autowired
    private HlsConvertService hlsConvertService;

    @Autowired
    private HlsDownloadService hlsDownloadService;


    /**
     * 把mp4文件转换成hls
     *
     * @param mp4FilePath 源mp4文件路径
     * @return
     */
    @RequestMapping(value = "/convert", method = RequestMethod.GET)
    @ResponseBody
    public String convert(@RequestParam(value = "mp4FilePath") String mp4FilePath) {
        hlsConvertService.convert(mp4FilePath);
        return "";
    }

    /**
     * 用户请求获取token接口
     *
     * @param userId   用户id
     * @param request  the req
     * @param response the resp
     * @return the generated token
     */
    @RequestMapping(value = "/token", method = RequestMethod.GET)
    @ResponseBody
    public String getToken(
            @RequestParam(value = "user_id") int userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        // todo 权限用户校验，校验不通过直接返回失败

        // 生成token并返回
        Token episodeToken = new Token();
        episodeToken.setUserId(userId);
        episodeToken.setCreatedTime(System.currentTimeMillis());

        return TokenUtils.encrypt(episodeToken);
    }

    /**
     * 下载m3u8文件接口，要求用户携带token，本接口处理过程中会动态修改m3u8文件，把token写入获取密钥的uri
     *
     * @param hlsToken 用户携带的token
     * @param request  the req
     * @param response the resp
     */
    @ResponseBody
    @RequestMapping(value = "/download/m3u8")
    public void downloadM3U8(
            @RequestParam(value = "hlsToken") String hlsToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Disposition", " attachment; filename=result.m3u8");
        try {
            byte[] bytes = hlsDownloadService.getM3U8FileWithToken(hlsToken);
            if (bytes != null) {
                response.getOutputStream().write(bytes);
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            LOG.error("found exception to get m3u8 file, token: {}, error: {}",
                    hlsToken, e.getMessage());
        }
    }

    /**
     * 获取密钥接口，由于获取m3u8文件时，我们对m3u8文件做了动态修改，因此浏览器/播放器请求本接口时会自动携带token
     *
     * @param hlsToken 播放器携带的token
     * @param response the resp
     * @return 校验token成功则返回密钥字节数组，校验token失败则返回空
     */
    @RequestMapping(value = "/key", method = RequestMethod.GET)
    @ResponseBody
    public byte[] getKey(
            @RequestParam(value = "hlsToken") String hlsToken,
            HttpServletResponse response) {
        Token token;
        try {
            token = TokenUtils.decrypt(hlsToken);
        } catch (Exception e) {
            // token非法，直接返回
            LOG.warn("failed to decrypt hlsToken: {}", hlsToken);
            return null;
        }
        int userId = token.getUserId();
        long createdTime = token.getCreatedTime();

        // token过期，直接返回
        if (System.currentTimeMillis() - createdTime > EXPIRE) {
            LOG.warn("hlsToken expired, token: {}", hlsToken);
            return null;
        }

        LOG.info("get m3u8 key, hlsToken:{}, userId: {}, createdTime:{}", hlsToken, userId,
                createdTime);
        try {
            return hlsDownloadService.getEncKeyFile();
        } catch (Exception e) {
            LOG.error("found exception to get key file, token: {}, error: {}",
                    token.toRawString(), e.getMessage());
            return null;
        }
    }

    /**
     * 下载ts文件接口
     *
     * @param tsFileName ts文件名
     * @param response   the resp
     */
    @ResponseBody
    @RequestMapping(value = "/download/{tsFileName}")
    public void downloadTS(
            @PathVariable("tsFileName") String tsFileName,
            HttpServletResponse response) {
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Disposition", " attachment; filename=" + tsFileName);
        try {
            byte[] bytes = hlsDownloadService.getTsFile(tsFileName);
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            LOG.error("found exception to get ts file: {}, error: {}", tsFileName, e.getMessage());
        }
    }
}
