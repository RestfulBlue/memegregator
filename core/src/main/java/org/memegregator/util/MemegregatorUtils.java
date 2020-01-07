package org.memegregator.util;

import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class MemegregatorUtils {

    public static String getExtension(String url) {
        String[] parts = url.split("\\.");
        return parts[parts.length - 1];
    }

    public static String getUid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
