package org.memegregator.configuration;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class MongoConfiguration {

  @Bean
  public MongoClient mongoClient(@Value("${mongo.connectionString}") String connectionString) {
    return MongoClients.create(connectionString);
  }

  @Bean
  public MongoDatabase database(MongoClient client, @Value("${mongo.databaseName}") String databaseName) {
    return client.getDatabase(databaseName);
  }

}