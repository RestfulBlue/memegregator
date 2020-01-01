package org.memegregator.core;

import org.memegregator.push.file.S3ContentPusher;
import org.memegregator.push.meta.KinesisMetaPusher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication
@EnableWebFlux
@Import({S3ContentPusher.class, KinesisMetaPusher.class})
public class CoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }

}
