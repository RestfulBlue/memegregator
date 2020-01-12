package org.memegregator.publishers;

import org.memegregator.entity.info.MemeInfo;
import reactor.core.publisher.Flux;

public interface Publisher {

  void publishMemes(Flux<MemeInfo> memeStream);

}
