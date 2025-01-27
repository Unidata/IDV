package ucar.unidata.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;


public class GeminiService {

    private final String apiKey;
    private final String baseUrl;
    private static final ObjectMapper mapper = new ObjectMapper();

    public GeminiService(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public GeminiResponse generateContent(GeminiRequest request, String model) throws IOException {
        return generateContent(request,model,null);
    }

    public GeminiResponse generateContent(GeminiRequest request, String model, String systemInstruction) throws IOException {
        URL url = new URL(baseUrl + model + ":generateContent?key=" + apiKey);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        if(systemInstruction != null && !systemInstruction.isEmpty()) {
            request.setContents(systemInstruction + "\n" + request.getContents());
        }

        String requestBody = mapper.writeValueAsString(request);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (InputStreamReader streamReader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
             java.io.BufferedReader reader = new java.io.BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        con.disconnect();

        return mapper.readValue(response.toString(), GeminiResponse.class);

    }


    public GeminiCountResponse countTokens(GeminiRequest request, String model) throws IOException {
        return countTokens(request,model,null);
    }

    public GeminiCountResponse countTokens(GeminiRequest request, String model, String systemInstruction) throws IOException {

        URL url = new URL(baseUrl + model + ":countTokens?key=" + apiKey);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        if(systemInstruction != null && !systemInstruction.isEmpty()) {
            request.setContents(systemInstruction + "\n" + request.getContents());
        }
        String requestBody = mapper.writeValueAsString(request);

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        try (InputStreamReader streamReader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
             java.io.BufferedReader reader = new java.io.BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        con.disconnect();

        return mapper.readValue(response.toString(), GeminiCountResponse.class);
    }

    public GeminiResponse getCompletionWithImage(GeminiRequestWithImage request, String model) throws IOException {
        return getCompletionWithImage(request,model, null);
    }

    public GeminiResponse getCompletionWithImage(GeminiRequestWithImage request, String model, String systemInstruction) throws IOException {
        // construct URL
        URL url = new URL(baseUrl + model + ":generateContent?key=" + apiKey);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);

        //Convert request into JSON
        if(systemInstruction != null && !systemInstruction.isEmpty()) {
            request.setTextPrompt(systemInstruction + "\n" + request.getTextPrompt());
        }
        String requestBody = mapper.writeValueAsString(request);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        StringBuilder response = new StringBuilder();
        try (InputStreamReader streamReader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8);
             java.io.BufferedReader reader = new java.io.BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        con.disconnect();
        return mapper.readValue(response.toString(), GeminiResponse.class);
    }

    public static class GeminiRequestWithImage {
        private String textPrompt;
        private String image; // base64 encoded image

        public String getTextPrompt() { return textPrompt;}
        public void setTextPrompt(String textPrompt) { this.textPrompt = textPrompt; }

        public String getImage() { return image;}
        public void setImage(String image) { this.image = image; }

    }
    // other Gemini API calls will go here, like "getModels"
    // inner class replacements for data models in com.kousenit.gemini
    public static class GeminiRequest {
        private String contents;

        public String getContents() { return contents;}
        public void setContents(String contents) { this.contents = contents;}
    }

    public static class GeminiResponse {
        private String content;

        public String getContent() {return content;}
        public void setContent(String content) {this.content = content;}
    }

    public static class GeminiCountResponse {
        private int tokenCount;
        public int getTokenCount() {return tokenCount;}
        public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }
    }

    public static class ModelList {
        private List<String> models;

        public List<String> getModels() { return models; }
        public void setModels(List<String> models) { this.models = models; }
    }

    public static void main(String[] args) {
        String apiKey = "YOUR_API_KEY"; // Replace with your actual API key
        String baseUrl = "https://generative-ai.googleapis.com/v1beta/models/"; // Replace with the Gemini API base URL
        String modelName = "gemini-1.0-pro";

        GeminiService service = new GeminiService(apiKey, baseUrl);
        GeminiService.GeminiRequest request = new GeminiService.GeminiRequest();
        request.setContents("Write a short story about a cat");

        try {
            GeminiService.GeminiResponse response = service.generateContent(request, modelName);
            System.out.println("Generated Content: " + response.getContent());
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

        try {
            GeminiService.GeminiCountResponse countResponse = service.countTokens(request, modelName);
            System.out.println("Token Count: " + countResponse.getTokenCount());
        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }

    }

    public static void testImage(String[] args) {
        String apiKey = "YOUR_API_KEY"; // Replace with your actual API key
        String baseUrl = "https://generative-ai.googleapis.com/v1beta/models/"; // Replace with the Gemini API base URL
        String modelName = "gemini-1.0-pro-vision"; // Make sure this model supports image input
        String imagePath = "path/to/your/image.jpg"; // Replace with the actual path to your image file

        GeminiService service = new GeminiService(apiKey, baseUrl);

        try {
            // Read image and encode to Base64
            byte[] imageData = Files.readAllBytes(Paths.get(imagePath));
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            // Create GeminiRequestWithImage object
            GeminiService.GeminiRequestWithImage request = new GeminiService.GeminiRequestWithImage();
            request.setTextPrompt("Describe the content of this image");
            request.setImage(base64Image);

            // Make API call
            GeminiService.GeminiResponse response = service.getCompletionWithImage(request, modelName);

            // Print the result
            System.out.println("Generated Content: " + response.getContent());

        } catch (IOException e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}