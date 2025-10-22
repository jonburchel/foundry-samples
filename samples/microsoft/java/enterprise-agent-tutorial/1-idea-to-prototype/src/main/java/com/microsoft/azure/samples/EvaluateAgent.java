// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

// <imports_and_includes>
import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.AgentsClientBuilder;
import com.azure.ai.agents.ConversationsClient;
import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.*;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.openai.models.conversations.Conversation;
import com.openai.models.conversations.items.ItemCreateParams;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
// </imports_and_includes>

/**
 * Evaluation Script for Modern Workplace Assistant
 * Tests the agent with predefined business scenarios to assess quality.
 */
public class EvaluateAgent {

    private static Dotenv dotenv;
    private static AgentsClient agentsClient;
    private static ResponsesClient responsesClient;
    private static ConversationsClient conversationsClient;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        System.out.println("🧪 Modern Workplace Assistant - Evaluation");
        System.out.println("=".repeat(50));

        try {
            // Load environment variables
            dotenv = Dotenv.configure().ignoreIfMissing().load();

            // Create agent
            AgentCreationResult result = createWorkplaceAssistant();
            
            // Run evaluation
            List<EvaluationResult> results = runEvaluation(result.agent.getName(), result.mcpTool);
            
            // Save results
            saveResults(results);
            
            System.out.println("💾 Results saved to evaluation_results.json");
            
        } catch (Exception e) {
            System.err.println("❌ Evaluation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // <load_test_data>
    /**
     * Load test questions from JSONL file
     */
    private static List<TestQuestion> loadTestQuestions(String filepath) throws IOException {
        List<TestQuestion> questions = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    TestQuestion q = new TestQuestion();
                    q.question = json.get("question").getAsString();
                    
                    // Parse expected keywords array
                    if (json.has("expected_keywords")) {
                        json.getAsJsonArray("expected_keywords").forEach(elem -> 
                            q.expectedKeywords.add(elem.getAsString())
                        );
                    }
                    
                    questions.add(q);
                }
            }
        }
        
        return questions;
    }
    // </load_test_data>

    // <run_batch_evaluation>
    /**
     * Run evaluation with test questions
     */
    private static List<EvaluationResult> runEvaluation(String agentName, MCPTool mcpTool) {
        List<TestQuestion> questions;
        try {
            questions = loadTestQuestions("questions.jsonl");
        } catch (IOException e) {
            System.err.println("⚠️  Could not load questions.jsonl, using default questions");
            questions = getDefaultQuestions();
        }
        
        List<EvaluationResult> results = new ArrayList<>();
        
        System.out.println("🧪 Running evaluation with " + questions.size() + " test questions...");
        
        for (int i = 0; i < questions.size(); i++) {
            TestQuestion q = questions.get(i);
            System.out.println(String.format("📝 Question %d/%d: %s", i + 1, questions.size(), q.question));
            
            ChatResult chatResult = chatWithAssistant(agentName, mcpTool, q.question);
            
            // Simple evaluation: check if response contains expected keywords
            boolean containsExpected = false;
            if (chatResult.response != null) {
                String responseLower = chatResult.response.toLowerCase();
                containsExpected = q.expectedKeywords.stream()
                        .anyMatch(keyword -> responseLower.contains(keyword.toLowerCase()));
            }
            
            EvaluationResult result = new EvaluationResult();
            result.question = q.question;
            result.response = chatResult.response;
            result.status = chatResult.status;
            result.containsExpected = containsExpected;
            result.expectedKeywords = q.expectedKeywords;
            
            results.add(result);
            
            String statusIcon = containsExpected ? "✅" : "⚠️";
            System.out.println(String.format("%s Response length: %d chars (Status: %s)", 
                    statusIcon, chatResult.response != null ? chatResult.response.length() : 0, chatResult.status));
        }
        
        return results;
    }
    // </run_batch_evaluation>

    // <evaluation_results>
    /**
     * Save evaluation results to JSON file
     */
    private static void saveResults(List<EvaluationResult> results) throws IOException {
        // Calculate pass rate
        long passed = results.stream().filter(r -> r.containsExpected).count();
        System.out.println(String.format("\n📊 Evaluation Results: %d/%d questions passed", passed, results.size()));
        
        try (FileWriter writer = new FileWriter("evaluation_results.json")) {
            gson.toJson(results, writer);
        }
    }
    // </evaluation_results>

    /**
     * Default test questions if file not found
     */
    private static List<TestQuestion> getDefaultQuestions() {
        List<TestQuestion> questions = new ArrayList<>();
        
        TestQuestion q1 = new TestQuestion();
        q1.question = "What is our remote work policy regarding security requirements?";
        q1.expectedKeywords.add("remote");
        q1.expectedKeywords.add("work");
        q1.expectedKeywords.add("security");
        questions.add(q1);
        
        TestQuestion q2 = new TestQuestion();
        q2.question = "How do I set up Azure Active Directory conditional access?";
        q2.expectedKeywords.add("Azure");
        q2.expectedKeywords.add("Active Directory");
        q2.expectedKeywords.add("conditional access");
        questions.add(q2);
        
        TestQuestion q3 = new TestQuestion();
        q3.question = "What collaboration tools are approved for internal use?";
        q3.expectedKeywords.add("Teams");
        q3.expectedKeywords.add("SharePoint");
        q3.expectedKeywords.add("collaboration");
        questions.add(q3);
        
        TestQuestion q4 = new TestQuestion();
        q4.question = "What Azure AD configuration should I implement to comply with our company's remote work security policy?";
        q4.expectedKeywords.add("remote");
        q4.expectedKeywords.add("Azure");
        q4.expectedKeywords.add("AD");
        q4.expectedKeywords.add("policy");
        questions.add(q4);
        
        return questions;
    }

    /**
     * Create workplace assistant (similar to main class but simplified)
     */
    private static AgentCreationResult createWorkplaceAssistant() {
        System.out.println("🤖 Creating Modern Workplace Assistant for evaluation...");

        // Authentication setup
        String aiFpoundryTenantId = dotenv.get("AI_FOUNDRY_TENANT_ID");
        TokenCredential credential;
        if (aiFpoundryTenantId != null && !aiFpoundryTenantId.isEmpty()) {
            credential = new AzureCliCredentialBuilder().build();
        } else {
            credential = new DefaultAzureCredentialBuilder().build();
        }

        String endpoint = dotenv.get("PROJECT_ENDPOINT");
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");

        // Build clients
        AgentsClientBuilder builder = new AgentsClientBuilder()
                .credential(credential)
                .endpoint(endpoint);

        agentsClient = builder.buildClient();
        responsesClient = builder.buildResponsesClient();
        conversationsClient = builder.buildConversationsClient();

        // SharePoint integration
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        SharepointAgentTool sharepointTool = null;

        try {
            ToolProjectConnectionList connections = new ToolProjectConnectionList();
            SharepointGroundingToolParameters groundingParams = new SharepointGroundingToolParameters()
                    .setProjectConnections(connections);
            
            sharepointTool = new SharepointAgentTool()
                    .setSharepointGrounding(groundingParams);
        } catch (Exception e) {
            System.out.println("⚠️  SharePoint connection not available for evaluation");
        }

        // MCP integration
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL");
        MCPTool mcpTool = new MCPTool()
                .setServerLabel("microsoft_learn")
                .setServerUrl(mcpServerUrl)
                .setAllowedTools(new ArrayList<>());

        // Agent creation
        String instructions = """
You are a Modern Workplace Assistant for Contoso Corporation.
Provide comprehensive answers combining internal policies with external technical guidance.
Always cite your sources and provide step-by-step guidance.
""";

        List<Object> availableTools = new ArrayList<>();
        if (sharepointTool != null) {
            availableTools.add(sharepointTool);
        }
        availableTools.add(mcpTool);

        PromptAgentDefinition definition = new PromptAgentDefinition(modelDeploymentName)
                .setInstructions(instructions)
                .setTools(availableTools);

        AgentVersionObject agent = agentsClient.createAgentVersion(
                "Modern Workplace Assistant Eval",
                definition
        );

        System.out.println("✅ Agent created: " + agent.getId());
        
        return new AgentCreationResult(agent, mcpTool, sharepointTool);
    }

    /**
     * Chat with assistant (simplified version)
     */
    private static ChatResult chatWithAssistant(String agentName, MCPTool mcpTool, String message) {
        try {
            Conversation conversation = conversationsClient.getOpenAIClient().create();

            conversationsClient.getOpenAIClient().items().create(
                    ItemCreateParams.builder()
                            .conversationId(conversation.id())
                            .addItem(EasyInputMessage.builder()
                                    .role(EasyInputMessage.Role.USER)
                                    .content(message)
                                    .build())
                            .build()
            );

            AgentReference agentReference = new AgentReference(agentName);
            Response response = responsesClient.createWithAgentConversation(
                    agentReference,
                    conversation.id()
            );

            String fullResponse = response.output() != null ? response.output().toString() : "";
            
            return new ChatResult(fullResponse, "completed");

        } catch (Exception e) {
            return new ChatResult("Error in conversation: " + e.getMessage(), "failed");
        }
    }

    // Helper classes
    private static class AgentCreationResult {
        AgentVersionObject agent;
        MCPTool mcpTool;
        SharepointAgentTool sharepointTool;

        AgentCreationResult(AgentVersionObject agent, MCPTool mcpTool, SharepointAgentTool sharepointTool) {
            this.agent = agent;
            this.mcpTool = mcpTool;
            this.sharepointTool = sharepointTool;
        }
    }

    private static class TestQuestion {
        String question;
        List<String> expectedKeywords = new ArrayList<>();
    }

    private static class EvaluationResult {
        String question;
        String response;
        String status;
        boolean containsExpected;
        List<String> expectedKeywords;
    }

    private static class ChatResult {
        String response;
        String status;

        ChatResult(String response, String status) {
            this.response = response;
            this.status = status;
        }
    }
}
