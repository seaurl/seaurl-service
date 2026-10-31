package com.seaurl.platform.users.utils;

import com.seaurl.platform.common.config.ServiceConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenUtil {
private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ServiceConfig serviceConfig;

    public String generateToken(Map<String, Object> claims){
        try{
            byte[] keyBytes = serviceConfig.getJwtSigningKey().getBytes(StandardCharsets.UTF_8);
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .signWith(Keys.hmacShaKeyFor(keyBytes))
                    .compact();
        }catch (Exception ex){
            logger.error("JWT generateToken error: {}",ex.getMessage());
            return  null;
        }
    }
}