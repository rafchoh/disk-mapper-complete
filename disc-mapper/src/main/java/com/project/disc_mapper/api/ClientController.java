package com.project.disc_mapper.api;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api")
public class ClientController {

    private String currentRequest = "none";
    private String currentRequestMap = "none";

    private Map<String, Object> latestResponse = new HashMap<>();
    private Map<String, Object> latestResponseMap = new HashMap<>();

    private boolean responseReceived = false;
    private boolean responseReceivedMap = false;



    @GetMapping("/ping")
    public String ping() {
        return "Server is running!";
    }



    @GetMapping("/client/request")
    public String sendRequest() {
        return currentRequest;
    }

    @GetMapping("/map/request")
    public String sendRequestMap() {
        return currentRequestMap;
    }



    @PostMapping("/client/response")
    public String receiveResponse(@RequestBody Map<String, Object> response) {

        System.out.println("Received response from client: " + response);

        this.latestResponse = response;
        this.responseReceived = true;

        this.currentRequest = "none";

        return "Response received!";
    }

    @PostMapping("/map/response")
    public String receiveResponseMap(@RequestBody Map<String, Object> response) {

        System.out.println("Received response from client: " + response);

        this.latestResponseMap = response;
        this.responseReceivedMap = true;

        this.currentRequestMap = "none";

        ClientService.ClientState state = ClientService.ClientState.getInstance();
        state.setBusy(false);

        return "Map received!";
    }


    public void revokeAccess() {

        this.currentRequest = "revokeAccess";

        this.responseReceived = false;
        this.latestResponse.clear();

        System.out.println("Sending request to client: " + this.currentRequest);

        int maxWaitTime = 3;
        int waitedTime = 0;

        while (waitedTime < maxWaitTime) {
            try {
                Thread.sleep(1000);
                waitedTime++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Error: Request interrupted!");
            }
        }

        this.responseReceived = true;
        this.currentRequest = "none";
    }

    public Object sendRequestToClient(String command, String parameter) {

        if (parameter != null && !parameter.isEmpty()) {
            this.currentRequest = command + "^" + parameter;
        } else {
            this.currentRequest = command;
        }

        this.responseReceived = false;
        this.latestResponse.clear();

        System.out.println("Sending request to client: " + this.currentRequest);

        return waitForResponse(() -> responseReceived, () -> latestResponse, () -> currentRequest = "none");
    }

    public Object sendRequestToMap(String command, String parameter) {

        ClientService.ClientState state = ClientService.ClientState.getInstance();
        state.setBusy(true);

        if (parameter != null && !parameter.isEmpty()) {
            this.currentRequestMap = command + "^" + parameter;
        } else {
            this.currentRequestMap = command;
        }

        this.responseReceivedMap = false;
        this.latestResponseMap.clear();

        System.out.println("Sending request to client: " + this.currentRequestMap);

        return waitForResponse(() -> responseReceivedMap, () -> latestResponseMap, () -> currentRequestMap = "none");
    }

    private Object waitForResponse(Supplier<Boolean> isReceived, Supplier<Map<String, Object>> getResponse, Runnable onTimeout) {
        int maxWaitTime = 360;
        int waitedTime = 0;

        while (!isReceived.get() && waitedTime < maxWaitTime) {
            try {
                Thread.sleep(1000);
                waitedTime++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Error: Request interrupted!";
            }
        }

        if (isReceived.get()) {
            return getResponse.get().get("data");
        } else {
            onTimeout.run();
            return "Error: Client did not respond within " + maxWaitTime + " seconds!";
        }
    }
}