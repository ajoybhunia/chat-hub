package com.chathub;

import org.springframework.boot.SpringApplication;

public class TestChatHubApplication {

	public static void main(String[] args) {
		SpringApplication.from(ChatHubApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
