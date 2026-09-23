package com.smipl.lcrecon.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * OpenAI client supporting the Responses API format with structured output (json_schema).
 */
public class AzureOpenAIClient {
    private static final Logger logger = LoggerFactory.getLogger(AzureOpenAIClient.class);

    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public AzureOpenAIClient(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
        // Strip "Bearer " prefix if user included it - we add it ourselves
        this.apiKey = apiKey != null && apiKey.startsWith("Bearer ") ? apiKey.substring(7) : apiKey;
        this.model = model;
    }

    /**
     * Response wrapper that includes the text and token usage for cost tracking.
     */
    public static class GptResponse {
        public final String text;
        public final int promptTokens;
        public final int completionTokens;
        public final int totalTokens;

        public GptResponse(String text, int promptTokens, int completionTokens, int totalTokens) {
            this.text = text;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }
    }

    /**
     * Call OpenAI with structured JSON schema output (Responses API format).
     *
     * @param systemPrompt  The system prompt text
     * @param userContent   The user message content (OCR text + context)
     * @param responseSchema JSON string of the schema object (the "schema" part inside text.format)
     *                       If null, falls back to json_object format
     * @param schemaName    Name for the schema (e.g., "lc_parameter", "invoice_data")
     * @return The response content as JSON string
     */
    public GptResponse chatCompletion(String systemPrompt, String userContent,
                                  String responseSchema, String schemaName) throws Exception {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", model);

        // Build input array (Responses API format)
        JsonArray input = new JsonArray();

        // System message
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        JsonArray systemContent = new JsonArray();
        JsonObject systemTextItem = new JsonObject();
        systemTextItem.addProperty("type", "input_text");
        systemTextItem.addProperty("text", systemPrompt);
        systemContent.add(systemTextItem);
        systemMsg.add("content", systemContent);
        input.add(systemMsg);

        // User message
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        JsonArray userContentArr = new JsonArray();
        JsonObject userTextItem = new JsonObject();
        userTextItem.addProperty("type", "input_text");
        userTextItem.addProperty("text", userContent);
        userContentArr.add(userTextItem);
        userMsg.add("content", userContentArr);
        input.add(userMsg);

        requestBody.add("input", input);

        // Build text.format with json_schema
        JsonObject textObj = new JsonObject();
        if (responseSchema != null && !responseSchema.trim().isEmpty()) {
            JsonObject formatObj = new JsonObject();
            formatObj.addProperty("type", "json_schema");
            formatObj.addProperty("name", schemaName != null ? schemaName : "response");
            formatObj.addProperty("strict", true);
            formatObj.add("schema", JsonParser.parseString(responseSchema));
            textObj.add("format", formatObj);
        } else {
            // Fallback: simple json_object format
            JsonObject formatObj = new JsonObject();
            formatObj.addProperty("type", "json_object");
            textObj.add("format", formatObj);
        }
        requestBody.add("text", textObj);

        logger.debug("OpenAI request payload size: {} chars", requestBody.toString().length());

        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createLongRunning()) {
            HttpPost post = new HttpPost(apiUrl);
            post.setHeader("Authorization", "Bearer " + apiKey);
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode != 200) {
                    logger.error("OpenAI API error {}: {}", statusCode, responseBody);
                    throw new RuntimeException("OpenAI API returned " + statusCode + ": " + responseBody);
                }

                String text = extractResponseText(responseBody);
                int[] tokens = extractTokenUsage(responseBody);
                return new GptResponse(text, tokens[0], tokens[1], tokens[2]);
            }
        }
    }

    /**
     * Extract token usage from the API response.
     * Returns [promptTokens, completionTokens, totalTokens].
     */
    private int[] extractTokenUsage(String responseBody) {
        try {
            JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();
            if (result.has("usage")) {
                JsonObject usage = result.getAsJsonObject("usage");
                int prompt = usage.has("input_tokens") ? usage.get("input_tokens").getAsInt()
                        : (usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0);
                int completion = usage.has("output_tokens") ? usage.get("output_tokens").getAsInt()
                        : (usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0);
                int total = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : (prompt + completion);
                return new int[]{prompt, completion, total};
            }
        } catch (Exception e) {
            logger.warn("Could not extract token usage: {}", e.getMessage());
        }
        return new int[]{0, 0, 0};
    }

    /**
     * Extract the text content from the Responses API response.
     * The response format has output[].content[].text
     */
    private String extractResponseText(String responseBody) {
        JsonObject result = JsonParser.parseString(responseBody).getAsJsonObject();

        // Try Responses API format: output[].content[].text
        if (result.has("output")) {
            JsonArray output = result.getAsJsonArray("output");
            for (JsonElement item : output) {
                JsonObject outputItem = item.getAsJsonObject();
                if ("message".equals(outputItem.get("type").getAsString())) {
                    JsonArray content = outputItem.getAsJsonArray("content");
                    if (content != null && content.size() > 0) {
                        return content.get(0).getAsJsonObject().get("text").getAsString();
                    }
                }
            }
        }

        // Fallback: Chat Completions format: choices[].message.content
        if (result.has("choices")) {
            JsonArray choices = result.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                return choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
            }
        }

        throw new RuntimeException("Could not extract response text from OpenAI response");
    }

    /**
     * Send a minimal structured request, exactly the shape the reconciliation prompts use.
     *
     * A schema is supplied deliberately: without one the call falls back to
     * {@code text.format: json_object}, which the Responses API rejects unless the prompt itself
     * asks for JSON - so a schema-less probe fails even when the key and model are perfectly good.
     *
     * @return null when the connection is good, otherwise a description of what went wrong.
     */
    public String testConnection() {
        String schema = "{\"type\":\"object\",\"additionalProperties\":false,"
                + "\"properties\":{\"ok\":{\"type\":\"boolean\"}},\"required\":[\"ok\"]}";
        try {
            GptResponse response = chatCompletion(
                    "You are a connectivity test. Reply strictly using the provided JSON schema.",
                    "Return ok = true.", schema, "connection_test");
            if (response != null && response.text != null && !response.text.trim().isEmpty()) {
                return null;
            }
            return "The model returned an empty response.";
        } catch (Exception e) {
            logger.error("OpenAI connection test failed for url={} model={}", apiUrl, model, e);
            String detail = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return detail.length() > 400 ? detail.substring(0, 400) + "..." : detail;
        }
    }
}
