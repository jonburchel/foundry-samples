using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Azure;
using Azure.AI.Agents.Persistent;
using Azure.Core;
using Azure.Identity;
using DotNetEnv;

/*
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

class Program
{
    private static PersistentAgentsClient? client;

    static async Task Main(string[] args)
    {
        Console.WriteLine("🚀 Azure AI Foundry - Modern Workplace Assistant");
        Console.WriteLine("Tutorial 1: Building Enterprise Agents with SharePoint + MCP Integration");
        Console.WriteLine("=".PadRight(70, '='));

        // Create the agent with full diagnostic output
        var (agentId, hasSharePoint) = await CreateWorkplaceAssistantAsync();

        // Demonstrate business scenarios
        await DemonstrateBusinessScenariosAsync(agentId, hasSharePoint);

        // Offer interactive testing
        Console.Write("\n🎯 Try interactive mode? (y/n): ");
        var response = Console.ReadLine();
        if (response?.ToLower().StartsWith("y") == true)
        {
            await InteractiveModeAsync(agentId);
        }

        Console.WriteLine("\n🎉 Sample completed successfully!");
        Console.WriteLine("📚 This foundation supports Tutorial 2 (Governance) and Tutorial 3 (Production)");
        Console.WriteLine("🔗 Next: Add evaluation metrics, monitoring, and production deployment");

        // Cleanup
        await client!.Administration.DeleteAgentAsync(agentId);
    }

    /// <summary>
    /// Create a Modern Workplace Assistant combining internal and external knowledge.
    /// 
    /// This demonstrates enterprise AI patterns:
    /// 1. Multi-source data integration (SharePoint + MCP)
    /// 2. Robust error handling with graceful degradation
    /// 3. Dynamic agent capabilities based on available resources
    /// 4. Clear diagnostic information for troubleshooting
    /// 
    /// Educational Value:
    /// - Shows real-world complexity of enterprise AI systems
    /// - Demonstrates how to handle partial system failures
    /// - Provides patterns for combining internal and external data
    /// </summary>
    private static async Task<(string agentId, bool hasSharePoint)> CreateWorkplaceAssistantAsync()
    {
        // Load environment variables from shared directory
        Env.Load("../shared/.env");

        var projectEndpoint = Environment.GetEnvironmentVariable("PROJECT_ENDPOINT");
        var modelDeploymentName = Environment.GetEnvironmentVariable("MODEL_DEPLOYMENT_NAME");
        var sharepointConnectionId = Environment.GetEnvironmentVariable("SHAREPOINT_CONNECTION_ID");
        var mcpServerUrl = Environment.GetEnvironmentVariable("MCP_SERVER_URL") ?? "https://learn.microsoft.com/api/mcp";
        var tenantId = Environment.GetEnvironmentVariable("AI_FOUNDRY_TENANT_ID");

        Console.WriteLine("\n🤖 Creating Modern Workplace Assistant...");

        // ============================================================================
        // AUTHENTICATION SETUP
        // ============================================================================
        // Support both default Azure credentials and specific tenant authentication
        TokenCredential credential;
        if (!string.IsNullOrEmpty(tenantId))
        {
            Console.WriteLine($"🔐 Using AI Foundry tenant: {tenantId}");
            credential = new AzureCliCredential(new AzureCliCredentialOptions { TenantId = tenantId });
        }
        else
        {
            credential = new DefaultAzureCredential();
        }

        client = new PersistentAgentsClient(projectEndpoint!, credential);

        // ========================================================================
        // SHAREPOINT INTEGRATION SETUP
        // ========================================================================
        // SharePoint provides access to internal company knowledge:
        // - Company policies and procedures
        // - Security guidelines and requirements
        // - Governance and compliance documentation
        // - Internal process documentation

        bool hasSharePoint = false;
        List<ToolDefinition> tools = new();

        Console.WriteLine("📁 Configuring SharePoint integration...");

        if (!string.IsNullOrEmpty(sharepointConnectionId))
        {
            try
            {
                // The SharePoint tool requires the full Azure resource ID for the connection
                // This ID is retrieved from the SHAREPOINT_CONNECTION_ID environment variable
                // Format: /subscriptions/{sub}/resourceGroups/{rg}/providers/Microsoft.CognitiveServices/accounts/{account}/projects/{project}/connections/{name}
                
                SharepointToolDefinition sharepointTool = new(
                    new SharepointGroundingToolParameters(sharepointConnectionId)
                );
                
                tools.Add(sharepointTool);
                hasSharePoint = true;
                Console.WriteLine($"✅ SharePoint successfully connected");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"⚠️  SharePoint connection failed: {ex.Message}");
                Console.WriteLine("   Agent will operate in technical guidance mode only");
                Console.WriteLine("   📝 To enable full functionality:");
                Console.WriteLine("      1. Create SharePoint connection in Azure AI Foundry portal");
                Console.WriteLine("      2. Set SHAREPOINT_CONNECTION_ID in .env file");
            }
        }

        // ========================================================================
        // MICROSOFT LEARN MCP INTEGRATION SETUP
        // ========================================================================
        // Microsoft Learn MCP provides access to current technical documentation:
        // - Azure service configuration guides
        // - Best practices and implementation patterns
        // - Troubleshooting and diagnostic information
        // - Latest feature updates and capabilities

        Console.WriteLine("📚 Configuring Microsoft Learn MCP integration...");
        
        // Create MCP tool definition
        MCPToolDefinition mcpTool = new("microsoft_learn", mcpServerUrl);
        tools.Add(mcpTool);
        
        Console.WriteLine($"✅ Microsoft Learn MCP connected: {mcpServerUrl}");

        // ========================================================================
        // AGENT CREATION WITH DYNAMIC CAPABILITIES
        // ========================================================================
        // Create agent instructions based on available data sources
        // This demonstrates adaptive system design

        string instructions;
        if (hasSharePoint)
        {
            instructions = @"You are a Modern Workplace Assistant for Contoso Corporation.

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
- ""What is our MFA policy?"" → Search SharePoint for security policies
- ""How do I configure Azure AD Conditional Access?"" → Use Microsoft Learn for technical steps
- ""Our policy requires MFA - how do I implement this?"" → Combine policy requirements with implementation guidance";
        }
        else
        {
            instructions = @"You are a Technical Assistant with access to Microsoft Learn documentation.

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
- Suggest how technical implementations typically align with enterprise requirements";
        }

        // Create the agent with appropriate tool configuration
        Console.WriteLine("🛠️  Configuring agent tools...");
        Console.WriteLine($"   Available tools: {tools.Count}");

        PersistentAgent agent = await client.Administration.CreateAgentAsync(
            model: modelDeploymentName!,
            name: "Modern Workplace Assistant",
            instructions: instructions,
            tools: tools
        );

        Console.WriteLine($"✅ Agent created successfully: {agent.Id}");
        
        return (agent.Id, hasSharePoint);
    }

    /// <summary>
    /// Demonstrate realistic business scenarios combining internal and external knowledge.
    /// 
    /// This function showcases the practical value of the Modern Workplace Assistant
    /// by walking through scenarios that enterprise employees face regularly.
    /// 
    /// Educational Value:
    /// - Shows real business problems that AI agents can solve
    /// - Demonstrates integration between internal policies and external guidance
    /// - Illustrates how AI can bridge the gap between requirements and implementation
    /// </summary>
    private static async Task DemonstrateBusinessScenariosAsync(string agentId, bool hasSharePoint)
    {
        var scenarios = new[]
        {
            new
            {
                Title = "� Company Policy Question",
                Question = "What is our remote work security policy regarding multi-factor authentication?",
                Context = "Employee needs to understand company MFA requirements",
                ExpectedSource = "SharePoint",
                LearningPoint = "Internal policy retrieval and interpretation"
            },
            new
            {
                Title = "🔧 Technical Implementation Question",
                Question = "How do I set up Azure Active Directory conditional access policies?",
                Context = "IT administrator needs technical implementation steps",
                ExpectedSource = "Microsoft Learn MCP",
                LearningPoint = "External technical documentation access"
            },
            new
            {
                Title = "🔄 Combined Business Implementation Question",
                Question = "What Azure AD configuration should I implement to comply with our company's remote work security policy?",
                Context = "Need to combine policy requirements with technical implementation",
                ExpectedSource = "Both SharePoint and MCP",
                LearningPoint = "Multi-source intelligence combining internal requirements with external implementation"
            }
        };

        Console.WriteLine("\n" + "=".PadRight(70, '='));
        Console.WriteLine("🏢 MODERN WORKPLACE ASSISTANT - BUSINESS SCENARIO DEMONSTRATION");
        Console.WriteLine("=".PadRight(70, '='));
        Console.WriteLine("This demonstration shows how AI agents solve real business problems");
        Console.WriteLine("by combining internal company knowledge with external technical guidance.");
        Console.WriteLine("=".PadRight(70, '='));

        for (int i = 0; i < scenarios.Length; i++)
        {
            var scenario = scenarios[i];
            Console.WriteLine($"\n📊 SCENARIO {i + 1}/3: {scenario.Title}");
            Console.WriteLine("-".PadRight(50, '-'));
            Console.WriteLine($"❓ QUESTION: {scenario.Question}");
            Console.WriteLine($"🎯 BUSINESS CONTEXT: {scenario.Context}");
            Console.WriteLine($"📚 EXPECTED SOURCE: {scenario.ExpectedSource}");
            Console.WriteLine($"🎓 LEARNING POINT: {scenario.LearningPoint}");
            Console.WriteLine("-".PadRight(50, '-'));

            // Get response from the agent
            Console.WriteLine("🤖 ASSISTANT RESPONSE:");
            var (response, status) = await ChatWithAssistantAsync(agentId, scenario.Question);

            // Display response with analysis
            if (status == "completed" && !string.IsNullOrWhiteSpace(response) && response.Length > 10)
            {
                var preview = response.Length > 300 ? response.Substring(0, 300) + "..." : response;
                Console.WriteLine($"✅ SUCCESS: {preview}");
                if (response.Length > 300)
                {
                    Console.WriteLine($"   📏 Full response: {response.Length} characters");
                }
            }
            else
            {
                Console.WriteLine($"⚠️  LIMITED RESPONSE: {response}");
                if (!hasSharePoint && (scenario.ExpectedSource == "SharePoint" || scenario.ExpectedSource == "Both SharePoint and MCP"))
                {
                    Console.WriteLine("   💡 This demonstrates graceful degradation when SharePoint is unavailable");
                }
            }

            Console.WriteLine($"📈 STATUS: {status}");
            Console.WriteLine("-".PadRight(50, '-'));
        }

        Console.WriteLine("\n✅ DEMONSTRATION COMPLETED!");
        Console.WriteLine("🎓 Key Learning Outcomes:");
        Console.WriteLine("   • Multi-source data integration in enterprise AI");
        Console.WriteLine("   • Robust error handling and graceful degradation");
        Console.WriteLine("   • Real business value through combined intelligence");
        Console.WriteLine("   • Foundation for governance and monitoring (Tutorials 2-3)");
    }

    /// <summary>
    /// Execute a conversation with the workplace assistant.
    /// 
    /// This function demonstrates the conversation pattern for Azure AI Foundry agents
    /// and includes comprehensive error handling for production readiness.
    /// 
    /// Educational Value:
    /// - Shows proper thread management and conversation flow
    /// - Demonstrates non-streaming response handling
    /// - Includes timeout and error management patterns
    /// </summary>
    private static async Task<(string response, string status)> ChatWithAssistantAsync(string agentId, string message)
    {
        try
        {
            // Create conversation thread (maintains conversation context)
            PersistentAgentThread thread = await client!.Threads.CreateThreadAsync();

            // Add user message to thread
            await client.Messages.CreateMessageAsync(thread.Id, MessageRole.User, message);

            // ========================================================================
            // TOOL RESOURCE CONFIGURATION - EXPERIMENT #4
            // ========================================================================
            // Python passes: tool_resources=mcp_tool.resources (MCP only)
            // C# SDK Issue: Passing MCP tool_resources causes SharePoint to fail
            // 
            // NEW APPROACH: Don't pass tool_resources, handle MCP approval manually
            // This matches the official SharePoint sample which passes no tool_resources
            
            PersistentAgent agent = await client.Administration.GetAgentAsync(agentId);
            
            // Create run WITHOUT tool_resources
            // Both SharePoint and MCP are configured at agent level
            ThreadRun run = await client.Runs.CreateRunAsync(thread, agent);

            // Poll for completion with MCP approval handling
            // Since we didn't pass MCP tool_resources with "never" approval,
            // we need to manually approve MCP tool calls when they're requested
            while (run.Status == RunStatus.Queued || 
                   run.Status == RunStatus.InProgress || 
                   run.Status == RunStatus.RequiresAction)
            {
                await Task.Delay(TimeSpan.FromMilliseconds(500));
                run = await client.Runs.GetRunAsync(thread.Id, run.Id);

                // Handle MCP tool approval requests
                if (run.Status == RunStatus.RequiresAction && 
                    run.RequiredAction is SubmitToolApprovalAction toolApprovalAction)
                {
                    Console.WriteLine($"   🔧 MCP tool requires approval ({toolApprovalAction.SubmitToolApproval.ToolCalls.Count} calls)");
                    
                    var toolApprovals = new List<ToolApproval>();
                    foreach (var toolCall in toolApprovalAction.SubmitToolApproval.ToolCalls)
                    {
                        if (toolCall is RequiredMcpToolCall mcpToolCall)
                        {
                            Console.WriteLine($"      ✅ Auto-approving: {mcpToolCall.Name}");
                            toolApprovals.Add(new ToolApproval(mcpToolCall.Id, approve: true));
                        }
                    }

                    if (toolApprovals.Count > 0)
                    {
                        run = await client.Runs.SubmitToolOutputsToRunAsync(
                            thread.Id, 
                            run.Id, 
                            toolApprovals: toolApprovals
                        );
                    }
                }
            }

            string finalStatus = run.Status.ToString().ToLower();
            string responseText = string.Empty;

            // Log run status and any errors
            if (run.Status == RunStatus.Failed && run.LastError != null)
            {
                Console.WriteLine($"\n❌ Run failed: {run.LastError.Code} - {run.LastError.Message}");
            }

            if (run.Status == RunStatus.Completed)
            {
                // Retrieve the assistant's response
                AsyncPageable<PersistentThreadMessage> messages = client.Messages.GetMessagesAsync(
                    threadId: thread.Id,
                    order: ListSortOrder.Descending
                );

                await foreach (PersistentThreadMessage msg in messages)
                {
                    if (msg.Role == MessageRole.Agent)
                    {
                        foreach (MessageContent content in msg.ContentItems)
                        {
                            if (content is MessageTextContent textContent)
                            {
                                responseText = textContent.Text;
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            // Cleanup thread
            await client.Threads.DeleteThreadAsync(thread.Id);

            return (responseText, finalStatus);
        }
        catch (Exception ex)
        {
            Console.WriteLine($"\n❌ Exception details: {ex.GetType().Name}: {ex.Message}");
            if (ex.InnerException != null)
            {
                Console.WriteLine($"   Inner: {ex.InnerException.Message}");
            }
            return ($"Error in conversation: {ex.Message}", "failed");
        }
    }

    /// <summary>
    /// Interactive mode for testing the workplace assistant.
    /// 
    /// This provides a simple interface for users to test the agent with their own questions
    /// and see how it combines different data sources for comprehensive answers.
    /// </summary>
    private static async Task InteractiveModeAsync(string agentId)
    {
        Console.WriteLine("\n" + "=".PadRight(60, '='));
        Console.WriteLine("💬 INTERACTIVE MODE - Test Your Workplace Assistant!");
        Console.WriteLine("=".PadRight(60, '='));
        Console.WriteLine("Ask questions that combine company policies with technical guidance:");
        Console.WriteLine("• 'What's our remote work policy for Azure access?'");
        Console.WriteLine("• 'How do I configure SharePoint security?'");
        Console.WriteLine("• 'Our policy requires encryption - how do I set this up in Azure?'");
        Console.WriteLine("Type 'quit' to exit.");
        Console.WriteLine("-".PadRight(60, '-'));

        while (true)
        {
            try
            {
                Console.Write("\n❓ Your question: ");
                string? question = Console.ReadLine()?.Trim();

                if (string.IsNullOrEmpty(question))
                {
                    Console.WriteLine("💡 Please ask a question about policies or technical implementation.");
                    continue;
                }

                if (question.ToLower() is "quit" or "exit" or "bye")
                {
                    break;
                }

                Console.Write("\n🤖 Workplace Assistant: ");
                var (response, status) = await ChatWithAssistantAsync(agentId, question);
                Console.WriteLine(response);

                if (status != "completed")
                {
                    Console.WriteLine($"\n⚠️  Response status: {status}");
                }

                Console.WriteLine("-".PadRight(60, '-'));
            }
            catch (Exception ex)
            {
                Console.WriteLine($"\n❌ Error: {ex.Message}");
                Console.WriteLine("-".PadRight(60, '-'));
            }
        }

        Console.WriteLine("\n👋 Thank you for testing the Modern Workplace Assistant!");
    }
}
