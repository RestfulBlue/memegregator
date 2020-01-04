package org.memegregator.service;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.Success;
import org.bson.Document;
import org.memegregator.configuration.MongoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static com.mongodb.client.model.Filters.eq;

@Component
@Import(MongoConfiguration.class)
public class OffsetService {

    private final String offsetCollection;
    private final MongoDatabase database;
    private final MongoClient mongoClient;

    @Autowired
    public OffsetService(
            MongoClient mongoClient,
            MongoDatabase mongoDatabase,
            @Value("${mongo.offsetCollection:offsets}") String offsetCollection) {
        this.database = mongoDatabase;
        this.mongoClient = mongoClient;
        this.offsetCollection = offsetCollection;
    }

    public Mono<Success> saveOffset(String systemName, long offset) {
        return Mono.from(database
                .getCollection(offsetCollection)
                .insertOne(new Document("system", systemName).append("offset", offset))
        );
    }

    public Mono<Integer> findOffset(String systemName) {
        return Mono.from(database
                .getCollection(offsetCollection)
                .find(eq("system", systemName))
                .first()
        ).map(document -> document.getInteger("system"));
    }


}
