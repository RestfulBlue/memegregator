package org.memegregator.api;

import java.util.List;
import org.memegregator.entity.content.InternalMemeContent;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.entity.info.ApiMemeInfo;
import org.memegregator.entity.info.MemeInfo;
import org.memegregator.service.MemeService;
import org.memegregator.storage.ContentStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class FeedController {

  private static final long DEFAULT_CHUNK_SIZE = 262144;

  private final ContentStorage contentStorage;
  private final MemeService memeService;
  private final int maxLimit;

  public FeedController(ContentStorage contentStorage,
      MemeService memeService,
      @Value("${query.feed.maxLimit:100}") int maxLimit) {
    this.memeService = memeService;
    this.maxLimit = maxLimit;
    this.contentStorage = contentStorage;
  }


  @GetMapping("/feed")
  public Flux<MemeInfo> listMemes(
      @RequestParam(value = "startId", required = false) String startId,
      @RequestParam(value = "endId", required = false) String endId,
      @RequestParam(value = "limit", defaultValue = "100") int limit) {

    if (startId != null && endId != null) {
      return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "startId and endId can't be used at the same time"));
    }

    int actualLimit = Math.min(limit, maxLimit);

    Flux<ApiMemeInfo> memeInfoFlux = null;

    if (startId != null) {
      memeInfoFlux = memeService.listMemesOlderThanId(startId, actualLimit);
    } else if (endId != null) {
      memeInfoFlux = memeService.listMemesNewerThanId(endId, actualLimit);
    } else {
      memeInfoFlux = memeService.listLatestMemes(actualLimit);
    }

    return memeInfoFlux
        .flatMap(memeInfo -> {
          MemeContent content = memeInfo.getContent();
          if (!(content instanceof InternalMemeContent)) {
            return Mono.empty();
          }

          InternalMemeContent internalContent = (InternalMemeContent) content;

          return internalContent
              .convertToApiContent()
              .map(apiMemeContent -> {
                return new ApiMemeInfo(memeInfo.getId(), memeInfo.getTitle(), apiMemeContent,
                    memeInfo.getRating());
              });
        });
  }

  @GetMapping("/image/{id}")
  public Mono<ResponseEntity<Flux<byte[]>>> getImage(ServerWebExchange exchange,
      @PathVariable("id") String key) {
    return streamContent(exchange, key);
  }

  @GetMapping("/video/{id}")
  public Mono<ResponseEntity<Flux<byte[]>>> getVideo(ServerWebExchange exchange,
      @PathVariable("id") String key) {
    return streamContent(exchange, key);
  }

  public Mono<ResponseEntity<Flux<byte[]>>> streamContent(ServerWebExchange exchange, String key) {
    List<String> rangeHeaders = exchange.getRequest().getHeaders().get("Range");
    if (rangeHeaders != null && !rangeHeaders.isEmpty()) {
      String rangeHeader = rangeHeaders.get(0);
      return contentStorage.pullData(key, rangeHeader)
          .map(streamWithLength -> {
            return ResponseEntity
                .status(206)
                .contentType(MediaType.valueOf("video/mp4"))
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", streamWithLength.getRange())
                .contentLength(streamWithLength.getLength())
                .body(streamWithLength.getStream());
          });
    }

    return contentStorage.pullData(key)
        .map(streamWithLength -> {
          return ResponseEntity
              .status(200)
              .contentType(MediaType.valueOf("video/mp4"))
              .header("Accept-Ranges", "bytes")
              .contentLength(streamWithLength.getLength())
              .body(streamWithLength.getStream());
        });
  }

}
