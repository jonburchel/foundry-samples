// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.ConversationsClient;
import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.AgentVersionObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;

/**
 * Azure AI Foundry - Tutorial 2: Prototype to Production
 * 
 * This tutorial demonstrates how to take an agent from prototype to production-ready
 * deployment with enterprise-grade safety, governance, monitoring, and CI/CD integration.
 * 
 * Production Readiness Features:
 * 1. Safety Assessment & Red-Teaming: Test against adversarial inputs
 * 2. Evaluation Datasets: Establish quality baselines with ground truth data
 * 3. Governance Policies: Apply org-wide policies consistently
 * 4. Model Comparison: A/B test different configurations
 * 5. Fleet Monitoring: Enterprise observability and alerting
 * 6. CI/CD Integration: Automated quality gates in deployment pipelines
 * 7. Production Endpoints: Deploy for external consumption
 * 
 * This implementation uses a modular architecture with separate classes for each
 * production readiness feature, making the code more maintainable and testable.
 */
public class PrototypeToProduction {

    public static void main(String[] args) {
        System.out.println("🚀 Azure AI Foundry - Prototype to Production");
        System.out.println("Tutorial 2: Enterprise Agent Deployment");
        System.out.println("=".repeat(70));

        // Load environment variables
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        // Initialize Gson for JSON serialization
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try {
            // Step 1: Initialize Azure AI clients and create agent
            AgentCreation agentCreation = new AgentCreation(dotenv);
            AgentCreation.ClientBundle clientBundle = agentCreation.createProductionReadyAgent();
            
            AgentsClient agentsClient = clientBundle.getAgentsClient();
            ResponsesClient responsesClient = clientBundle.getResponsesClient();
            ConversationsClient conversationsClient = clientBundle.getConversationsClient();
            AgentVersionObject agent = clientBundle.getAgent();

            // Execute production readiness workflow
            System.out.println("\n🎯 PRODUCTION READINESS WORKFLOW");
            System.out.println("=".repeat(70));

            // Step 2: Safety Assessment
            SafetyAssessment safetyAssessment = new SafetyAssessment(responsesClient, conversationsClient, gson);
            Map<String, Object> safetyResults = safetyAssessment.assessAgentSafety(agent);

            // Step 3: Create Evaluation Dataset and Run Baseline Evaluation
            EvaluationFramework evaluationFramework = new EvaluationFramework(responsesClient, conversationsClient, gson);
            evaluationFramework.createEvaluationDataset();
            Map<String, Object> evalResults = evaluationFramework.runBaselineEvaluation(agent);

            // Step 4: Apply Governance Policies and Compare Models
            GovernancePolicies governancePolicies = new GovernancePolicies(gson);
            governancePolicies.applyGovernancePolicies(agent);
            governancePolicies.compareModelConfigurations(agent);

            // Step 5: Setup Fleet Monitoring and CI/CD
            MonitoringCICD monitoringCICD = new MonitoringCICD(gson);
            monitoringCICD.setupFleetMonitoring();
            monitoringCICD.createCiCdEvaluationConfig();

            // Step 6: Generate Production Endpoint Configuration
            ProductionDeployment productionDeployment = new ProductionDeployment(gson);
            productionDeployment.generateProductionEndpointConfig(agent);

            // Print completion summary
            Helpers.printCompletionSummary();

        } catch (Exception e) {
            System.err.println("❌ Error in production workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
