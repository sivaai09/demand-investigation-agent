package dev.demandagent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class App {
    // Tool output is LLM prompt input, so cap how many days one call can return.
    static final int MAX_ROWS = 366;

    @Bean
    @Profile("chat-test")
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
