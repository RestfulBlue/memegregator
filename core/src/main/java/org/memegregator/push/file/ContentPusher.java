package org.memegregator.push.file;

import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ContentPusher {

    Mono<Void> pushData(String key, Flux<byte[]> stream);

}
