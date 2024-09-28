package com.casino.blackjack.config.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class Mapper {

    @Bean
    ObjectMapper getObjectMapper(){

        return new ObjectMapper();
    }

}
