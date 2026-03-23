package com.bomin.aireviewreply;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiReviewReplyApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiReviewReplyApplication.class, args);
    }
}
