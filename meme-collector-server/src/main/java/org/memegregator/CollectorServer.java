package org.memegregator;

import org.memegregator.push.file.S3ContentPusher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@ComponentScan
@EnableWebFlux
@Import({S3ContentPusher.class})
public class CollectorServer {

    public static void main(String[] args) {
        SpringApplication.run(CollectorServer.class, args);
    }
}
