import com.azure.ai.projects.*;
import com.azure.ai.projects.models.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Evaluate {
    private static AIProjectClient projectClient;
    private static Dotenv dotenv;
    
    static {
        dotenv = Dotenv.configure().ignoreIfMissing().load();
        String projectEndpoint = dotenv.get("PROJECT_ENDPOINT");
        projectClient = new AIProjectClientBuilder()
            .endpoint(projectEndpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }
    
    static class EvalResult {
        String question;
        String response;
        String status;
        int responseLength;
        boolean passed;
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("🧪 Modern Workplace Assistant - Evaluation");
        System.out.println("=".repeat(50));
        
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL");
        
        System.out.println("🤖 Creating Modern Workplace Assistant...");
        Map<String, Object> mcpTool = new HashMap<>();
        mcpTool.put("type", "mcp");
        mcpTool.put("server_label", "microsoft_learn");
        mcpTool.put("server_url", mcpServerUrl);
        mcpTool.put("require_approval", "never");
        
        Agent agent = projectClient.createAgent(
            modelDeploymentName,
            "Modern Workplace Assistant",
            "You are a helpful assistant with access to Microsoft Learn documentation.",
            List.of(mcpTool)
        );
        System.out.println("✅ Agent created successfully: " + agent.getId());
        
        // Load questions
        List<String> questions = new ArrayList<>();
        Path questionsPath = Paths.get("questions.jsonl");
        for (String line : Files.readAllLines(questionsPath)) {
            JsonObject obj = JsonParser.parseString(line).getAsJsonObject();
            questions.add(obj.get("question").getAsString());
        }
        
        System.out.println("🧪 Running evaluation with " + questions.size() + " test questions...");
        
        List<EvalResult> results = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            String question = questions.get(i);
            System.out.println("📝 Question " + (i + 1) + "/" + questions.size() + ": " + question);
            
            AgentThread thread = projectClient.createThread();
            projectClient.createMessage(thread.getId(), "user", question);
            ThreadRun run = projectClient.createRun(thread.getId(), agent.getId());
            
            while ("queued".equals(run.getStatus()) || "in_progress".equals(run.getStatus())) {
                TimeUnit.MILLISECONDS.sleep(500);
                run = projectClient.getRun(thread.getId(), run.getId());
            }
            
            List<ThreadMessage> messages = projectClient.listMessages(thread.getId());
            StringBuilder response = new StringBuilder();
            for (ThreadMessage msg : messages) {
                if ("assistant".equals(msg.getRole())) {
                    for (MessageContent content : msg.getContent()) {
                        if (content instanceof MessageTextContent) {
                            response.append(((MessageTextContent) content).getText());
                        }
                    }
                }
            }
            
            EvalResult result = new EvalResult();
            result.question = question;
            result.response = response.toString();
            result.status = run.getStatus();
            result.responseLength = response.length();
            result.passed = "completed".equals(run.getStatus()) && response.length() > 50;
            results.add(result);
            
            System.out.println("✅ Response length: " + response.length() + " chars (Status: " + run.getStatus() + ")");
        }
        
        long passedCount = results.stream().filter(r -> r.passed).count();
        System.out.println("\n📊 Evaluation Results: " + passedCount + "/" + results.size() + " questions passed");
        
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.writeString(Paths.get("evaluation_results.json"), gson.toJson(results));
        System.out.println("💾 Results saved to evaluation_results.json");
    }
}
