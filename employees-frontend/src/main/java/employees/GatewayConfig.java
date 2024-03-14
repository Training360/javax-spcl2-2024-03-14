package employees;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class GatewayConfig {

    @Bean
    public Consumer<EmployeeHasBeenCreatedEvent> employeeCreated() {
        return event -> {
            log.debug("Message has come: {}", event);
        };
    }

}
