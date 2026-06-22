package com.tripify.backend;

import com.tripify.backend.service.WeatherService;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {"eureka.client.enabled=false"})
public class CucumberSpringConfiguration {

    @MockBean
    private WeatherService weatherService;
}
