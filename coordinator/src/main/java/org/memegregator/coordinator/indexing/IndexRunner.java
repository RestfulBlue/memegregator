package org.memegregator.coordinator.indexing;

import org.memegregator.coordinator.service.OffsetService;
import org.springframework.stereotype.Component;

@Component
public class IndexRunner implements Runnable {

    private final OffsetService offsetService;

    public IndexRunner(OffsetService offsetService){
        this.offsetService = offsetService;
        new Thread(this).start();
    }

    @Override
    public void run() {
        offsetService.saveOffset("debasto", 0).block();
        offsetService.findOffset("debasto").log().block();
    }
}
