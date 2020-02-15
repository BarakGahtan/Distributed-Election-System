package rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import state.FrontendWrapper;

@Configuration
public class Config {

    @Bean
    public FrontendWrapper frontendWrapper() {
        return new FrontendWrapper();
    }

}