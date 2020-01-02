package org.memegregator.coordinator.service;

import org.memegregator.coordinator.entity.Offset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class OffsetService {

    private final ReactiveMongoTemplate mongoTemplate;

    @Autowired
    public OffsetService(ReactiveMongoTemplate mongoTemplate){
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Void> saveOffset(Offset offset){
        return mongoTemplate.save(offset).then();
    }

    public Flux<Offset> findOffset(String service){
        return mongoTemplate.findAll(Offset.class);
    }


}
