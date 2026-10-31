package com.seaurl.platform.space.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Kept as empty config placeholder. Static file serving is handled by
 * {@link LocalUploadsController} via @RestController (annotation-based),
 * which correctly honors spring.webflux.base-path.
 * <p>
 * RouterFunction-based approach was removed because RouterFunctions.resources()
 * does not respect spring.webflux.base-path, causing 404 for /api/uploads/**.
 */
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageWebConfig {
}
