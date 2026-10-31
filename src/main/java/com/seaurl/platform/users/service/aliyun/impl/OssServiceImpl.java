package com.seaurl.platform.users.service.aliyun.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.users.service.aliyun.OssService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Service("usersOssServiceImpl")
public class OssServiceImpl implements OssService {
    private Logger log = LoggerFactory.getLogger(OssService.class);


    @Autowired
    AliyunConfig aliyunOssConfig;

    /**
     * 上传文件到OSS指定目录中
     *
     * @param base64Img
     * @param folder
     * @throws IOException
     */
    public String upload(String base64Img, String folder) throws IOException {
        base64Img = base64Img.split(",")[1];
        //将生成的base64码反编译回二进制字节码
//        BASE64Decoder decoder = new BASE64Decoder();
//        byte[] file = decoder.decodeBuffer(base64Img);


        // 新的解码格式，上面是老的
        byte[] file = Base64.getDecoder().decode(base64Img);
        ObjectMetadata objectMeta = new ObjectMetadata();
        objectMeta.setContentLength(file.length);

        //将字节码转换成流
        InputStream input = new ByteArrayInputStream(file);

        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 创建PutObjectRequest对象。
        // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, folder, input, objectMeta);

        // 上传字符串。
        ossClient.putObject(putObjectRequest);

        // 关闭OSSClient。
        ossClient.shutdown();

        return aliyunOssConfig.getOssBucketCdnHost() + "/" + folder;
    }

    /**
     *
     * @param sourceFilePath
     * @param targetFilePath
     * @return
     * @throws IOException
     */
    @Override
    public Boolean copyTo(String sourceFilePath, String targetFilePath){
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 拷贝文件。
            CopyObjectResult result = ossClient.copyObject(bucket, sourceFilePath, bucket, targetFilePath);
            log.info("ETag:{}", result.getETag());
            log.info("LastModified:{}", result.getLastModified());
            return true;
        } catch (OSSException oe) {
            log.info("Caught an OSSException, which means your request made it to OSS,but was rejected with an error response for some reason.");
            log.info("Error Message:{}", oe.getErrorMessage());
            log.info("Error Code:{}", oe.getErrorCode());
            log.info("Request ID:{}", oe.getRequestId());
            log.info("Host ID:{}", oe.getHostId());
        } catch (ClientException ce) {
            log.info("Caught an ClientException, which means the client encountered a serious internal problem while trying to communicate with OSS, such as not being able to access the network.");
            log.info("Error Message:{}", ce.getErrorMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return false;
    }
}
