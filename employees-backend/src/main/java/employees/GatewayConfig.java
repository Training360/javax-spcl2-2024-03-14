package employees;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration(proxyBeanMethods = false)
@Slf4j
@AllArgsConstructor
public class GatewayConfig {

    private EmployeesService employeesService;

    @Bean
    public Function<CreateEmployeeCommand, EmployeeHasBeenCreatedEvent> createEmployee() {
        return command -> {
            log.debug("Message has come: {}", command);
            var created = employeesService.createEmployee(new EmployeeResource(command.getName()));
            var event = new EmployeeHasBeenCreatedEvent(created.getId(), created.getName());
            return event;
        };
    }

    @Bean
    public Supplier<String> tick() {
        return () ->
                "Hello from Supplier " + LocalDateTime.now();
    }

}
