package training.calculator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

@SpringBootApplication
@Slf4j
public class CalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);
    }

    @Bean
    public Function<CalculationRequest, CalculationResponse> calculate() {
        return request -> {
            log.info("Request: {}", request);
            return new CalculationResponse(request.getA() + request.getB());
        };
    }

    @Bean
    public Function<CalculationResponse, RoundResponse> round() {
        return CalculationResponse::round;
    }

}
