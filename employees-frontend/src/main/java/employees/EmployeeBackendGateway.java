package employees;

import lombok.AllArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmployeeBackendGateway {

    private StreamBridge streamBridge;

    public void send(String name) {
        streamBridge.send("createEmployee", new CreateEmployeeCommand(name));
    }
}
