package com.chandana.stockoutagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class App {
    @Bean
    CommandLineRunner testChat(ChatClient.Builder builder) {
        return args -> {
            var chatClient = builder.build();
            String response = chatClient.prompt()
                    .user("Say hello in one sentence.")
                    .call()
                    .content();
            System.out.println(response);
        };
    }
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
