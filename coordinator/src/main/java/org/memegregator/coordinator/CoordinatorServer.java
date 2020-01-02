package org.memegregator.coordinator;

import org.memegregator.coordinator.configuration.MongoConfiguration;
import org.memegregator.push.file.S3ContentPusher;
import org.memegregator.push.meta.KinesisMetaPusher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan
@EnableWebFlux
@Import({S3ContentPusher.class, KinesisMetaPusher.class, MongoConfiguration.class})
public class CoordinatorServer {

    public static void main(String[] args) {
        SpringApplication.run(CoordinatorServer.class, args);
    }
}
