package com.seaurl.platform.space.controller.aiHub;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.config.ServiceConfig;
import com.seaurl.platform.common.context.UserContext;
import com.seaurl.platform.common.context.UserContextHolder;

import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.WeatherCity;
import com.seaurl.platform.space.request.WeatherCityRequest;
import com.seaurl.platform.space.service.WeatherCityService;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/space")

public class WeatherController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ServiceConfig serviceConfig;

    @Autowired
    WeatherCityService weatherCityService;

    /**
     * 查询当前用户所有的城市天气
     *
     * @return
     */
    @GetMapping("/crud/weather/city/query")
    public ResponseEntity<?> queryCity() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<WeatherCity> result = weatherCityService.queryAll(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("addCity error:{}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 置顶城市
     *
     * @param id
     * @return
     */
    @GetMapping("/crud/weather/city/top/{id}")
    public ResponseEntity<?> top(@PathVariable String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Boolean result = weatherCityService.top(id);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("addCity error:{}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 删除城市天气
     *
     * @param id
     * @return
     */
    @DeleteMapping("/crud/weather/city/{id}")
    public ResponseEntity<?> deleteCity(@PathVariable String id) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(id)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Boolean result = weatherCityService.delete(loginUser.getUid(), id);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("addCity error:{}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 添加城市
     *
     * @param weatherCityRequest
     * @return
     */
    @PostMapping("/crud/weather/city/add")
    public ResponseEntity<?> addCity(@RequestBody WeatherCityRequest weatherCityRequest) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(weatherCityRequest.getId())
                || !StringUtils.hasLength(weatherCityRequest.getLat())
                || !StringUtils.hasLength(weatherCityRequest.getLon())
                || !StringUtils.hasLength(weatherCityRequest.getName())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Boolean isExit = weatherCityService.isExit(loginUser.getUid(), weatherCityRequest.getId());
            if (isExit) {
                return ResponseEntity.ok(new ResultInfo<>(ResultStatus.VALUE_EXIST));
            }

            Date date = new Date();
            WeatherCity weatherCity = new WeatherCity();
            weatherCity.setWid(weatherCityRequest.getId());
            weatherCity.setUid(loginUser.getUid());
            weatherCity.setName(weatherCityRequest.getName());
            weatherCity.setLat(weatherCityRequest.getLat());
            weatherCity.setLon(weatherCityRequest.getLon());
            weatherCity.setTz(weatherCityRequest.getTz());
            weatherCity.setCountry(weatherCityRequest.getCountry());
            weatherCity.setAdm2(weatherCityRequest.getAdm2());
            weatherCity.setAdm1(weatherCityRequest.getAdm1());
            weatherCity.setFxLink(weatherCityRequest.getFxLink());
            weatherCity.setRank(weatherCityRequest.getRank());
            weatherCity.setIsDst(weatherCityRequest.getIsDst());
            weatherCity.setType(weatherCityRequest.getType());
            weatherCity.setUtcOffset(weatherCityRequest.getUtcOffset());
            weatherCity.setIsDelete(false);
            weatherCity.setCreatedDt(date);
            weatherCity.setUpdatedDt(date);

            Boolean result = weatherCityService.add(weatherCity);
            return ResponseEntity.ok(new ResultSuccess<>(result));

        } catch (Exception ex) {
            log.info("addCity error:{}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取免费和风天气jwt token
     */
    @GetMapping("/crud/weather/free/token")
    public ResponseEntity<?> getWeatherFreeToken() {
        try {

            String ssrPrivateKey = serviceConfig.getFreePrivateKey();
            String projectId = serviceConfig.getFreeProjectId();
            String credentId = serviceConfig.getFreeCredentId();
            log.info("-----privateKey-----{}", ssrPrivateKey);
            log.info("-----projectId-----{}", projectId);
            log.info("-----credentId-----{}", credentId);
            // Private key
            byte[] privateKeyBytes = Base64.getDecoder().decode(ssrPrivateKey);
            PKCS8EncodedKeySpec encoded = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = new EdDSAPrivateKey(encoded);

            // Header kid:凭据id
            String headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"" + credentId + "\"}";

            // Payload
            long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30;
            long exp = iat + 900;
//            sub:项目id
            String payloadJson = "{\"sub\": \"" + projectId + "\", \"iat\": " + iat + ", \"exp\": " + exp + "}";

            // Base64url header+payload
            String headerEncoded = Base64.getUrlEncoder().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String data = headerEncoded + "." + payloadEncoded;

            EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);

            // Sign
            final Signature s = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
            s.initSign(privateKey);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signature = s.sign();

            String signatureString = Base64.getUrlEncoder().encodeToString(signature);

            log.info("Signature: " + signatureString);

            // Print Token
            String jwt = data + "." + signatureString;
            log.info("JWT: " + jwt);
            return ResponseEntity.ok(new ResultSuccess<>(jwt));

        } catch (Exception exception) {
            log.info("getWeatherToken error:{}", exception.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    /**
     * 获取标准收费token
     *
     * @return
     */
    @GetMapping("/crud/weather/standard/token")
    public ResponseEntity<?> getWeatherStandardToken() {
        try {
            String ssrPrivateKey = serviceConfig.getStandardPrivateKey();
            String projectId = serviceConfig.getStandardProjectId();
            String credentId = serviceConfig.getStandardCredentId();
            log.info("-----privateKey-----{}", ssrPrivateKey);
            log.info("-----projectId-----{}", projectId);
            log.info("-----credentId-----{}", credentId);
            // Private key
            byte[] privateKeyBytes = Base64.getDecoder().decode(ssrPrivateKey);
            PKCS8EncodedKeySpec encoded = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = new EdDSAPrivateKey(encoded);

            // Header kid:凭据id
            String headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"" + credentId + "\"}";

            // Payload
            long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30;
            long exp = iat + 900;
//            sub:项目id
            String payloadJson = "{\"sub\": \"" + projectId + "\", \"iat\": " + iat + ", \"exp\": " + exp + "}";

            // Base64url header+payload
            String headerEncoded = Base64.getUrlEncoder().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = Base64.getUrlEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String data = headerEncoded + "." + payloadEncoded;

            EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519);

            // Sign
            final Signature s = new EdDSAEngine(MessageDigest.getInstance(spec.getHashAlgorithm()));
            s.initSign(privateKey);
            s.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signature = s.sign();

            String signatureString = Base64.getUrlEncoder().encodeToString(signature);

            log.info("Signature: " + signatureString);

            // Print Token
            String jwt = data + "." + signatureString;
            log.info("JWT: " + jwt);
            return ResponseEntity.ok(new ResultSuccess<>(jwt));

        } catch (Exception exception) {
            log.info("getWeatherToken error:{}", exception.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
