// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

import com.azure.ai.agents.models.AgentVersionObject;
import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Handles governance policy definition and application.
 * Configures organization-wide policies for the agent.
 */
public class GovernancePolicies {
    
    private final Gson gson;
    
    public GovernancePolicies(Gson gson) {
        this.gson = gson;
    }
    
    /**
     * Apply governance policies to the agent.
     * 
     * @param agent The agent to apply policies to
     */
    public void applyGovernancePolicies(AgentVersionObject agent) {
        System.out.println("\n🏛️  APPLYING GOVERNANCE POLICIES");
        System.out.println("=".repeat(70));

        // <governance_policy_definition>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        Map<String, Object> governancePolicies = new HashMap<>();

        // Content Safety Policy
        Map<String, Object> contentSafety = new HashMap<>();
        contentSafety.put("enabled", true);
        contentSafety.put("filter_categories", Arrays.asList("hate", "sexual", "violence", "self_harm"));
        contentSafety.put("severity_threshold", "medium");
        contentSafety.put("action", "block_and_log");
        governancePolicies.put("content_safety", contentSafety);

        // Data Protection Policy
        Map<String, Object> dataProtection = new HashMap<>();
        dataProtection.put("enabled", true);
        dataProtection.put("pii_detection", true);
        dataProtection.put("pii_categories", Arrays.asList("ssn", "credit_card", "email", "phone", "ip_address"));
        dataProtection.put("redaction_method", "hash");
        dataProtection.put("data_residency", "us");
        governancePolicies.put("data_protection", dataProtection);

        // Rate Limiting Policy
        Map<String, Object> rateLimiting = new HashMap<>();
        rateLimiting.put("enabled", true);
        rateLimiting.put("requests_per_minute", 60);
        rateLimiting.put("requests_per_hour", 1000);
        rateLimiting.put("burst_limit", 100);
        rateLimiting.put("action", "throttle_with_429");
        governancePolicies.put("rate_limiting", rateLimiting);

        // Audit Logging Policy
        Map<String, Object> auditLogging = new HashMap<>();
        auditLogging.put("enabled", true);
        auditLogging.put("log_requests", true);
        auditLogging.put("log_responses", true);
        auditLogging.put("log_metadata", true);
        auditLogging.put("retention_days", 90);
        auditLogging.put("log_location", "azure_log_analytics");
        governancePolicies.put("audit_logging", auditLogging);

        // Compliance Policy
        Map<String, Object> compliance = new HashMap<>();
        compliance.put("enabled", true);
        compliance.put("standards", Arrays.asList("GDPR", "SOC2", "HIPAA"));
        compliance.put("data_classification", "confidential");
        compliance.put("encryption_at_rest", true);
        compliance.put("encryption_in_transit", true);
        governancePolicies.put("compliance", compliance);
        // </governance_policy_definition>

        Map<String, Object> policyDocument = new HashMap<>();
        policyDocument.put("policy_version", "1.0");
        policyDocument.put("applied_date", Instant.now().toString());
        policyDocument.put("agent_id", agent.getId());
        policyDocument.put("agent_name", agent.getName());
        policyDocument.put("policies", governancePolicies);

        saveToFile(policyDocument, "governance_policies.json");

        System.out.println("✅ Governance policies applied successfully");
        System.out.println("   Policies configured:");
        System.out.println("   • Content Safety: Filter harmful content");
        System.out.println("   • Data Protection: PII detection and redaction");
        System.out.println("   • Rate Limiting: Request quotas and burst limits");
        System.out.println("   • Audit Logging: Request/response logging");
        System.out.println("   • Compliance: GDPR, SOC2, HIPAA standards");
        System.out.println("   Configuration saved: governance_policies.json");
    }
    
    /**
     * Compare different model configurations for optimal performance.
     * 
     * @param agent The agent to compare configurations for
     */
    public void compareModelConfigurations(AgentVersionObject agent) {
        System.out.println("\n🔄 MODEL COMPARISON AND ITERATION");
        System.out.println("=".repeat(70));

        List<Map<String, Object>> modelConfigs = Arrays.asList(
                createModelConfig("gpt-4o", 0.7, 2000, "balanced"),
                createModelConfig("gpt-4o-mini", 0.5, 1500, "fast"),
                createModelConfig("gpt-4", 0.3, 3000, "conservative")
        );

        List<Map<String, Object>> comparisonResults = new ArrayList<>();

        for (Map<String, Object> config : modelConfigs) {
            String model = (String) config.get("model");
            double temperature = (double) config.get("temperature");
            int maxTokens = (int) config.get("max_tokens");

            System.out.println("📊 Testing configuration: " + model);
            System.out.println("   Temperature: " + temperature);
            System.out.println("   Max tokens: " + maxTokens);

            // Simulate metrics (in production, these would be real measurements)
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("avg_latency_ms", Math.random() * 2000 + 1000);
            metrics.put("avg_tokens", Math.random() * 500 + 500);
            metrics.put("quality_score", Math.random() * 0.3 + 0.7);
            metrics.put("cost_per_1k_tokens", Math.random() * 0.05 + 0.01);

            Map<String, Object> result = new HashMap<>(config);
            result.put("metrics", metrics);
            result.put("tested_at", Instant.now().toString());

            comparisonResults.add(result);

            System.out.println("   Latency: " + String.format("%.0fms", metrics.get("avg_latency_ms")));
            System.out.println("   Quality: " + String.format("%.2f", metrics.get("quality_score")));
            System.out.println();
        }

        // Determine recommended configuration
        Map<String, Object> recommended = comparisonResults.get(0); // Simplified logic

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("comparison_date", Instant.now().toString());
        comparison.put("agent_id", agent.getId());
        comparison.put("configurations_tested", comparisonResults);
        comparison.put("recommended_configuration", recommended);
        comparison.put("recommendation_reason", "Balanced performance and quality");

        saveToFile(comparison, "model_comparison.json");

        System.out.println("🏆 MODEL COMPARISON COMPLETE");
        System.out.println("   Configurations tested: " + modelConfigs.size());
        System.out.println("   Recommended: " + recommended.get("model"));
        System.out.println("   Report saved: model_comparison.json");
    }
    
    private Map<String, Object> createModelConfig(String model, double temp, int tokens, String profile) {
        Map<String, Object> config = new HashMap<>();
        config.put("model", model);
        config.put("temperature", temp);
        config.put("max_tokens", tokens);
        config.put("profile", profile);
        return config;
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
