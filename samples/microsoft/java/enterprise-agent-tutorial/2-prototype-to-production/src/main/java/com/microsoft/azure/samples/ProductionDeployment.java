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
 * Handles production endpoint configuration and deployment.
 * Generates configuration for Azure Container Apps, Marketplace, and Logic Apps.
 */
public class ProductionDeployment {
    
    private final Gson gson;
    
    public ProductionDeployment(Gson gson) {
        this.gson = gson;
    }
    
    /**
     * Generate production endpoint configuration for the agent.
     * 
     * @param agent The agent to configure for production
     */
    public void generateProductionEndpointConfig(AgentVersionObject agent) {
        System.out.println("\n🚀 PRODUCTION ENDPOINT DEPLOYMENT");
        System.out.println("=".repeat(70));

        // <endpoint_configuration>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        Map<String, Object> endpointConfig = new HashMap<>();

        // Container App Configuration
        Map<String, Object> containerApp = new HashMap<>();
        containerApp.put("name", "workplace-assistant");
        containerApp.put("resource_group", "agents-production-rg");
        containerApp.put("environment", "agents-prod-env");
        containerApp.put("container_image", "your-registry.azurecr.io/workplace-assistant:latest");
        containerApp.put("ingress_type", "external");
        containerApp.put("target_port", 8000);
        containerApp.put("min_replicas", 2);
        containerApp.put("max_replicas", 10);

        // Marketplace Configuration
        Map<String, Object> marketplace = new HashMap<>();
        marketplace.put("offer_id", "workplace-assistant");
        marketplace.put("plan_id", "enterprise");
        marketplace.put("publisher_id", "contoso");
        marketplace.put("categories", Arrays.asList("AI + Machine Learning", "Productivity"));
        marketplace.put("pricing_model", "per_user_per_month");

        // Logic Apps Connector Configuration
        Map<String, Object> logicAppsConnector = new HashMap<>();
        logicAppsConnector.put("connector_name", "WorkplaceAssistant");
        logicAppsConnector.put("display_name", "Workplace Assistant Connector");
        logicAppsConnector.put("description", "Connect to your workplace assistant for automated workflows");
        logicAppsConnector.put("icon_url", "https://your-cdn.com/icon.png");

        // Managed Identity Configuration
        Map<String, Object> managedIdentity = new HashMap<>();
        managedIdentity.put("type", "SystemAssigned");
        managedIdentity.put("role_assignments", Arrays.asList(
                createRoleAssignment("Cognitive Services User", "AI Foundry Project"),
                createRoleAssignment("Storage Blob Data Reader", "Storage Account")
        ));

        // API Management Configuration
        Map<String, Object> apim = new HashMap<>();
        apim.put("api_name", "workplace-assistant-api");
        apim.put("api_path", "workplace-assistant");
        apim.put("rate_limit", 100);
        apim.put("quota_limit", 10000);
        apim.put("cors_enabled", true);
        apim.put("cors_origins", Arrays.asList("https://your-app.com"));
        // </endpoint_configuration>

        endpointConfig.put("configuration_date", Instant.now().toString());
        endpointConfig.put("agent_id", agent.getId());
        endpointConfig.put("agent_name", agent.getName());
        endpointConfig.put("container_app", containerApp);
        endpointConfig.put("marketplace", marketplace);
        endpointConfig.put("logic_apps_connector", logicAppsConnector);
        endpointConfig.put("managed_identity", managedIdentity);
        endpointConfig.put("api_management", apim);

        saveToFile(endpointConfig, "endpoint_config.json");

        // Generate Logic Apps definition
        Map<String, Object> logicAppDef = new HashMap<>();
        logicAppDef.put("$schema", "https://schema.management.azure.com/providers/Microsoft.Logic/schemas/2016-06-01/workflowdefinition.json#");
        logicAppDef.put("contentVersion", "1.0.0.0");
        
        Map<String, Object> parameters = new HashMap<>();
        Map<String, Object> triggers = new HashMap<>();
        triggers.put("manual", createManualTrigger());
        
        Map<String, Object> actions = new HashMap<>();
        actions.put("Call_Workplace_Assistant", createHttpAction(
                "POST",
                "https://workplace-assistant.azurecontainerapps.io/api/chat",
                "@triggerBody()"
        ));
        
        Map<String, Object> definition = new HashMap<>();
        definition.put("triggers", triggers);
        definition.put("actions", actions);
        
        logicAppDef.put("parameters", parameters);
        logicAppDef.put("definition", definition);

        saveToFile(logicAppDef, "logic_app_definition.json");

        System.out.println("✅ Production endpoint configured successfully");
        System.out.println("   Deployment: Azure Container Apps");
        System.out.println("   Authentication: Managed Identity");
        System.out.println("   API Management: Rate limiting enabled");
        System.out.println("   Configuration saved: endpoint_config.json");
        System.out.println("   Logic Apps definition: logic_app_definition.json");
    }
    
    private Map<String, Object> createRoleAssignment(String role, String scope) {
        Map<String, Object> assignment = new HashMap<>();
        assignment.put("role", role);
        assignment.put("scope", scope);
        return assignment;
    }
    
    private Map<String, Object> createManualTrigger() {
        Map<String, Object> trigger = new HashMap<>();
        trigger.put("type", "Request");
        trigger.put("kind", "Http");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("method", "POST");
        trigger.put("inputs", inputs);
        return trigger;
    }
    
    private Map<String, Object> createHttpAction(String method, String uri, String body) {
        Map<String, Object> action = new HashMap<>();
        action.put("type", "Http");
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("method", method);
        inputs.put("uri", uri);
        inputs.put("body", body);
        action.put("inputs", inputs);
        return action;
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
