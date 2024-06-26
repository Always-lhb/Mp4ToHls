# Mp4ToHls
springboot2项目，实现mp4转hls功能。

## 目的
mp4视频被下载后，用户可以进行随意的传播，我们无法对这种非法传播进行控制。
因此，可以将mp4转换成hls，通过有过期时间的token对其进行权限控制。

## 原理
使用 **ffmpeg** 自定义的cmd命令实现mp4转hls，依赖ffmpeg项目的java包装器[jave2](https://github.com/a-schild/jave2)

## 测试流程：
1. 运行`org.lihb.Application`，在本地8900端口启动一个http server
2. 访问 `http://127.0.0.1:8900/api/admin/convert?mp4FilePath=your_mp4_file_path.mp4`，触发mp4转hls任务，
任务执行完成后会在输出目录生成一系列文件，包括：`enc.key`, `enc.keyinfo`, `result.m3u8`, `000.ts` 等
3. 访问`http://127.0.0.1:8900/api/admin/token?user_id=123`，即可为 `用户123` 生成一个有效期为24小时的token
4. 更新resources目录下的`player.html`文件，把上一步生成的token替换进m3u8文件的下载地址
5. 使用浏览器打开resources目录下的`player.html`文件，浏览器会自动请求并解析m3u8文件，然后循环获取密钥和ts文件，并利用密钥解析ts文件进行播放