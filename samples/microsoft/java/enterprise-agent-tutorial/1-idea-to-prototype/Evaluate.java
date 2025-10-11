import com.azure.ai.projects.AIProjectClient;
import com.azure.ai.projects.AIProjectClientBuilder;
import com.azure.ai.projects.models.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Evaluate {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String projectEndpoint = dotenv.get("PROJECT_ENDPOINT");
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        
        AIProjectClient client = new AIProjectClientBuilder()
            .endpoint(projectEndpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
        
        System.out.println("🧪 Modern Workplace Assistant Evaluation\n");
        
        List<ToolDefinition> tools = new ArrayList<>();
        
        if (sharepointResourceName != null && !sharepointResourceName.isEmpty()) {
            try {
                ConnectionResponse connection = client.getConnection(sharepointResourceName);
                if (connection != null) {
                    tools.add(new SharePointToolDefinition(connection.getId()));
                    System.out.println("✅ SharePoint configured for evaluation\n");
                }
            } catch (Exception e) {
                System.out.println("⚠️  SharePoint unavailable: " + e.getMessage() + "\n");
            }
        }
        
        String instructions = "You are a Modern Workplace Assistant for Contoso Corporation. " +
            "Answer questions using available tools and provide specific, detailed responses.";
        
        Agent agent = client.createAgent(
            modelDeploymentName,
            new AgentCreationOptions()
                .setName("Evaluation Agent")
                .setInstructions(instructions)
                .setTools(tools)
        );
        
        List<Map<String, String>> questions = new ArrayList<>();
        Gson gson = new Gson();
        
        try (BufferedReader br = Files.newBufferedReader(Path.of("questions.jsonl"))) {
            String line;
            while ((line = br.readLine()) != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> q = gson.fromJson(line, Map.class);
                questions.add(q);
            }
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        System.out.println("Running " + questions.size() + " evaluation questions...\n");
        
        for (int i = 0; i < questions.size(); i++) {
            Map<String, String> q = questions.get(i);
            String question = q.get("question");
            String[] expectedKeywords = q.containsKey("expected_keywords") ? 
                q.get("expected_keywords").split(",") : new String[0];
            
            System.out.println("Question " + (i + 1) + "/" + questions.size() + ": " + question);
            
            AgentThread thread = client.createThread();
            client.createMessage(thread.getId(), MessageRole.USER, question);
            
            ThreadRun run = client.createRun(thread.getId(), agent.getId());
            
            while (run.getStatus() == RunStatus.QUEUED || run.getStatus() == RunStatus.IN_PROGRESS) {
                TimeUnit.MILLISECONDS.sleep(1000);
                run = client.getRun(thread.getId(), run.getId());
            }
            
            String response = "";
            if (run.getStatus() == RunStatus.COMPLETED) {
                List<ThreadMessage> messages = client.listMessages(thread.getId(), 
                    new ListMessagesOptions().setOrder(ListSortOrder.DESCENDING));
                
                for (ThreadMessage message : messages) {
                    if (message.getRole() == MessageRole.ASSISTANT) {
                        for (MessageContent content : message.getContentItems()) {
                            if (content instanceof MessageTextContent) {
                                response = ((MessageTextContent) content).getText();
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            
            boolean passed = response.length() > 50;
            if (expectedKeywords.length > 0) {
                boolean hasKeyword = false;
                for (String keyword : expectedKeywords) {
                    if (response.toLowerCase().contains(keyword.trim().toLowerCase())) {
                        hasKeyword = true;
                        break;
                    }
                }
                passed = passed && hasKeyword;
            }
            
            System.out.println("   Status: " + (passed ? "✅ PASS" : "❌ FAIL"));
            System.out.println("   Response length: " + response.length() + " characters\n");
            
            Map<String, Object> result = new HashMap<>();
            result.put("question", question);
            result.put("response", response);
            result.put("passed", passed);
            result.put("response_length", response.length());
            results.add(result);
            
            client.deleteThread(thread.getId());
        }
        
        client.deleteAgent(agent.getId());
        
        long passedCount = results.stream().filter(r -> (Boolean) r.get("passed")).count();
        long failedCount = results.size() - passedCount;
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("total_questions", questions.size());
        summary.put("passed", passedCount);
        summary.put("failed", failedCount);
        summary.put("results", results);
        
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(summary);
        Files.write(Path.of("evaluation_results.json"), json.getBytes());
        
        System.out.println("📊 Evaluation Complete:");
        System.out.println("   Total: " + questions.size());
        System.out.println("   Passed: " + passedCount);
        System.out.println("   Failed: " + failedCount);
        System.out.println("\n📄 Results saved to evaluation_results.json");
    }
}
