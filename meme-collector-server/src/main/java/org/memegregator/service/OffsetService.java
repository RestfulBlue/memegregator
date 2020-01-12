package org.memegregator.service;

import static com.mongodb.client.model.Filters.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.memegregator.configuration.MongoConfiguration;
import org.memegregator.entity.offsets.ScrappingOffset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Import(MongoConfiguration.class)
public class OffsetService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String offsetCollection;
  private final MongoDatabase database;

  @Autowired
  public OffsetService(
      MongoDatabase mongoDatabase,
      @Value("${mongo.offsetCollection:offsets}") String offsetCollection) {
    this.database = mongoDatabase;
    this.offsetCollection = offsetCollection;
  }

  public Mono<UpdateResult> saveOffset(String type, ScrappingOffset offset) {
    try {
      Document data = Document.parse(objectMapper.writeValueAsString(offset));
      return Mono.from(database
          .getCollection(offsetCollection)
          .replaceOne(eq("type", type), data, new ReplaceOptions().upsert(true))
      );
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }

  public Mono<ScrappingOffset> findOffset(String systemName) {
    return Mono
        .from(database
            .getCollection(offsetCollection)
            .find(eq("type", systemName))
            .first()
        )
        .map(document -> {
          try {
            return objectMapper.readValue(document.toJson(), ScrappingOffset.class);
          } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
          }
        });
  }


}
