package com.example.demo;

import com.example.demo.service.MsgService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(DemoApplication.class, args);
        MsgService msgService = ctx.getBean(MsgService.class);
        msgService.pop();
        msgService.pull();
    }
}

