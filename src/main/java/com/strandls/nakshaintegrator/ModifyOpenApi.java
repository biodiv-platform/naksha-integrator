package com.strandls.nakshaintegrator;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ModifyOpenApi {
	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.err.println("Usage: ModifyOpenApi <path-to-openapi.json>");
			return;
		}

		// Load config.properties
		Properties properties = new Properties();
		try (InputStream input = ModifyOpenApi.class.getClassLoader().getResourceAsStream("config.properties")) {
			if (input != null) {
				properties.load(input);
			} else {
				System.out.println("config.properties not found. Proceeding with defaults.");
			}
		}

		// Get properties with defaults
		String title = properties.getProperty("title", "");
		String description = properties.getProperty("description", "");
		String version = properties.getProperty("version", "");

		String schemes = properties.getProperty("schemes", "http");
		String host = properties.getProperty("host", "localhost");
		String basePath = properties.getProperty("basePath", "");

		if (!basePath.startsWith("/") && !basePath.isEmpty()) {
			basePath = "/" + basePath;
		}

		String serverUrl = schemes + "://" + host + basePath;

		// Read OpenAPI file
		File openApiFile = new File(args[0]);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root = (ObjectNode) mapper.readTree(openApiFile);

		// Set or update info
		ObjectNode info = mapper.createObjectNode();
		info.put("title", title);
		info.put("description", description);
		info.put("version", version);
		root.set("info", info);

		// Set servers array
		ArrayNode servers = mapper.createArrayNode();
		ObjectNode server = mapper.createObjectNode();
		server.put("url", serverUrl);
		servers.add(server);
		root.set("servers", servers);

		// Remove Jersey multipart framework classes from schemas
		// These should not be part of the API contract as they are implementation details
		JsonNode components = root.get("components");
		if (components != null && components.has("schemas")) {
			ObjectNode schemas = (ObjectNode) components.get("schemas");

			// List of Jersey framework classes to remove from schema
			List<String> schemasToRemove = new ArrayList<>();
			schemasToRemove.add("FormDataBodyPart");
			schemasToRemove.add("FormDataMultiPart");
			schemasToRemove.add("FormDataContentDisposition");
			schemasToRemove.add("BodyPartEntity");
			schemasToRemove.add("ContentDisposition");
			schemasToRemove.add("MediaType");
			schemasToRemove.add("MultiPart");
			schemasToRemove.add("BodyPart");
			schemasToRemove.add("BodyPartHeaders");
			schemasToRemove.add("BodyPartParameterizedHeaders");

			for (String schemaName : schemasToRemove) {
				if (schemas.has(schemaName)) {
					schemas.remove(schemaName);
					System.out.println("Removed schema: " + schemaName);
				}
			}
		}

		// Write modified file
		mapper.writerWithDefaultPrettyPrinter().writeValue(openApiFile, root);
		System.out.println("OpenAPI file updated successfully.");
	}
}
