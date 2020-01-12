package org.memegregator.entity.content;

import org.memegregator.storage.ContentStorage;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public interface ExternalMemeContent extends MemeContent {

    Mono<InternalMemeContent> convertToInternal(Function<String, Mono<ClientResponse>> fileStreamFunction, ContentStorage contentStorage);

}
