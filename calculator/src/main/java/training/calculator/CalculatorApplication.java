package training.calculator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Function;

@SpringBootApplication
public class CalculatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalculatorApplication.class, args);
    }

    @Bean
    public Function<CalculationRequest, CalculationResponse> calculate() {
        return request -> new CalculationResponse(request.getA() + request.getB());
    }

    @Bean
    public Function<CalculationResponse, RoundResponse> round() {
        return CalculationResponse::round;
    }

}
