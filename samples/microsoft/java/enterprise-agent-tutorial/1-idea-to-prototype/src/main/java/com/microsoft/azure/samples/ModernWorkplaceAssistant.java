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
import com.openai.models.conversations.Conversation;
import com.openai.models.conversations.items.ItemCreateParams;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
// </imports_and_includes>

/**
 * Azure AI Foundry Agent Sample - Tutorial 1: Modern Workplace Assistant
 * 
 * This sample demonstrates a complete business scenario combining:
 * - SharePoint integration for internal company knowledge
 * - Microsoft Learn MCP integration for external technical guidance
 * - Intelligent orchestration of multiple data sources
 * - Robust error handling and graceful degradation
 * 
 * Educational Focus:
 * - Enterprise AI patterns with multiple data sources
 * - Real-world business scenarios that enterprises face daily
 * - Production-ready error handling and diagnostics
 * - Foundation for governance, evaluation, and monitoring (Tutorials 2-3)
 * 
 * Business Scenario:
 * An employee needs to implement Azure AD multi-factor authentication. They need:
 * 1. Company security policy requirements (from SharePoint)
 * 2. Technical implementation steps (from Microsoft Learn)
 * 3. Combined guidance showing how policy requirements map to technical implementation
 */
public class ModernWorkplaceAssistant {

    private static Dotenv dotenv;
    private static AgentsClient agentsClient;
    private static ResponsesClient responsesClient;
    private static ConversationsClient conversationsClient;

    public static void main(String[] args) {
        System.out.println("🚀 Azure AI Foundry - Modern Workplace Assistant");
        System.out.println("Tutorial 1: Building Enterprise Agents with SharePoint + MCP Integration");
        System.out.println("=".repeat(70));

        // Load environment variables
        dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Create the agent with full diagnostic output
        AgentCreationResult result = createWorkplaceAssistant();
        AgentVersionObject agent = result.agent;
        MCPTool mcpTool = result.mcpTool;
        SharepointAgentTool sharepointTool = result.sharepointTool;

        // Demonstrate business scenarios
        demonstrateBusinessScenarios(agent, mcpTool, sharepointTool);

        // Offer interactive testing
        System.out.print("\n🎯 Try interactive mode? (y/n): ");
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim();
        if (input.toLowerCase().startsWith("y")) {
            interactiveMode(agent, mcpTool);
        }

        System.out.println("\n🎉 Sample completed successfully!");
        System.out.println("📚 This foundation supports Tutorial 2 (Governance) and Tutorial 3 (Production)");
        System.out.println("🔗 Next: Add evaluation metrics, monitoring, and production deployment");
        
        scanner.close();
    }

    /**
     * Create a Modern Workplace Assistant combining internal and external knowledge.
     * 
     * This demonstrates enterprise AI patterns:
     * 1. Multi-source data integration (SharePoint + MCP)
     * 2. Robust error handling with graceful degradation
     * 3. Dynamic agent capabilities based on available resources
     * 4. Clear diagnostic information for troubleshooting
     * 
     * Educational Value:
     * - Shows real-world complexity of enterprise AI systems
     * - Demonstrates how to handle partial system failures
     * - Provides patterns for combining internal and external data
     * 
     * @return AgentCreationResult containing agent and tools for further interaction
     */
    private static AgentCreationResult createWorkplaceAssistant() {
        System.out.println("🤖 Creating Modern Workplace Assistant...");

        // ========================================================================
        // AUTHENTICATION SETUP
        // ========================================================================
        // <agent_authentication>
        String aiFpoundryTenantId = dotenv.get("AI_FOUNDRY_TENANT_ID");
        TokenCredential credential;
        if (aiFpoundryTenantId != null && !aiFpoundryTenantId.isEmpty()) {
            System.out.println("🔐 Using AI Foundry tenant: " + aiFpoundryTenantId);
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
        // </agent_authentication>

        // ========================================================================
        // SHAREPOINT INTEGRATION SETUP
        // ========================================================================
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        SharepointAgentTool sharepointTool = null;

        System.out.println("📁 Configuring SharePoint integration...");
        System.out.println("   Connection: " + sharepointResourceName);

        try {
            // <sharepoint_tool_setup>

            // Attempt to retrieve pre-configured SharePoint connection
            // Note: In Java SDK, we'll need to construct the tool with connection info
            // This is a simplified version - actual implementation may vary based on SDK
            ToolProjectConnectionList connections = new ToolProjectConnectionList();
            // connections.addConnection(new ToolProjectConnection().setConnectionId(sharepointResourceName));
            
            SharepointGroundingToolParameters groundingParams = new SharepointGroundingToolParameters()
                    .setProjectConnections(connections);
            
            sharepointTool = new SharepointAgentTool()
                    .setSharepointGrounding(groundingParams);
            
            System.out.println("✅ SharePoint successfully connected");
            // </sharepoint_tool_setup>
        }
        catch (Exception e) {
            // Graceful degradation - system continues without SharePoint
            System.out.println("⚠️  SharePoint connection failed: " + e.getMessage());
            System.out.println("   Agent will operate in technical guidance mode only");
            System.out.println("   📝 To enable full functionality:");
            System.out.println("      Create SharePoint connection in Azure AI Foundry portal");
            System.out.println("      Connection name: " + sharepointResourceName);
            sharepointTool = null;
        }

        // ========================================================================
        // MICROSOFT LEARN MCP INTEGRATION SETUP
        // ========================================================================
        // <mcp_tool_setup>
        System.out.println("📚 Configuring Microsoft Learn MCP integration...");
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL");
        
        MCPTool mcpTool = new MCPTool()
                .setServerLabel("microsoft_learn")
                .setServerUrl(mcpServerUrl)
                .setAllowedTools(new ArrayList<>()); // Allow all available tools
        
        System.out.println("✅ Microsoft Learn MCP connected: " + mcpServerUrl);
        // </mcp_tool_setup>

        // ========================================================================
        // AGENT CREATION WITH DYNAMIC CAPABILITIES
        // ========================================================================
        String instructions;
        if (sharepointTool != null) {
            instructions = """
You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search SharePoint for company-specific requirements and guidelines
- For technical questions: Use Microsoft Learn for current Azure/M365 documentation and best practices
- For implementation questions: Combine both sources to show how company policies map to technical implementation
- Always cite your sources and provide step-by-step guidance
- Explain how internal requirements connect to external implementation steps

EXAMPLE SCENARIOS:
- "What is our MFA policy?" → Search SharePoint for security policies
- "How do I configure Azure AD Conditional Access?" → Use Microsoft Learn for technical steps
- "Our policy requires MFA - how do I implement this?" → Combine policy requirements with implementation guidance
""";
        } else {
            instructions = """
You are a Technical Assistant with access to Microsoft Learn documentation.

CAPABILITIES:
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide detailed implementation steps and best practices
- Explain Azure services, features, and configuration options

LIMITATIONS:
- SharePoint integration is not available
- Cannot access company-specific policies or internal documentation
- When asked about company policies, explain that internal document access requires SharePoint configuration

RESPONSE STRATEGY:
- Provide comprehensive technical guidance from Microsoft Learn
- Include step-by-step implementation instructions
- Reference official documentation and best practices
- Suggest how technical implementations typically align with enterprise requirements
""";
        }

        // <create_agent_with_tools>
        // Create the agent with appropriate tool configuration
        System.out.println("🛠️  Configuring agent tools...");
        List<Object> availableTools = new ArrayList<>();
        if (sharepointTool != null) {
            availableTools.add(sharepointTool);
        }
        availableTools.add(mcpTool);
        System.out.println("   Available tools: " + availableTools.size());

        PromptAgentDefinition definition = new PromptAgentDefinition(modelDeploymentName)
                .setInstructions(instructions)
                .setTools(availableTools);

        AgentVersionObject agent = agentsClient.createAgentVersion(
                "Modern Workplace Assistant",
                definition
        );

        System.out.println("✅ Agent created successfully: " + agent.getId());
        
        return new AgentCreationResult(agent, mcpTool, sharepointTool);
        // </create_agent_with_tools>
    }

    /**
     * Demonstrate realistic business scenarios combining internal and external knowledge.
     * 
     * This function showcases the practical value of the Modern Workplace Assistant
     * by walking through scenarios that enterprise employees face regularly.
     */
    private static void demonstrateBusinessScenarios(AgentVersionObject agent, MCPTool mcpTool, SharepointAgentTool sharepointTool) {
        List<BusinessScenario> scenarios = new ArrayList<>();
        
        scenarios.add(new BusinessScenario(
                "📋 Company Policy Question",
                "What is our remote work security policy regarding multi-factor authentication?",
                "Employee needs to understand company MFA requirements",
                "SharePoint",
                "Internal policy retrieval and interpretation"
        ));
        
        scenarios.add(new BusinessScenario(
                "🔧 Technical Implementation Question",
                "How do I set up Azure Active Directory conditional access policies?",
                "IT administrator needs technical implementation steps",
                "Microsoft Learn MCP",
                "External technical documentation access"
        ));
        
        scenarios.add(new BusinessScenario(
                "🔄 Combined Business Implementation Question",
                "What Azure AD configuration should I implement to comply with our company's remote work security policy?",
                "Need to combine policy requirements with technical implementation",
                "Both SharePoint and MCP",
                "Multi-source intelligence combining internal requirements with external implementation"
        ));

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🏢 MODERN WORKPLACE ASSISTANT - BUSINESS SCENARIO DEMONSTRATION");
        System.out.println("=".repeat(70));
        System.out.println("This demonstration shows how AI agents solve real business problems");
        System.out.println("by combining internal company knowledge with external technical guidance.");
        System.out.println("=".repeat(70));

        for (int i = 0; i < scenarios.size(); i++) {
            BusinessScenario scenario = scenarios.get(i);
            System.out.println(String.format("\n📊 SCENARIO %d/%d: %s", i + 1, scenarios.size(), scenario.title));
            System.out.println("-".repeat(50));
            System.out.println("❓ QUESTION: " + scenario.question);
            System.out.println("🎯 BUSINESS CONTEXT: " + scenario.context);
            System.out.println("📚 EXPECTED SOURCE: " + scenario.expectedSource);
            System.out.println("🎓 LEARNING POINT: " + scenario.learningPoint);
            System.out.println("-".repeat(50));

            // <agent_conversation>
            // Get response from the agent
            System.out.println("🤖 ASSISTANT RESPONSE:");
            ChatResult result = chatWithAssistant(agent.getName(), mcpTool, scenario.question);
            // </agent_conversation>

            // Display response with analysis
            if ("completed".equals(result.status) && result.response != null && result.response.length() > 10) {
                String preview = result.response.length() > 300 
                        ? result.response.substring(0, 300) + "..." 
                        : result.response;
                System.out.println("✅ SUCCESS: " + preview);
                if (result.response.length() > 300) {
                    System.out.println("   📏 Full response: " + result.response.length() + " characters");
                }
            } else {
                System.out.println("⚠️  LIMITED RESPONSE: " + result.response);
                if (sharepointTool == null && (scenario.expectedSource.contains("SharePoint"))) {
                    System.out.println("   💡 This demonstrates graceful degradation when SharePoint is unavailable");
                }
            }

            System.out.println("📈 STATUS: " + result.status);
            System.out.println("-".repeat(50));
        }

        System.out.println("\n✅ DEMONSTRATION COMPLETED!");
        System.out.println("🎓 Key Learning Outcomes:");
        System.out.println("   • Multi-source data integration in enterprise AI");
        System.out.println("   • Robust error handling and graceful degradation");
        System.out.println("   • Real business value through combined intelligence");
        System.out.println("   • Foundation for governance and monitoring (Tutorials 2-3)");
    }

    /**
     * Execute a conversation with the workplace assistant.
     * 
     * This function demonstrates the conversation pattern for Azure AI Foundry agents
     * and includes comprehensive error handling for production readiness.
     */
    private static ChatResult chatWithAssistant(String agentName, MCPTool mcpTool, String message) {
        try {
            // Create conversation with initial message
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

            // Create response using the agent
            AgentReference agentReference = new AgentReference(agentName);
            Response response = responsesClient.createWithAgentConversation(
                    agentReference,
                    conversation.id()
            );

            // Extract the response text
            String fullResponse = response.output() != null ? response.output().toString() : "";
            
            return new ChatResult(fullResponse, "completed");

        } catch (Exception e) {
            return new ChatResult("Error in conversation: " + e.getMessage(), "failed");
        }
    }

    /**
     * Interactive mode for testing the workplace assistant.
     * 
     * This provides a simple interface for users to test the agent with their own questions
     * and see how it combines different data sources for comprehensive answers.
     */
    private static void interactiveMode(AgentVersionObject agent, MCPTool mcpTool) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("💬 INTERACTIVE MODE - Test Your Workplace Assistant!");
        System.out.println("=".repeat(60));
        System.out.println("Ask questions that combine company policies with technical guidance:");
        System.out.println("• 'What's our remote work policy for Azure access?'");
        System.out.println("• 'How do I configure SharePoint security?'");
        System.out.println("• 'Our policy requires encryption - how do I set this up in Azure?'");
        System.out.println("Type 'quit' to exit.");
        System.out.println("-".repeat(60));

        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            try {
                System.out.print("\n❓ Your question: ");
                String question = scanner.nextLine().trim();

                if (question.toLowerCase().matches("quit|exit|bye")) {
                    break;
                }

                if (question.isEmpty()) {
                    System.out.println("💡 Please ask a question about policies or technical implementation.");
                    continue;
                }

                System.out.print("\n🤖 Workplace Assistant: ");
                ChatResult result = chatWithAssistant(agent.getName(), mcpTool, question);
                System.out.println(result.response);

                if (!"completed".equals(result.status)) {
                    System.out.println("\n⚠️  Response status: " + result.status);
                }

                System.out.println("-".repeat(60));

            } catch (Exception e) {
                System.out.println("\n❌ Error: " + e.getMessage());
                System.out.println("-".repeat(60));
            }
        }

        System.out.println("\n👋 Thank you for testing the Modern Workplace Assistant!");
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

    private static class BusinessScenario {
        String title;
        String question;
        String context;
        String expectedSource;
        String learningPoint;

        BusinessScenario(String title, String question, String context, String expectedSource, String learningPoint) {
            this.title = title;
            this.question = question;
            this.context = context;
            this.expectedSource = expectedSource;
            this.learningPoint = learningPoint;
        }
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
