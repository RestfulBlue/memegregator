package org.memegregator.service;

import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.operation.OrderBy;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.memegregator.configuration.MongoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Import(MongoConfiguration.class)
public class MemeService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final String memeCollection;
  private final MongoDatabase database;
  private final MongoClient mongoClient;

  @Autowired
  public MemeService(
      MongoClient mongoClient,
      MongoDatabase mongoDatabase,
      @Value("${mongo.memeCollection:memes}") String memeCollection) {
    this.database = mongoDatabase;
    this.mongoClient = mongoClient;
    this.memeCollection = memeCollection;
  }

  public Flux<String> listMemes(int limit) {
    return listMemes(new Document(), OrderBy.DESC, limit);
  }

  public Flux<String> listMemes(String startId, int limit) {
    return listMemes(lt("_id",  new ObjectId(startId)), OrderBy.DESC, limit);
  }

  public Flux<String> listMemes(String startId, OrderBy orderBy, int limit) {
    return listMemes(lt("_id", new ObjectId(startId)), orderBy, limit);
  }

  private Flux<String> listMemes(Bson query, OrderBy orderBy, int limit) {
    return Flux.from(database.getCollection(memeCollection)
        .find(query)
        .sort(new BasicDBObject("_id", orderBy.getIntRepresentation()))
        .limit(limit))
        .map(Document::toJson);
  }
}
