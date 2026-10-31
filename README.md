seaurl-platform

# 开发环境运行
我们在config/application-local.yml中设置了账号，所以直接运行即可
```angular2html
spring:
  config:
    import: optional:file:./config/application-local.yml
```

# 生产环境打包
## 注意：
1、不能把config/application-local.yml打包到target目录，否则泄露了账号
2、生产环境，只能在服务器上创建内容和application-local.yml内容一样的.env文件
3、然后在compose中配置
```
  seaurl-platform:
    #生产环境注入账号
    env_file:
      - .env
```
下面是打包的命令
```
mvn clean package -DskipTests

docker buildx build \
  --platform linux/amd64 \
  --build-arg JAR_FILE=target/seaurl-platform-1.0-SNAPSHOT.jar \
  -t registry.cn-hangzhou.aliyuncs.com/com-seaurl/seaurl-platform:26.07.28-1930 \
  -f src/main/docker/Dockerfile .
  
docker push registry.cn-hangzhou.aliyuncs.com/com-seaurl/seaurl-platform:26.07.28-1930
```
