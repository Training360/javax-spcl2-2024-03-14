package training.calculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.function.context.FunctionCatalog;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class CalculationTest {

    @Autowired
    FunctionCatalog functionCatalog;

    @Test
    void calculate() {
        var function = (Function<CalculationRequest, CalculationResponse>) functionCatalog.lookup("calculate");
        var response = function.apply(new CalculationRequest(1, 2 ));
        assertEquals(3, response.getResult(), 0.0005);
    }
}
