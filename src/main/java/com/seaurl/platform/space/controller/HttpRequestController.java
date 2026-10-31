package com.seaurl.platform.space.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.space.model.HttpRequest;
import com.seaurl.platform.space.service.HttpRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.request.HttpRequestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.ByteBuffer;


import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;


@RestController
@RequestMapping("/space")

public class HttpRequestController {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private HttpRequestService httpRequestService;


    // 添加新的 HTTP 请求记录
    @PostMapping("/crud/httpRequest/add")
    public ResponseEntity<?> add(@RequestBody HttpRequestRequest httpRequestRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            com.seaurl.platform.space.model.HttpRequest httpRequest = new com.seaurl.platform.space.model.HttpRequest();
            Date date = new Date();

            // 普通变量
            httpRequest.setUrl(httpRequestRequest.getUrl());
            httpRequest.setHeaders(httpRequestRequest.getHeaders());
            httpRequest.setBody(httpRequestRequest.getBody());
            httpRequest.setMethod(httpRequestRequest.getMethod());
            httpRequest.setContentType(httpRequestRequest.getContentType());

            httpRequest.setUid(loginUser.getUid());
            httpRequest.setCreatedDt(date);
            httpRequest.setUpdatedDt(date);


            com.seaurl.platform.space.model.HttpRequest created = httpRequestService.add(httpRequest);
            return ResponseEntity.ok(new ResultSuccess<>(created));
        } catch (Exception ex) {
            log.error("Add HttpRequest exception: {}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    // 更新 HTTP 请求记录
    @PostMapping("/crud/httpRequest/update")
    public ResponseEntity<?> update(@RequestBody HttpRequest httpRequest) {
        try {
            // 1. 获取当前登录用户
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            // 2. 验证 ID 是否有效
            if (httpRequest.getId() == null || httpRequest.getId().isEmpty()) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            // 3. 验证数据是否存在且属于当前用户
            HttpRequest existing = httpRequestService.queryById(httpRequest.getId());
            if (existing == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.QUERY_EMPTY));
            }

            // 4. 设置用户ID确保数据归属不变
            httpRequest.setUid(loginUser.getUid());

            // 5. 执行更新
            Boolean updated = httpRequestService.update(httpRequest);

            // 6. 返回更新后的数据
            return ResponseEntity.ok(new ResultSuccess<>(updated));
        } catch (Exception ex) {
            log.error("Update HttpRequest exception: {}", ex.getMessage(), ex);
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    // 查询用户所有 HTTP 请求记录
    @PostMapping("/crud/httpRequest/queryAll")
    public ResponseEntity<?> queryAll() {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            List<com.seaurl.platform.space.model.HttpRequest> requests = httpRequestService.queryAll(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(requests));
        } catch (Exception ex) {
            log.error("Query all HttpRequests exception: {}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    // 根据 ID 删除 HTTP 请求记录（带用户验证）
    @PostMapping("/crud/httpRequest/deleteById")
    public ResponseEntity<?> deleteById(@RequestBody HttpRequestRequest deleteRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            if (deleteRequest.getId() == null || deleteRequest.getId().isEmpty()) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            boolean deleted = httpRequestService.deleteById(loginUser.getUid(), deleteRequest.getId());
            return ResponseEntity.ok(new ResultSuccess<>(deleted));
        } catch (Exception ex) {
            log.error("Delete HttpRequest exception: {}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    // 根据 ID 查询单个 HTTP 请求记录
    @PostMapping("/crud/httpRequest/queryById")
    public ResponseEntity<?> queryById(@RequestBody HttpRequestRequest queryRequest) {
        try {
            LoginUser loginUser = UserContextHolder.getContext().getUser();
            if (loginUser == null) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
            }

            if (queryRequest.getId() == null || queryRequest.getId().isEmpty()) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
            }

            HttpRequest result = httpRequestService.queryById(queryRequest.getId());

            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("Query HttpRequest by ID exception: {}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }


    /**
     * 构造文件的表单数据
     *
     * @param dataBuffer
     * @param key
     * @param filename
     * @param boundary
     * @param contentType
     * @return
     */
    private DataBuffer createFilePartDataBuffer(DataBuffer dataBuffer, String key, String filename, String boundary, String contentType) {
        String header = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + key + "\"; filename=\"" + filename + "\"\r\nContent-Type: " + contentType + "\r\n\r\n";
        DataBuffer headerBuffer = new DefaultDataBufferFactory().wrap(header.getBytes(StandardCharsets.UTF_8));
        DataBuffer footerBuffer = new DefaultDataBufferFactory().wrap("\r\n".getBytes(StandardCharsets.UTF_8));
        return new DefaultDataBufferFactory().join(List.of(headerBuffer, dataBuffer, footerBuffer));
    }


    private Flux<DataBuffer> endBoundaryFlux(String boundary) {
        String endBoundary = "--" + boundary + "--\r\n";
        DataBuffer endBoundaryBuffer = new DefaultDataBufferFactory().wrap(endBoundary.getBytes(StandardCharsets.UTF_8));
        return Flux.just(endBoundaryBuffer);
    }

    /**
     * @param files         所有的文件都在files中，注意：即使名称相同，文件也可以是多个，而不是覆盖
     * @param fileParams    文件对应的files的key值参数，后面需要解析并对应起来
     * @param params        这个是除了文件之外的key-value参数
     * @param requestParams 这个是包含请求的必须参数，如：url method contentType headers
     * @return
     */
    @PostMapping("/crud/httpRequest/proxy/formData")
    public Mono<ResponseEntity<?>> proxyRequest2(@RequestPart("files") Flux<FilePart> files,
                                                 @RequestPart("fileParams") String fileParams,
                                                 @RequestPart("params") String params,
                                                 @RequestPart("requestParams") String requestParams) {
        log.info("files={}", files);
        log.info("fileParams={}", fileParams);
        log.info("params={}", params);
        log.info("requestParams={}", requestParams);
        String boundary = "Boundary-" + System.currentTimeMillis();


        try {
            if (!StringUtils.hasLength(requestParams)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }

            // 解析requestParams
            JSONObject requestParamsMap = JSONObject.parseObject(requestParams);

            String url = (String) requestParamsMap.get("url");
            String method = (String) requestParamsMap.get("method");
            String contentType = (String) requestParamsMap.get("contentType");
            String headers = (String) requestParamsMap.get("headers");

            if (!StringUtils.hasLength(method) || !StringUtils.hasLength(url)) {
                return Mono.just(ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY)));
            }
            if (!method.equals("put") && !method.equals("post")) {
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Method不正确！提示：Form-Data请求Method只能是POST或者PUT"));
            }

            // 获取文件个数
            Mono<Long> fileCountMono = files.count();
            // 判断文件数量是否大于0
            return fileCountMono.flatMap(fileCount -> {

                Flux<DataBuffer> paramsPublisherFlux = getParamsContentFlux(params, boundary);

                // 有文件的情况
                if (fileCount > 0 && StringUtils.hasLength(fileParams)) {
                    List<String> fileParamsList = JSON.parseObject(fileParams, new TypeReference<>() {
                    });

                    if (fileParamsList.size() != fileCount) {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件数量和文件Key数量不一致！"));
                    }

                    Flux<DataBuffer> fileBodyPublisherFlux = files.index()
                            .flatMap(indexedFilePart -> {
                                long index = indexedFilePart.getT1();
                                FilePart filePart = indexedFilePart.getT2();
                                return filePart.content()
                                        .map(dataBuffer -> {
                                            String fileKey = fileParamsList.get((int) index);
                                            return createFilePartDataBuffer(dataBuffer, fileKey, filePart.filename(), boundary, filePart.headers().getContentType().toString());
                                        });
                            });


                    Flux<DataBuffer> combinedFlux;
                    if (paramsPublisherFlux != null) {
                        combinedFlux = Flux.concat(fileBodyPublisherFlux, paramsPublisherFlux, endBoundaryFlux(boundary));
                    } else {
                        combinedFlux = Flux.concat(fileBodyPublisherFlux, endBoundaryFlux(boundary));
                    }

                    return startRequest(boundary, url, method, headers, combinedFlux);
                }
                // 没有文件的情况
                else {
                    if (paramsPublisherFlux != null) {
                        Flux<DataBuffer> combinedFlux = Flux.concat(paramsPublisherFlux, endBoundaryFlux(boundary));
                        return startRequest(boundary, url, method, headers, combinedFlux);
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("请求参数和文件参数不能同时为空！"));
                    }
                }
            });

        } catch (Exception ex) {
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("请求失败！错误信息: " + ex.getMessage()));
        }
    }

    /**
     * 解析参数后构造成form-data形式返回
     *
     * @param params
     * @param boundary
     * @return
     */
    private Flux<DataBuffer> getParamsContentFlux(String params, String boundary) {
        List<Map<String, String>> paramList;
        if (StringUtils.hasLength(params)) {
            paramList = JSON.parseObject(params, new TypeReference<>() {
            });
        } else {
            paramList = null;
        }

        if (paramList != null && !paramList.isEmpty()) {
            return Flux.fromIterable(paramList)
                    .map(param -> {
                        String partContent = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + param.get("key") + "\"\r\n\r\n" + param.get("value") + "\r\n";
                        return new DefaultDataBufferFactory().wrap(partContent.getBytes(StandardCharsets.UTF_8));
                    });
        } else {
            return Flux.empty();
        }
    }

    /**
     * 开始请求
     *
     * @param boundary
     * @param url
     * @param method
     * @param headers
     * @param bodyPublisher
     * @return
     */
    private Mono<ResponseEntity<?>> startRequest(String boundary, String url, String method, String headers, Flux<DataBuffer> bodyPublisher) {
        WebClient webClient = WebClient.create();

        WebClient.RequestBodySpec requestSpec = webClient.method(HttpMethod.valueOf(method.toUpperCase()))
                .uri(url)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);

        // 添加自定义头
        if (StringUtils.hasLength(headers)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, String> headersMap = objectMapper.readValue(headers, Map.class);
                if (headersMap != null && !headersMap.isEmpty()) {
                    log.info("headersMap={}", JSONArray.toJSONString(headersMap));
                    headersMap.forEach(requestSpec::header);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        requestSpec.body(BodyInserters.fromDataBuffers(bodyPublisher));
        return handleWebClientResponse(requestSpec);
    }

    public static Mono<ResponseEntity<?>> handleWebClientResponse(WebClient.RequestBodySpec requestBodySpec) {
        return requestBodySpec
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.createException().flatMap(Mono::error))
                .toEntityFlux(String.class)
                .flatMap(responseEntity -> {
                    HttpHeaders headers = responseEntity.getHeaders();
                    MediaType contentType = headers.getContentType();
                    Flux<String> bodyFlux = responseEntity.getBody();
                    long contentLength = headers.getContentLength();

                    System.out.println("Response Headers: " + headers);
                    System.out.println("Content-Type: " + contentType);
                    System.out.println("Content-Length: " + contentLength);

                    // 🚨 处理空响应：Content-Length: 0
                    if (contentLength == 0 || bodyFlux == null) {
                        System.out.println("🚨 No Content: Returning 204");
                        return Mono.just(ResponseEntity.status(204).headers(headers).body(""));
                    }

                    if (MediaType.TEXT_EVENT_STREAM.isCompatibleWith(contentType)) {
                        System.out.println("------------ 1: Handling SSE ------------");
                        return Mono.just(ResponseEntity.ok().headers(headers).contentType(MediaType.TEXT_EVENT_STREAM).body(bodyFlux));
                    } else if (MediaType.TEXT_HTML.isCompatibleWith(contentType)) {
                        System.out.println("------------ 2: Handling HTML ------------");
                        return bodyFlux.collectList()
                                .map(list -> String.join("", list))
                                .map(body -> ResponseEntity.ok().headers(headers).contentType(MediaType.TEXT_HTML).body(body));
                    } else {
                        System.out.println("------------ 3: Handling Other Types ------------");
                        return bodyFlux.collectList()
                                .map(list -> String.join("", list))
                                .map(body -> ResponseEntity.ok().headers(headers).body(body));
                    }
                })
                .onErrorResume(WebClientResponseException.class, e -> Mono.just(ResponseEntity.status(e.getStatusCode())
                        .headers(e.getHeaders())
                        .body(e.getResponseBodyAsString())))
                .onErrorResume(e -> {
                    HttpHeaders errorHeaders = new HttpHeaders();
                    errorHeaders.set("error-status", "500");
                    errorHeaders.set("error-message", e.getMessage());
                    return Mono.just(ResponseEntity.status(500)
                            .headers(errorHeaders)
                            .body(Map.of(
                                    "status", "500",
                                    "error", "Internal Server Error",
                                    "message", e.getMessage()
                            )));
                });
    }


    /**
     * @param httpRequestRequest
     * @return
     */
    @PostMapping("/crud/httpRequest/proxy")
    public Mono<ResponseEntity<?>> proxyRequest(@RequestBody HttpRequestRequest httpRequestRequest) {
        String url = httpRequestRequest.getUrl();
        String headers = httpRequestRequest.getHeaders();
        String body = httpRequestRequest.getBody();
        String method = httpRequestRequest.getMethod();
        String contentType = httpRequestRequest.getContentType();

        log.info("Received proxy request: url={}, method={}, headers={}, body={}, contentType={}",
                url, method, headers, body, contentType);

        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            log.warn("User not logged in");
            return Mono.just(ResponseEntity.ok(Flux.just(ResultStatus.DATA_EMPTY.getMessage())));
        }
        if (!StringUtils.hasLength(method) || !StringUtils.hasLength(url)) {
            log.warn("Method or URL is empty");
            return Mono.just(ResponseEntity.ok(Flux.just(ResultStatus.DATA_EMPTY.getMessage())));
        }

        WebClient webClient = WebClient.create();

        try {
            Map<String, String> headersMap = null;
            if (StringUtils.hasLength(headers)) {
                log.info("Parsing headers: {}", headers);
                ObjectMapper objectMapper = new ObjectMapper();
                headersMap = objectMapper.readValue(headers, Map.class);
            }

            WebClient.RequestBodySpec requestSpec = webClient.method(HttpMethod.valueOf(method.toUpperCase()))
                    .uri(URI.create(url));

            if (headersMap != null && !headersMap.isEmpty()) {
                log.info("Adding headers: {}", headersMap);
                headersMap.forEach(requestSpec::header);
            }

            if ("put".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method)) {
                if (StringUtils.hasLength(contentType)) {
                    log.info("Setting Content-Type: {}", contentType);
                    requestSpec.header("Content-Type", contentType);
                }
                if (StringUtils.hasLength(body)) {
                    log.info("Setting body: {}", body);
                    requestSpec.bodyValue(body);
                }
            }

            log.info("Sending request to: {}", url);
            return handleWebClientResponse(requestSpec);
        } catch (Exception ex) {
            log.error("Error occurred in proxyRequest: ", ex);
            return Mono.just(ResponseEntity.ok(Flux.just(ex.getMessage())));
        }
    }

}
