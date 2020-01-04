package org.memegregator;

import org.memegregator.storage.S3ContentStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan
@EnableWebFlux
@Import({S3ContentStorage.class})
public class CollectorServer {

    public static void main(String[] args) {
        SpringApplication.run(CollectorServer.class, args);
    }
}
