// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.AgentsClientBuilder;
import com.azure.ai.agents.ConversationsClient;
import com.azure.ai.agents.ResponsesClient;
import com.azure.ai.agents.models.*;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles agent creation and Azure client initialization.
 * Returns initialized clients and the created agent.
 */
public class AgentCreation {
    
    private final Dotenv dotenv;
    
    public AgentCreation(Dotenv dotenv) {
        this.dotenv = dotenv;
    }
    
    /**
     * Initialize Azure AI Foundry clients and create the Modern Workplace Assistant.
     * 
     * @return ClientBundle containing all initialized clients and the created agent
     */
    public ClientBundle createProductionReadyAgent() {
        System.out.println("🔐 Initializing Azure AI clients...");
        
        // <authentication_to_azure>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        String aiFpoundryTenantId = dotenv.get("AI_FOUNDRY_TENANT_ID");
        TokenCredential credential;
        if (aiFpoundryTenantId != null && !aiFpoundryTenantId.isEmpty()) {
            System.out.println("   Using AI Foundry tenant: " + aiFpoundryTenantId);
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

        AgentsClient agentsClient = builder.buildClient();
        ResponsesClient responsesClient = builder.buildResponsesClient();
        ConversationsClient conversationsClient = builder.buildConversationsClient();
        // </authentication_to_azure>

        System.out.println("✅ Clients initialized successfully");
        
        // Create the Modern Workplace Assistant
        AgentVersionObject agent = createWorkplaceAssistant(agentsClient);
        
        return new ClientBundle(agentsClient, responsesClient, conversationsClient, agent);
    }
    
    /**
     * Create the Modern Workplace Assistant from Tutorial 1.
     * This agent combines SharePoint and MCP for comprehensive workplace assistance.
     */
    private AgentVersionObject createWorkplaceAssistant(AgentsClient agentsClient) {
        System.out.println("\n🤖 Creating Modern Workplace Assistant (from Tutorial 1)...");

        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL");

        // Setup tools
        List<Object> tools = new ArrayList<>();

        // Add SharePoint tool if configured
        if (sharepointResourceName != null && !sharepointResourceName.isEmpty()) {
            try {
                ToolProjectConnectionList connections = new ToolProjectConnectionList();
                SharepointGroundingToolParameters groundingParams = new SharepointGroundingToolParameters()
                        .setProjectConnections(connections);
                SharepointAgentTool sharepointTool = new SharepointAgentTool()
                        .setSharepointGrounding(groundingParams);
                tools.add(sharepointTool);
                System.out.println("   ✅ SharePoint tool configured");
            } catch (Exception e) {
                System.out.println("   ⚠️  SharePoint tool unavailable: " + e.getMessage());
            }
        }

        // Add MCP tool
        if (mcpServerUrl != null && !mcpServerUrl.isEmpty()) {
            MCPTool mcpTool = new MCPTool()
                    .setServerLabel("microsoft_learn")
                    .setServerUrl(mcpServerUrl)
                    .setAllowedTools(new ArrayList<>());
            tools.add(mcpTool);
            System.out.println("   ✅ MCP tool configured");
        }

        String instructions = """
You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search SharePoint for company-specific requirements
- For technical questions: Use Microsoft Learn for current documentation
- For implementation questions: Combine both sources
- Always cite your sources and provide step-by-step guidance
- Explain how internal requirements connect to technical implementation
""";

        PromptAgentDefinition definition = new PromptAgentDefinition(modelDeploymentName)
                .setInstructions(instructions)
                .setTools(tools);

        AgentVersionObject agent = agentsClient.createAgentVersion(
                "Modern Workplace Assistant",
                definition
        );

        System.out.println("✅ Agent created: " + agent.getId());
        return agent;
    }
    
    /**
     * Bundle containing all initialized Azure AI clients and the created agent.
     */
    public static class ClientBundle {
        private final AgentsClient agentsClient;
        private final ResponsesClient responsesClient;
        private final ConversationsClient conversationsClient;
        private final AgentVersionObject agent;
        
        public ClientBundle(AgentsClient agentsClient, ResponsesClient responsesClient,
                          ConversationsClient conversationsClient, AgentVersionObject agent) {
            this.agentsClient = agentsClient;
            this.responsesClient = responsesClient;
            this.conversationsClient = conversationsClient;
            this.agent = agent;
        }
        
        public AgentsClient getAgentsClient() { return agentsClient; }
        public ResponsesClient getResponsesClient() { return responsesClient; }
        public ConversationsClient getConversationsClient() { return conversationsClient; }
        public AgentVersionObject getAgent() { return agent; }
    }
}
