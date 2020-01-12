package org.memegregator.service;

import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.lt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.operation.OrderBy;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.memegregator.configuration.MongoConfiguration;
import org.memegregator.entity.content.MemeContent;
import org.memegregator.entity.info.ApiMemeInfo;
import org.memegregator.entity.info.MemeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@Import(MongoConfiguration.class)
public class MemeService {

  private final String memeCollection;
  private final MongoDatabase database;
  private final ObjectMapper mapper = new ObjectMapper();

  @Autowired
  public MemeService(
      MongoClient mongoClient,
      MongoDatabase mongoDatabase,
      @Value("${mongo.memeCollection:memes}") String memeCollection) {
    this.database = mongoDatabase;
    this.memeCollection = memeCollection;
  }

  public Flux<ApiMemeInfo> listMemesOlderThanId(String id, int limit) {
    return fetchMemes(lt("_id", new ObjectId(id)), OrderBy.DESC, limit);
  }

  public Flux<ApiMemeInfo> listMemesNewerThanId(String id, int limit) {
    return fetchMemes(gt("_id", new ObjectId(id)), OrderBy.ASC, limit);
  }

  public Flux<ApiMemeInfo> listLatestMemes(int limit) {
    return fetchMemes(new Document(), OrderBy.DESC, limit);
  }

  public Flux<ApiMemeInfo> fetchMemes(Bson query, OrderBy orderBy, int limit) {
    return Flux.from(database.getCollection(memeCollection)
        .find(query)
        .sort(new BasicDBObject("_id", orderBy.getIntRepresentation()))
        .limit(limit))
        .map(this::documentToInfo);
  }

  private ApiMemeInfo documentToInfo(Document document) {
    try {
      return new ApiMemeInfo(
          document.getObjectId("_id").toString(),
          document.getString("title"),
          mapper.readValue(document.get("content", Document.class).toJson(), MemeContent.class),
          document.getInteger("rating")
      );
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
