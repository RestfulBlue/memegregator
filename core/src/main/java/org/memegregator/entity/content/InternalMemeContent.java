package org.memegregator.entity.content;

import org.memegregator.storage.ContentStorage;
import reactor.core.publisher.Mono;

public interface InternalMemeContent extends MemeContent {

  Mono<Void> dropFromStorage(ContentStorage contentStorage);

}
