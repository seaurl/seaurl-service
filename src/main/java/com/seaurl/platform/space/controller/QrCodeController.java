package com.seaurl.platform.space.controller;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.QrCodeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

@RestController
@RequestMapping("/space")

public class QrCodeController {

    private Logger log = LoggerFactory.getLogger(getClass());


    /**
     * 给二维码图片添加边框提高解析
     * @param image
     * @param borderWidth
     * @return
     */
    public static BufferedImage addBorder(BufferedImage image, int borderWidth) {
        int width = image.getWidth() + 2 * borderWidth;
        int height = image.getHeight() + 2 * borderWidth;
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = newImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.drawImage(image, borderWidth, borderWidth, null);
        g.dispose();

        return newImage;
    }

    //        原文链接：https://blog.csdn.net/qq_37699336/article/details/137583974

    /**
     * 上传二维码图片并解析出内容
     */
    @PostMapping(value = "/crud/qrcode/uploadAndRead")
    public Mono<ResponseEntity<?>> uploadFileAndRead(@RequestPart("file") Mono<FilePart> file) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.AUTH_ERROR)));
        }
        return file.flatMap(fp -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return fp.content().collectList().flatMap(dataBuffers -> {

                try {
                    dataBuffers.forEach(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        baos.write(bytes, 0, bytes.length);
                    });
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(baos.toByteArray());
                    return Mono.fromCallable(() -> {
                        BufferedImage bufferedImage = ImageIO.read(inputStream);


                        BufferedImage bufferedImage2 = addBorder(bufferedImage, 20);

                        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage2);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                        Result result = new MultiFormatReader().decode(bitmap);
                        // 这里可以处理 BufferedImage，例如保存或处理图像
                        return ResponseEntity.ok(new ResultSuccess<>(result));
                    }).subscribeOn(Schedulers.boundedElastic());
                } catch (Exception ex) {
                    log.info("uploadFileAndRead ex={}", ex.getMessage());
                    return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception)));
                }
            });
        });

    }

    /**
     * 根据url下载图片并转换成InputStream类型
     *
     * @param imageUrl
     * @return
     * @throws IOException
     */
    public URLConnection downloadImageAsStream(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        URLConnection connection = url.openConnection();

        // 设置超时，防止网络延迟
        connection.setConnectTimeout(5000); // 5秒
        connection.setReadTimeout(5000); // 5秒
        // 获取输入流
        return connection;
    }

    /**
     * 根据二维码图片地址解析内容
     * @param url
     * @return
     */
    @PostMapping(value = "/crud/qrcode/uploadFileAndReadByUrl")
    public ResponseEntity<?> uploadFileAndReadByUrl(@RequestBody QrCodeRequest qrCodeRequest) {

        String url = qrCodeRequest.getUrl();
        if (!StringUtils.hasLength(url)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            URLConnection conn = downloadImageAsStream(url);
            InputStream inputStream = conn.getInputStream();
            BufferedImage bufferedImage = ImageIO.read(inputStream);
            BufferedImage bufferedImage2 = addBorder(bufferedImage, 20);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage2);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (IOException e) {
            log.info("Could not read QR Code, IOException :: " + e.getMessage());
            return  ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        } catch (NotFoundException e) {
            log.info("QR Code not found, NotFoundException :: " + e.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 上传中间小图标
     * 将file转换成base64
     * (没用到，不过后面有这个功能，可以用)
     *
     * @param file
     * @return
     */
    @PostMapping(value = "/crud/qrcode/uploadIcon")
    public Mono<String> uploadIcon(@RequestPart("upload") FilePart file) {
        return file.content()
                .flatMap(dataBuffer -> {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    try {
                        byteArrayOutputStream.write(dataBuffer.asByteBuffer().array());
                    } catch (IOException e) {
                        log.info("QR Code uploadIcon exception :: " + e.getMessage());
                        return Mono.error(e);
                    }
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    String base64String = Base64.getEncoder().encodeToString(byteArray);
                    return Mono.just(base64String);
                })
                .reduce((partialBase64String, newBase64String) -> partialBase64String + newBase64String)
                .doOnError(e -> e.printStackTrace());
    }
}
