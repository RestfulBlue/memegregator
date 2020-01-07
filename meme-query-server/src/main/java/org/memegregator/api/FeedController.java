package org.memegregator.api;

import org.memegregator.service.MemeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class FeedController {

  private final MemeService memeService;

  public FeedController(MemeService memeService) {
    this.memeService = memeService;
  }


  @GetMapping("/feed")
  public Flux<String> listMemes(
      @RequestParam(value = "startOffset", required = false) String startId,
      @RequestParam(value = "limit", defaultValue = "100") int limit) {

    if (startId == null) {
      return memeService.listMemes(limit);
    }
    return memeService.listMemes(startId, limit);
  }
}
