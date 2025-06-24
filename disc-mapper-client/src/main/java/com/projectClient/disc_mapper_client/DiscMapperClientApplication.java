package com.projectClient.disc_mapper_client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.projectClient.disc_mapper_client.functionality.DriveFuncs;
import com.projectClient.disc_mapper_client.objects.ClientDTO;
import com.projectClient.disc_mapper_client.services.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

@SpringBootApplication
@EnableScheduling
public class DiscMapperClientApplication implements CommandLineRunner {

	private static final String SERVER_URL = "http://localhost:8080";

	private static final ObjectMapper mapper = new ObjectMapper();

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private ScheduledFuture<?> connectionTask;
	private ScheduledFuture<?> listeningTask;

	private boolean isConnected = false;

	@Autowired
	private DriveFuncs driveAPI;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ClientService clientService;


	public static void main(String[] args) {
		SpringApplication.run(DiscMapperClientApplication.class, args);
	}

	@Override
	public void run(String... args) {
		checkConnection();
	}

	private void checkConnection() {
		connectionTask = scheduler.scheduleWithFixedDelay(
				this::connectToServer,
				0,
				5000,
				TimeUnit.MILLISECONDS);
	}

	private void startListening() {
		connectionTask.cancel(true);

		listeningTask = scheduler.scheduleWithFixedDelay(
				() -> {
					if (!isConnected) {
						listeningTask.cancel(true);
						checkConnection();
					}

					checkForRequestsMap();
					checkForRequests();
				},
				0,
				2000,
				TimeUnit.MILLISECONDS);
	}

	public void connectToServer() {
		System.out.println("Connecting to server at: " + SERVER_URL);

		try {
			String response = restTemplate.getForObject(SERVER_URL + "/api/ping", String.class);
			System.out.println("Server response: " + response);

            if (("Server is running!").equals(response)) {
				isConnected = true;

				System.out.println("Client is connected.");
				startListening();
			} else {
				throw new RuntimeException("Unexpected ping response: " + response);
			}
		} catch (Exception e) {
			System.err.println("Server connection error: " + e.getMessage());
			isConnected = false;
		}
	}



	private void checkForRequests() {
		try {
			String request = restTemplate.getForObject(SERVER_URL + "/api/client/request", String.class);

			if (request != null && !request.isEmpty() && !request.equals("none")) {
				handleRequest(request);
			}
		} catch (Exception ignore) { }
	}
	private void checkForRequestsMap() {
		try {
			String request = restTemplate.getForObject(SERVER_URL + "/api/map/request", String.class);

			if (request != null && !request.isEmpty() && !request.equals("none")) {
				handleRequestMap(request);
			}
		} catch (Exception ignore) { }
	}



	private void handleRequest(String request) {
		System.out.println("Handling request: " + request);

		try {
			String[] parts = request.split("\\^");
			String command = parts[0];
			String param = parts.length > 1 ? parts[1] : "";

			switch (command) {
				case "clientData" -> {
					ClientDTO cdt = clientService.clientInfo();

					String json = mapper.writeValueAsString(cdt);
					sendResponse(command, json);
				}
				case "revokeAccess" -> {
					isConnected = false;
					System.err.println("User logged out!");
				}
				case "listDrives" -> sendResponse(command, driveAPI.listDrives());
				case "storage" -> sendResponse(command, driveAPI.storage(param));
				case "freeStorage" -> sendResponse(command, driveAPI.freeStorage(param));

				default -> sendResponse(command, "Unknown command");
			}
		} catch (JsonProcessingException e) {
			System.err.println("Error serializing response: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Error handling request: " + e.getMessage());
			sendResponse(request, "Error: " + e.getMessage());
		}
	}
	private void handleRequestMap(String request) {
		System.out.println("Handling request: " + request);

		try {
			String[] parts = request.split("\\^");
			String command = parts[0];
			String param = parts.length > 1 ? parts[1] : "";

			if (command.equals("mapDirectory")) {

				CompletableFuture<Map<String, Object>> future = driveAPI.mapDirectory(Paths.get(param));

				Map<String, Object> result = future.get();

				String json = mapper.writeValueAsString(result);
				sendResponseMap(command, json);
			}

		} catch (Exception e) {
			System.err.println("Error handling request: " + e.getMessage());
			sendResponse(request, "Error: " + e.getMessage());
		}
	}



	private void sendResponse(String command, Object data) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> response = new HashMap<>();
			response.put("command", command);
			response.put("data", data);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(response, headers);

			restTemplate.postForObject(
					SERVER_URL + "/api/client/response",
					entity,
					String.class
			);

			System.out.println("Response sent for command: " + command);
		} catch (Exception e) {
			System.err.println("Failed to send response: " + e.getMessage());
		}
	}
	private void sendResponseMap(String command, Object data) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			Map<String, Object> response = new HashMap<>();
			response.put("command", command);
			response.put("data", data);

			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(response, headers);

			restTemplate.postForObject(
					SERVER_URL + "/api/map/response",
					entity,
					String.class
			);

			System.out.println("Response sent for command: " + command);
		} catch (Exception e) {
			System.err.println("Failed to send response: " + e.getMessage());
		}
	}
}