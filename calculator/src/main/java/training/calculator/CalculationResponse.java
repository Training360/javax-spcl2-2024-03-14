package training.calculator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalculationResponse {

    public double result;

    public RoundResponse round() {
        return new RoundResponse(BigDecimal.valueOf(result).setScale(2, RoundingMode.HALF_UP).toString());
    }
}
