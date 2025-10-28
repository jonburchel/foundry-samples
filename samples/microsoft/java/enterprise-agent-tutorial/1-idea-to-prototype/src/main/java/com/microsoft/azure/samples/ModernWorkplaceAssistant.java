// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

// <imports_and_includes>
import com.azure.ai.agents.AgentsClient;
import com.azure.ai.agents.AgentsClientBuilder;
import com.azure.ai.agents.models.*;
import com.azure.ai.projects.AIProjectClient;
import com.azure.ai.projects.AIProjectClientBuilder;
import com.azure.ai.projects.models.Connection;
import com.azure.core.credential.TokenCredential;
import com.azure.core.util.polling.SyncPoller;
import com.azure.identity.AzureCliCredential;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
// </imports_and_includes>

/**
 * Azure AI Foundry Agent Sample - Tutorial 1: Modern Workplace Assistant
 * 
 * This sample demonstrates a complete business scenario using Azure AI Agents SDK v2:
 * - Agent creation with the new SDK
 * - Thread and message management
 * - Robust error handling and graceful degradation
 * 
 * Educational Focus:
 * - Enterprise AI patterns with Agent SDK v2
 * - Real-world business scenarios that enterprises face daily
 * - Production-ready error handling and diagnostics
 * - Foundation for governance, evaluation, and monitoring (Tutorials 2-3)
 * 
 * Business Scenario:
 * An employee needs to implement Azure AD multi-factor authentication. They need:
 * 1. Company security policy requirements
 * 2. Technical implementation steps
 * 3. Combined guidance showing how policy requirements map to technical implementation
 */
public class ModernWorkplaceAssistant {

    private static Dotenv dotenv;
    private static AgentsClient agentsClient;

    public static void main(String[] args) {
        System.out.println("🚀 Azure AI Foundry - Modern Workplace Assistant");
        System.out.println("Tutorial 1: Building Enterprise Agents with Agent SDK v2");
        System.out.println("=".repeat(70));

        try {
            // Load environment variables
            dotenv = Dotenv.configure()
                .directory("../")
                .ignoreIfMissing()
                .load();

            // Create the agent with full diagnostic output
            Agent agent = createWorkplaceAssistant();

            // Demonstrate business scenarios
            demonstrateBusinessScenarios(agent);

            // Offer interactive testing
            System.out.print("\n🎯 Try interactive mode? (y/n): ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();
            if (input.toLowerCase().startsWith("y")) {
                interactiveMode(agent);
            }

            System.out.println("\n🎉 Sample completed successfully!");
            System.out.println("📚 This foundation supports Tutorial 2 (Governance) and Tutorial 3 (Production)");
            System.out.println("🔗 Next: Add evaluation metrics, monitoring, and production deployment");
            
            scanner.close();

        } catch (Exception e) {
            System.out.println("\n❌ Error: " + e.getMessage());
            System.out.println("Please check your .env configuration and ensure:");
            System.out.println("  - PROJECT_ENDPOINT is correct");
            System.out.println("  - MODEL_DEPLOYMENT_NAME is deployed");
            System.out.println("  - Azure credentials are configured (az login)");
            e.printStackTrace();
        }
    }

    /**
     * Create a Modern Workplace Assistant using Agent SDK v2.
     * 
     * This demonstrates enterprise AI patterns:
     * 1. Agent creation with the new SDK
     * 2. Robust error handling with graceful degradation
     * 3. Dynamic agent capabilities based on available resources
     * 4. Clear diagnostic information for troubleshooting
     * 
     * Educational Value:
     * - Shows real-world complexity of enterprise AI systems
     * - Demonstrates how to handle partial system failures
     * - Provides patterns for agent creation with Agent SDK v2
     * 
     * @return Agent object for further interaction
     */
    private static Agent createWorkplaceAssistant() {
        System.out.println("\n🤖 Creating Modern Workplace Assistant...");

        // ========================================================================
        // AUTHENTICATION SETUP
        // ========================================================================
        // <agent_authentication>
        String endpoint = dotenv.get("PROJECT_ENDPOINT");
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        
        // Support default Azure credentials
        TokenCredential credential = new DefaultAzureCredentialBuilder().build();

        agentsClient = new AgentsClientBuilder()
                .endpoint(endpoint)
                .credential(credential)
                .buildClient();
        
        System.out.println("✅ Connected to Azure AI Foundry: " + endpoint);
        // </agent_authentication>

        // ========================================================================
        // SHAREPOINT INTEGRATION SETUP
        // ========================================================================
        // <sharepoint_connection_resolution>
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        SharepointToolDefinition sharepointTool = null;

        if (sharepointResourceName != null && !sharepointResourceName.isEmpty()) {
            System.out.println("📁 Configuring SharePoint integration...");
            System.out.println("   Connection name: " + sharepointResourceName);

            try {
                // Resolve the connection name to its full ARM resource ID
                System.out.println("   🔍 Resolving connection name to ARM resource ID...");
                
                AIProjectClient projectClient = new AIProjectClientBuilder()
                        .endpoint(endpoint)
                        .credential(credential)
                        .buildClient();
                
                // List all connections and find the one we need
                String connectionId = null;
                for (Connection conn : projectClient.getConnections().list()) {
                    if (sharepointResourceName.equals(conn.getName())) {
                        connectionId = conn.getId();
                        System.out.println("   ✅ Resolved to: " + connectionId);
                        break;
                    }
                }

                if (connectionId == null) {
                    throw new RuntimeException("Connection '" + sharepointResourceName + "' not found in project");
                }

                // Create SharePoint tool with the full ARM resource ID
                sharepointTool = new SharepointToolDefinition(connectionId);
                System.out.println("✅ SharePoint tool configured successfully");

            } catch (Exception e) {
                System.out.println("⚠️  SharePoint connection unavailable: " + e.getMessage());
                System.out.println("   Possible causes:");
                System.out.println("   - Connection '" + sharepointResourceName + "' doesn't exist in the project");
                System.out.println("   - Insufficient permissions to access the connection");
                System.out.println("   - Connection configuration is incomplete");
                System.out.println("   Agent will operate without SharePoint access");
                sharepointTool = null;
            }
        } else {
            System.out.println("📁 SharePoint integration skipped (SHAREPOINT_RESOURCE_NAME not set)");
        }
        // </sharepoint_connection_resolution>

        // ========================================================================
        // MICROSOFT LEARN MCP INTEGRATION SETUP
        // ========================================================================
        // <mcp_tool_setup>
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL");
        McpToolDefinition mcpTool = null;

        if (mcpServerUrl != null && !mcpServerUrl.isEmpty()) {
            System.out.println("📚 Configuring Microsoft Learn MCP integration...");
            System.out.println("   Server URL: " + mcpServerUrl);

            try {
                // Create MCP tool for Microsoft Learn documentation access
                // server_label must match pattern: ^[a-zA-Z0-9_]+$ (alphanumeric and underscores only)
                mcpTool = new McpToolDefinition(
                        mcpServerUrl,
                        "Microsoft_Learn_Documentation"
                );
                System.out.println("✅ MCP tool configured successfully");
            } catch (Exception e) {
                System.out.println("⚠️  MCP tool unavailable: " + e.getMessage());
                System.out.println("   Agent will operate without Microsoft Learn access");
                mcpTool = null;
            }
        } else {
            System.out.println("📚 MCP integration skipped (MCP_SERVER_URL not set)");
        }
        // </mcp_tool_setup>

        // ========================================================================
        // AGENT CREATION WITH DYNAMIC CAPABILITIES
        // ========================================================================
        String instructions;
        if (sharepointTool != null && mcpTool != null) {
            instructions = "You are a Modern Workplace Assistant for Contoso Corporation.\n\n" +
                    "CAPABILITIES:\n" +
                    "- Search SharePoint for company policies, procedures, and internal documentation\n" +
                    "- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance\n" +
                    "- Provide comprehensive solutions combining internal requirements with external implementation\n\n" +
                    "RESPONSE STRATEGY:\n" +
                    "- For policy questions: Search SharePoint for company-specific requirements and guidelines\n" +
                    "- For technical questions: Use Microsoft Learn for current Azure/M365 documentation and best practices\n" +
                    "- For implementation questions: Combine both sources to show how company policies map to technical implementation\n" +
                    "- Always cite your sources and provide step-by-step guidance\n" +
                    "- Explain how internal requirements connect to external implementation steps\n\n" +
                    "EXAMPLE SCENARIOS:\n" +
                    "- \"What is our MFA policy?\" → Search SharePoint for security policies\n" +
                    "- \"How do I configure Azure AD Conditional Access?\" → Use Microsoft Learn for technical steps\n" +
                    "- \"Our policy requires MFA - how do I implement this?\" → Combine policy requirements with implementation guidance";
        } else if (sharepointTool != null) {
            instructions = "You are a Modern Workplace Assistant with access to Contoso Corporation's SharePoint.\n\n" +
                    "CAPABILITIES:\n" +
                    "- Search SharePoint for company policies, procedures, and internal documentation\n" +
                    "- Provide detailed technical guidance based on your knowledge\n" +
                    "- Combine company policies with general best practices\n\n" +
                    "RESPONSE STRATEGY:\n" +
                    "- Search SharePoint for company-specific requirements\n" +
                    "- Provide technical guidance based on Azure and M365 best practices\n" +
                    "- Explain how to align implementations with company policies";
        } else if (mcpTool != null) {
            instructions = "You are a Technical Assistant with access to Microsoft Learn documentation.\n\n" +
                    "CAPABILITIES:\n" +
                    "- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance\n" +
                    "- Provide detailed implementation steps and best practices\n" +
                    "- Explain Azure services, features, and configuration options\n\n" +
                    "RESPONSE STRATEGY:\n" +
                    "- Use Microsoft Learn for technical documentation\n" +
                    "- Provide comprehensive implementation guidance\n" +
                    "- Reference official documentation and best practices";
        } else {
            instructions = "You are a Technical Assistant specializing in Azure and Microsoft 365 guidance.\n\n" +
                    "CAPABILITIES:\n" +
                    "- Provide detailed Azure and Microsoft 365 technical guidance\n" +
                    "- Explain implementation steps and best practices\n" +
                    "- Help with Azure AD, Conditional Access, MFA, and security configurations\n\n" +
                    "RESPONSE STRATEGY:\n" +
                    "- Provide comprehensive technical guidance\n" +
                    "- Include step-by-step implementation instructions\n" +
                    "- Reference best practices and security considerations";
        }

        // <create_agent_with_tools>
        System.out.println("🛠️  Creating agent with model: " + modelDeploymentName);

        // Build tools list
        List<ToolDefinition> tools = new ArrayList<>();
        
        if (sharepointTool != null) {
            tools.add(sharepointTool);
            System.out.println("   ✓ SharePoint tool added");
        }
        
        if (mcpTool != null) {
            tools.add(mcpTool);
            System.out.println("   ✓ MCP tool added");
        }
        
        System.out.println("   Total tools: " + tools.size());

        // Create agent with or without tools
        Agent agent;
        if (!tools.isEmpty()) {
            agent = agentsClient.createAgent(
                    modelDeploymentName,
                    new CreateAgentOptions()
                            .setName("Modern Workplace Assistant")
                            .setInstructions(instructions)
                            .setTools(tools)
            );
        } else {
            agent = agentsClient.createAgent(
                    modelDeploymentName,
                    new CreateAgentOptions()
                            .setName("Modern Workplace Assistant")
                            .setInstructions(instructions)
            );
        }

        System.out.println("✅ Agent created successfully: " + agent.getId());
        return agent;
        // </create_agent_with_tools>
    }

    /**
     * Demonstrate realistic business scenarios with Agent SDK v2.
     * 
     * This function showcases the practical value of the Modern Workplace Assistant
     * by walking through scenarios that enterprise employees face regularly.
     * 
     * Educational Value:
     * - Shows real business problems that AI agents can solve
     * - Demonstrates proper thread and message management
     * - Illustrates Agent SDK v2 conversation patterns
     */
    private static void demonstrateBusinessScenarios(Agent agent) {
        List<BusinessScenario> scenarios = List.of(
                new BusinessScenario(
                        "📋 Company Policy Question (SharePoint Only)",
                        "What is Contoso's remote work policy?",
                        "Employee needs to understand company-specific remote work requirements",
                        "SharePoint tool retrieves internal company policies"
                ),
                new BusinessScenario(
                        "📚 Technical Documentation Question (MCP Only)",
                        "According to Microsoft Learn, what is the correct way to implement Azure AD Conditional Access policies? Please include reference links to the official documentation.",
                        "IT administrator needs authoritative Microsoft technical guidance",
                        "MCP tool accesses Microsoft Learn for official documentation with links"
                ),
                new BusinessScenario(
                        "🔄 Combined Implementation Question (SharePoint + MCP)",
                        "Based on our company's remote work security policy, how should I configure my Azure environment to comply? Please include links to Microsoft documentation showing how to implement each requirement.",
                        "Need to map company policy to technical implementation with official guidance",
                        "Both tools work together: SharePoint for policy + MCP for implementation docs"
                )
        );

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🏢 MODERN WORKPLACE ASSISTANT - BUSINESS SCENARIO DEMONSTRATION");
        System.out.println("=".repeat(70));
        System.out.println("This demonstration shows how AI agents solve real business problems");
        System.out.println("using the Azure AI Agents SDK v2.");
        System.out.println("=".repeat(70));

        for (int i = 0; i < scenarios.size(); i++) {
            BusinessScenario scenario = scenarios.get(i);
            System.out.println(String.format("\n📊 SCENARIO %d/%d: %s", i + 1, scenarios.size(), scenario.title));
            System.out.println("-".repeat(50));
            System.out.println("❓ QUESTION: " + scenario.question);
            System.out.println("🎯 BUSINESS CONTEXT: " + scenario.context);
            System.out.println("🎓 LEARNING POINT: " + scenario.learningPoint);
            System.out.println("-".repeat(50));

            // <agent_conversation>
            System.out.println("🤖 ASSISTANT RESPONSE:");
            ChatResult result = chatWithAssistant(agent.getId(), scenario.question);
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
                System.out.println("⚠️  RESPONSE: " + result.response);
            }

            System.out.println("📈 STATUS: " + result.status);
            System.out.println("-".repeat(50));

            // Small delay between scenarios
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n✅ DEMONSTRATION COMPLETED!");
        System.out.println("🎓 Key Learning Outcomes:");
        System.out.println("   • Agent SDK v2 usage for enterprise AI");
        System.out.println("   • Proper thread and message management");
        System.out.println("   • Real business value through AI assistance");
        System.out.println("   • Foundation for governance and monitoring (Tutorials 2-3)");
    }

    /**
     * Execute a conversation with the workplace assistant using Agent SDK v2.
     * 
     * This function demonstrates the conversation pattern for Azure AI Agents SDK v2
     * including MCP tool approval handling.
     * 
     * Educational Value:
     * - Shows proper conversation management with Agent SDK v2
     * - Demonstrates thread creation and message handling
     * - Illustrates MCP approval with auto-approval pattern
     * - Includes timeout and error management patterns
     * 
     * @param agentId The ID of the agent to chat with
     * @param message The user's message
     * @return ChatResult containing response text and status
     */
    private static ChatResult chatWithAssistant(String agentId, String message) {
        try {
            // Create a thread for the conversation
            AgentThread thread = agentsClient.createThread(new CreateAgentThreadOptions());

            // Create a message in the thread
            ThreadMessage messageObj = agentsClient.createMessage(
                    thread.getId(),
                    new CreateMessageOptions(MessageRole.USER, message)
            );

            // <mcp_approval_usage>
            // Create and process run with auto-approval for MCP tools
            // This is the recommended pattern for MCP tools in Agent SDK v2
            SyncPoller<ThreadRun, ThreadRun> poller = agentsClient.beginCreateAndProcessRun(
                    thread.getId(),
                    agentId,
                    new CreateAndProcessRunOptions()
                            .setAutoApproveMcpTools(true) // Auto-approve MCP tool calls
            );
            
            ThreadRun run = poller.getFinalResult();
            // </mcp_approval_usage>

            // Retrieve messages
            if (run.getStatus() == RunStatus.COMPLETED) {
                List<ThreadMessage> messages = agentsClient.listMessages(thread.getId())
                        .stream()
                        .collect(Collectors.toList());

                // Get the assistant's response (last message from assistant)
                for (int i = messages.size() - 1; i >= 0; i--) {
                    ThreadMessage msg = messages.get(i);
                    if (msg.getRole() == MessageRole.ASSISTANT && !msg.getContentItems().isEmpty()) {
                        MessageTextContent textContent = (MessageTextContent) msg.getContentItems().get(0);
                        return new ChatResult(textContent.getText().getValue(), "completed");
                    }
                }

                return new ChatResult("No response from assistant", "completed");
            } else {
                return new ChatResult("Run ended with status: " + run.getStatus(), run.getStatus().toString());
            }

        } catch (Exception e) {
            return new ChatResult("Error in conversation: " + e.getMessage(), "failed");
        }
    }

    /**
     * Interactive mode for testing the workplace assistant.
     * 
     * This provides a simple interface for users to test the agent with their own questions
     * and see how it provides comprehensive technical guidance.
     */
    private static void interactiveMode(Agent agent) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("💬 INTERACTIVE MODE - Test Your Workplace Assistant!");
        System.out.println("=".repeat(60));
        System.out.println("Ask questions about Azure, M365, security, and technical implementation:");
        System.out.println("• 'How do I configure Azure AD conditional access?'");
        System.out.println("• 'What are MFA best practices for remote workers?'");
        System.out.println("• 'How do I set up secure SharePoint access?'");
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
                    System.out.println("💡 Please ask a question about Azure or M365 technical implementation.");
                    continue;
                }

                System.out.print("\n🤖 Workplace Assistant: ");
                ChatResult result = chatWithAssistant(agent.getId(), question);
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
    private static class BusinessScenario {
        String title;
        String question;
        String context;
        String learningPoint;

        BusinessScenario(String title, String question, String context, String learningPoint) {
            this.title = title;
            this.question = question;
            this.context = context;
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
