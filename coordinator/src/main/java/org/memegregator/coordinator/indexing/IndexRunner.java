package org.memegregator.coordinator.indexing;

import org.memegregator.coordinator.entity.Offset;
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
        offsetService.saveOffset(new Offset("debasto", 0)).block();
        offsetService.findOffset("qwe123").log().subscribe();
    }
}
