package com.seaurl.platform.space.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.*;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.seaurl.platform.common.config.AliyunConfig;
import com.seaurl.platform.space.service.OssService;
import com.seaurl.platform.space.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "oss")
public class OssServiceImpl implements OssService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    AliyunConfig aliyunOssConfig;

    /**
     * 上传文件到OSS指定目录中
     *
     * @throws IOException
     */
    public String uploadImgBase64(String base64Img, String fullPath) throws IOException {
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
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fullPath, input, objectMeta);

        // 上传字符串。
        ossClient.putObject(putObjectRequest);

        // 关闭OSSClient。
        ossClient.shutdown();

        return aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath;
    }

    @Override
    public String uploadImgByInputStream(MultipartFile file, String fullPath) throws IOException {
//        base64Img = base64Img.split(",")[1];
//        //将生成的base64码反编译回二进制字节码
//        BASE64Decoder decoder = new BASE64Decoder();
//        byte[] file = decoder.decodeBuffer(base64Img);
//
        ObjectMetadata objectMeta = new ObjectMetadata();
        objectMeta.setContentLength(file.getBytes().length);

        //将字节码转换成流
        InputStream input = new ByteArrayInputStream(file.getBytes());

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
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fullPath, input, objectMeta);

        // 上传字符串。
        ossClient.putObject(putObjectRequest);

        // 关闭OSSClient。
        ossClient.shutdown();

        return aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath;
    }

    @Override
    public Mono<String> uploadImgByInputStreamNoBlock(FilePart file, String fullPath) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    InputStream input = new ByteArrayInputStream(bytes);
                    String endpoint = aliyunOssConfig.getOssBucketEndpoint();
                    // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
                    String accessKeyId = aliyunOssConfig.getAccessKeyId();
                    String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
                    String bucket = aliyunOssConfig.getOssBucket();
                    // 创建OSSClient实例。
                    OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

                    // 创建PutObjectRequest对象。
                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fullPath, input);

                    // 上传字符串。
                    ossClient.putObject(putObjectRequest);

                    // 关闭OSSClient。
                    ossClient.shutdown();

                    return Mono.just(aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath);
                })
                .subscribeOn(Schedulers.boundedElastic()); // 使用Schedulers.boundedElastic()来在另一个线程上执行阻塞操作
    }

    @Override
    public String uploadFavicon(InputStream stream, String fullPath) {
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 创建PutObjectRequest对象。
        // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fullPath, stream);

        // 上传字符串。
        ossClient.putObject(putObjectRequest);

        // 关闭OSSClient。
        ossClient.shutdown();

        return aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath;
    }

    @Override
    public String uploadFile(InputStream stream, String fullPath) throws IOException {
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 创建PutObjectRequest对象。
        // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fullPath, stream);

        // 上传字符串。
        ossClient.putObject(putObjectRequest);

        // 关闭OSSClient。
        ossClient.shutdown();

        return aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath;
    }

    @Override
    public String uploadFile(MultipartFile file, String fullPath) throws IOException {
        ObjectMetadata objectMeta = new ObjectMetadata();
        objectMeta.setContentLength(file.getBytes().length);

        //将字节码转换成流
        InputStream input = new ByteArrayInputStream(file.getBytes());

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
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fullPath, input, objectMeta);

        // 上传字符串。
        ossClient.putObject(putObjectRequest);

        // 关闭OSSClient。
        ossClient.shutdown();

        return aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath;
    }

    /**
     * @param oldFullPath
     * @param newFullPath
     * @return
     * @throws IOException
     */
    @Override
    public boolean rename(String oldFullPath, String newFullPath) throws IOException {
        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        // 将examplebucket下的srcobject.txt拷贝至同一Bucket下的destobject.txt。
        ossClient.copyObject(bucket, oldFullPath, bucket, newFullPath);

        // 删除srcobject.txt。
        ossClient.deleteObject(bucket, oldFullPath);
        return true;
    }

    /**
     * 复制文件，如果有同名则加个_副本 后缀
     * @param oldFullPath
     * @param newFullPath
     * @return
     * @throws IOException
     */
    @Override
    public boolean copy(String oldFullPath, String newFullPath) throws IOException {
        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        // 将examplebucket下的srcobject.txt拷贝至同一Bucket下的destobject.txt。
        ossClient.copyObject(bucket, oldFullPath, bucket, newFullPath);
        return true;
    }

    /**
     * 判断是否已存在相同的文件
     * @param filePath
     * @return
     * @throws IOException
     */
    @Override
    public boolean isExist(String filePath) throws IOException {
        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        // 判断文件是否存在。如果返回值为true，则文件存在，否则存储空间或者文件不存在。
        // 设置是否进行重定向或者镜像回源。默认值为true，表示忽略302重定向和镜像回源；如果设置isINoss为false，则进行302重定向或者镜像回源。
        //boolean isINoss = true;
        return ossClient.doesObjectExist(bucket, filePath);
    }

    /**
     * 分片上传大文件
     *
     * @param file
     * @param fullPath
     * @return
     * @throws IOException
     */
    @Override

    public String uploadLargeFile(MultipartFile file, String fullPath) throws IOException {
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            // 创建InitiateMultipartUploadRequest对象。
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucket, fullPath);
            // 初始化分片。
            InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(request);
            // 返回uploadId，它是分片上传事件的唯一标识。您可以根据该uploadId发起相关的操作，例如取消分片上传、查询分片上传等。
            String uploadId = upresult.getUploadId();
            // partETags是PartETag的集合。PartETag由分片的ETag和分片号组成。
            List<PartETag> partETags = new ArrayList<>();
            // 每个分片的大小，用于计算文件有多少个分片。单位为字节。
            final long partSize = 5 * 1024 * 1024L;   //5 MB。

            // 填写本地文件的完整路径。如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件。
//            final File sampleFile = new File("D:\\localpath\\examplefile.txt");
            long fileLength = file.getSize();
            int partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
            }
            // 遍历分片上传。
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                InputStream instream = new ByteArrayInputStream(file.getBytes());
                // 跳过已经上传的分片。
                instream.skip(startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucket);
                uploadPartRequest.setKey(fullPath);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                // 设置分片大小。除了最后一个分片没有大小限制，其他的分片最小为100 KB。
                uploadPartRequest.setPartSize(curPartSize);
                // 设置分片号。每一个上传的分片都有一个分片号，取值范围是1~10000，如果超出此范围，OSS将返回InvalidArgument错误码。
                uploadPartRequest.setPartNumber(i + 1);
                // 每个分片不需要按顺序上传，甚至可以在不同客户端上传，OSS会按照分片号排序组成完整的文件。
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                // 每次上传分片之后，OSS的返回结果包含PartETag。PartETag将被保存在partETags中。
                partETags.add(uploadPartResult.getPartETag());
            }


            // 创建CompleteMultipartUploadRequest对象。
            // 在执行完成分片上传操作时，需要提供所有有效的partETags。OSS收到提交的partETags后，会逐一验证每个分片的有效性。当所有的数据分片验证通过后，OSS将把这些分片组合成一个完整的文件。
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucket, fullPath, uploadId, partETags);

            // 如果需要在完成分片上传的同时设置文件访问权限，请参考以下示例代码。
            // completeMultipartUploadRequest.setObjectACL(CannedAccessControlList.Private);
            // 指定是否列举当前UploadId已上传的所有Part。如果通过服务端List分片数据来合并完整文件时，以上CompleteMultipartUploadRequest中的partETags可为null。
            // Map<String, String> headers = new HashMap<String, String>();
            // 如果指定了x-oss-complete-all:yes，则OSS会列举当前UploadId已上传的所有Part，然后按照PartNumber的序号排序并执行CompleteMultipartUpload操作。
            // 如果指定了x-oss-complete-all:yes，则不允许继续指定body，否则报错。
            // headers.put("x-oss-complete-all","yes");
            // completeMultipartUploadRequest.setHeaders(headers);

            // 完成分片上传。
            CompleteMultipartUploadResult completeMultipartUploadResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            System.out.println(completeMultipartUploadResult.getETag());
            return aliyunOssConfig.getOssBucketCdnHost() + "/" + fullPath;
        } catch (OSSException oe) {
            System.out.println("出错：Caught an OSSException, which means your request made it to OSS, " + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
            return "";
        } catch (ClientException ce) {
            System.out.println("出错：Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
            return "";
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }


    public boolean downloadLargeFile(InputStream inputStream, String fullPath) throws IOException {
        return false;
    }

    @Override
    public boolean deleteFile(String filePath) throws IOException {
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucket, filePath);
            return true;
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public InputStream downloadFile(String filePath) throws IOException {
        String endpoint = aliyunOssConfig.getOssBucketEndpoint();
        String accessKeyId = aliyunOssConfig.getAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getAccessKeySecret();
        String bucket = aliyunOssConfig.getOssBucket();
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            OSSObject ossObject = ossClient.getObject(bucket, filePath);
            return ossObject.getObjectContent();
        } finally {
            ossClient.shutdown();
        }
    }

    @Override
    public AssumeRoleResponse getAcsForUploadFile(String userName) {
        // STS接入地址，例如sts.cn-hangzhou.aliyuncs.com。
        String endpoint = aliyunOssConfig.getRamStsEndpoint();
        // 填写步骤1生成的访问密钥AccessKey ID和AccessKey Secret。
        String AccessKeyId = aliyunOssConfig.getRamAccessKeyId();
        String accessKeySecret = aliyunOssConfig.getRamAccessKeySecret();
        // 填写步骤3获取的角色ARN。
        String roleArn = aliyunOssConfig.getRamRoleArn();
        // 自定义角色会话名称，用来区分不同的令牌，例如可填写为SessionTest。
        String roleSessionName = userName;
        // 以下Policy用于限制仅允许使用临时访问凭证向目标存储空间examplebucket上传文件。
        // 临时访问凭证最后获得的权限是步骤4设置的角色权限和该Policy设置权限的交集，即仅允许将文件上传至目标存储空间examplebucket下的exampledir目录。
//        String policy = "{" +
//                "    \"Version\": \"1\", " +
//                "    \"Statement\": [" +
//                "        {\n" +
//                "            \"Action\": [" +
//                "                \"oss:PutObject\"" +
//                "            ]," +
//                "            \"Resource\": [" +
//                "                \"acs:oss:*:*:com-seaurl-cdn/space\"," +
//                "                \"acs:oss:*:*:com-seaurl-cdn/space/*\"," +
//                "            ]," +
//                "            \"Effect\": \"Allow\"" +
//                "        }" +
//                "    ]" +
//                "}";
        try {
            // regionId表示RAM的地域ID。以华东1（杭州）地域为例，regionID填写为cn-hangzhou。也可以保留默认值，默认值为空字符串（""）。
            String regionId = aliyunOssConfig.getRegion();
            // 添加endpoint。适用于Java SDK 3.12.0及以上版本。
            DefaultProfile.addEndpoint(regionId, "Sts", endpoint);
            // 添加endpoint。适用于Java SDK 3.12.0以下版本。
            // DefaultProfile.addEndpoint("",regionId, "Sts", endpoint);
            // 构造default profile。
            IClientProfile profile = DefaultProfile.getProfile(regionId, AccessKeyId, accessKeySecret);
            // 构造client。
            DefaultAcsClient client = new DefaultAcsClient(profile);
            final AssumeRoleRequest request = new AssumeRoleRequest();
            // 适用于Java SDK 3.12.0及以上版本。
            request.setSysMethod(MethodType.POST);
            // 适用于Java SDK 3.12.0以下版本。
            //request.setMethod(MethodType.POST);
            request.setRoleArn(roleArn);
            request.setRoleSessionName(roleSessionName);
            request.setPolicy(null); // 如果policy为空，则用户将获得该角色下所有权限。
            request.setDurationSeconds(3600L); // 设置临时访问凭证的有效时间为3600秒。
            final AssumeRoleResponse response = client.getAcsResponse(request);
            System.out.println("Expiration: " + response.getCredentials().getExpiration());
            System.out.println("Access Key Id: " + response.getCredentials().getAccessKeyId());
            System.out.println("Access Key Secret: " + response.getCredentials().getAccessKeySecret());
            System.out.println("Security Token: " + response.getCredentials().getSecurityToken());
            System.out.println("RequestId: " + response.getRequestId());
            return response;
        } catch (com.aliyuncs.exceptions.ClientException e) {
            log.error("getAcsForUploadFile failed: errCode={}, errMsg={}, requestId={}", e.getErrCode(), e.getErrMsg(), e.getRequestId());
            return null;
        }
    }
}
