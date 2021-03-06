package org.memegregator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.mongodb.reactivestreams.client.Success;
import org.bson.Document;
import org.memegregator.configuration.MongoConfiguration;
import org.memegregator.entity.info.MemeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Import(MongoConfiguration.class)
public class MemeService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final String memeCollection;
  private final MongoDatabase database;

  @Autowired
  public MemeService(
      MongoDatabase mongoDatabase,
      @Value("${mongo.memeCollection:memes}") String memeCollection) {
    this.database = mongoDatabase;
    this.memeCollection = memeCollection;
  }

  public Mono<Success> saveMeme(MemeInfo memeInfo) {
    try {
      Document document = Document.parse(objectMapper.writeValueAsString(memeInfo));
      return Mono.from(database
          .getCollection(memeCollection)
          .insertOne(document)
      );
    } catch (JsonProcessingException e) {
      // probably we'll never come here
      throw new IllegalStateException(e);
    }
  }
}
