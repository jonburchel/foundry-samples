// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

import com.azure.ai.agents.ConversationsClient;
import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.AgentReference;
import com.azure.ai.agents.models.AgentVersionObject;
import com.openai.models.conversations.Conversation;
import com.openai.models.conversations.items.ItemCreateParams;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * Handles evaluation dataset creation and baseline evaluation.
 * Establishes quality baselines using ground truth data.
 */
public class EvaluationFramework {
    
    private final ResponsesClient responsesClient;
    private final ConversationsClient conversationsClient;
    private final Gson gson;
    
    public EvaluationFramework(ResponsesClient responsesClient, ConversationsClient conversationsClient, Gson gson) {
        this.responsesClient = responsesClient;
        this.conversationsClient = conversationsClient;
        this.gson = gson;
    }
    
    /**
     * Create an evaluation dataset with ground truth data.
     */
    public void createEvaluationDataset() {
        System.out.println("\n📊 CREATING EVALUATION DATASET");
        System.out.println("=".repeat(70));

        // <evaluation_dataset_structure>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        List<Map<String, Object>> evalDataset = Arrays.asList(
                createEvalItem("eval_001",
                        "What is our remote work security policy?",
                        "sharepoint",
                        Arrays.asList("remote work", "security", "policy"),
                        0.7),
                createEvalItem("eval_002",
                        "How do I configure Azure AD conditional access?",
                        "microsoft_learn",
                        Arrays.asList("azure ad", "conditional access", "configuration"),
                        0.7),
                createEvalItem("eval_003",
                        "What Azure security features should I use to comply with our data protection policy?",
                        "both",
                        Arrays.asList("azure security", "data protection", "compliance"),
                        0.75),
                createEvalItem("eval_004",
                        "Explain our company's password policy requirements.",
                        "sharepoint",
                        Arrays.asList("password", "policy", "requirements"),
                        0.7),
                createEvalItem("eval_005",
                        "How do I enable multi-factor authentication in Microsoft 365?",
                        "microsoft_learn",
                        Arrays.asList("mfa", "multi-factor", "authentication", "microsoft 365"),
                        0.75)
        );
        // </evaluation_dataset_structure>

        // Save as JSONL format
        try {
            FileWriter writer = new FileWriter("evaluation_dataset.jsonl");
            for (Map<String, Object> item : evalDataset) {
                writer.write(gson.toJson(item) + "\n");
            }
            writer.close();
            System.out.println("✅ Created evaluation dataset: evaluation_dataset.jsonl");
            System.out.println("   Total items: " + evalDataset.size());
        } catch (IOException e) {
            System.err.println("❌ Error saving evaluation dataset: " + e.getMessage());
        }
    }
    
    /**
     * Run baseline evaluation using the evaluation dataset.
     * 
     * @param agent The agent to evaluate
     * @return Map containing evaluation results
     */
    public Map<String, Object> runBaselineEvaluation(AgentVersionObject agent) {
        System.out.println("\n📈 RUNNING BASELINE EVALUATION");
        System.out.println("=".repeat(70));

        List<Map<String, Object>> evalResults = new ArrayList<>();
        int passedCount = 0;
        double totalScore = 0.0;

        try {
            // Read evaluation dataset
            List<String> lines = Files.readAllLines(Paths.get("evaluation_dataset.jsonl"));
            
            for (String line : lines) {
                @SuppressWarnings("unchecked")
                Map<String, Object> evalItem = gson.fromJson(line, Map.class);
                
                String id = (String) evalItem.get("id");
                String question = (String) evalItem.get("question");
                @SuppressWarnings("unchecked")
                List<String> expectedTopics = (List<String>) evalItem.get("expected_topics");
                double threshold = ((Number) evalItem.get("quality_threshold")).doubleValue();

                System.out.println("📝 Evaluating: " + id);
                System.out.println("   Question: " + question.substring(0, Math.min(50, question.length())) + "...");

                try {
                    String response = chatWithAssistant(agent.getName(), question);
                    
                    // <quality_metric_calculation>
                    // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
                    double qualityScore = calculateQualityScore(response, expectedTopics);
                    // </quality_metric_calculation>

                    boolean passed = qualityScore >= threshold;
                    if (passed) passedCount++;
                    totalScore += qualityScore;

                    Map<String, Object> evalResult = new HashMap<>();
                    evalResult.put("id", id);
                    evalResult.put("question", question);
                    evalResult.put("response", response);
                    evalResult.put("quality_score", qualityScore);
                    evalResult.put("threshold", threshold);
                    evalResult.put("passed", passed);
                    evalResult.put("timestamp", Instant.now().toString());

                    evalResults.add(evalResult);

                    String status = passed ? "✅ PASS" : "❌ FAIL";
                    System.out.println("   " + status + " - Quality: " + String.format("%.2f", qualityScore) + " (threshold: " + threshold + ")");

                } catch (Exception e) {
                    System.out.println("   ⚠️  Error evaluating: " + e.getMessage());
                }

                System.out.println();
            }

            double passRate = (passedCount / (double) lines.size()) * 100.0;
            double avgQualityScore = totalScore / lines.size();

            Map<String, Object> baselineEvaluation = new HashMap<>();
            baselineEvaluation.put("evaluation_date", Instant.now().toString());
            baselineEvaluation.put("agent_id", agent.getId());
            baselineEvaluation.put("agent_name", agent.getName());
            baselineEvaluation.put("total_items", lines.size());
            baselineEvaluation.put("passed_items", passedCount);
            baselineEvaluation.put("pass_rate", passRate);
            baselineEvaluation.put("average_quality_score", avgQualityScore);
            baselineEvaluation.put("evaluation_results", evalResults);

            saveToFile(baselineEvaluation, "baseline_evaluation.json");

            System.out.println("📊 BASELINE EVALUATION COMPLETE");
            System.out.println("   Pass Rate: " + String.format("%.1f%%", passRate));
            System.out.println("   Avg Quality Score: " + String.format("%.2f", avgQualityScore));
            System.out.println("   Report saved: baseline_evaluation.json");

            return baselineEvaluation;

        } catch (IOException e) {
            System.err.println("❌ Error reading evaluation dataset: " + e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Chat with the assistant and get a response.
     */
    private String chatWithAssistant(String agentName, String message) {
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

            return response.output() != null ? response.output().toString() : "";

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Calculate quality score based on topic coverage.
     */
    private double calculateQualityScore(String response, List<String> expectedTopics) {
        String lowerResponse = response.toLowerCase();
        int topicsFound = 0;

        for (String topic : expectedTopics) {
            if (lowerResponse.contains(topic.toLowerCase())) {
                topicsFound++;
            }
        }

        return topicsFound / (double) expectedTopics.size();
    }
    
    private Map<String, Object> createEvalItem(String id, String question, String source, 
                                                List<String> topics, double threshold) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", id);
        item.put("question", question);
        item.put("expected_source", source);
        item.put("expected_topics", topics);
        item.put("quality_threshold", threshold);
        return item;
    }
    
    private void saveToFile(Object data, String filename) {
        try {
            FileWriter writer = new FileWriter(filename);
            gson.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            System.err.println("Error saving file " + filename + ": " + e.getMessage());
        }
    }
}
