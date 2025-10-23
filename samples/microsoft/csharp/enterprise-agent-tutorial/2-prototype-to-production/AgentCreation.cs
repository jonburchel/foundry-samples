// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

using Azure.AI.Agents;
using Azure.AI.Agents.Models;
using Azure.Core.Credential;
using Azure.Identity;
using DotNetEnv;

namespace Microsoft.Azure.Samples.PrototypeToProduction;

/// <summary>
/// Handles Azure AI Foundry client initialization and agent creation.
/// Contains authentication setup and agent configuration from Tutorial 1.
/// </summary>
public class AgentCreation
{
    private readonly string _endpoint;
    private readonly string _modelDeploymentName;
    private readonly string? _sharepointResourceName;
    private readonly string? _mcpServerUrl;

    public AgentsClient AgentsClient { get; private set; } = null!;
    public ResponsesClient ResponsesClient { get; private set; } = null!;
    public ConversationsClient ConversationsClient { get; private set; } = null!;

    public AgentCreation()
    {
        Env.Load();
        _endpoint = Env.GetString("PROJECT_ENDPOINT");
        _modelDeploymentName = Env.GetString("MODEL_DEPLOYMENT_NAME");
        _sharepointResourceName = Env.GetString("SHAREPOINT_RESOURCE_NAME", "");
        _mcpServerUrl = Env.GetString("MCP_SERVER_URL", "");
    }

    /// <summary>
    /// Initialize Azure AI Foundry clients for agent operations.
    /// </summary>
    public void InitializeClients()
    {
        Console.WriteLine("🔐 Initializing Azure AI clients...");

        // <authentication_to_azure>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        string? aiFoundryTenantId = Env.GetString("AI_FOUNDRY_TENANT_ID", null);
        TokenCredential credential;
        
        if (!string.IsNullOrEmpty(aiFoundryTenantId))
        {
            Console.WriteLine($"   Using AI Foundry tenant: {aiFoundryTenantId}");
            credential = new AzureCliCredential();
        }
        else
        {
            credential = new DefaultAzureCredential();
        }

        // Build clients
        var builder = new AgentsClientBuilder()
            .Credential(credential)
            .Endpoint(_endpoint);

        AgentsClient = builder.BuildClient();
        ResponsesClient = builder.BuildResponsesClient();
        ConversationsClient = builder.BuildConversationsClient();
        // </authentication_to_azure>

        Console.WriteLine("✅ Clients initialized successfully");
    }

    /// <summary>
    /// Creates a production-ready agent and returns all necessary components.
    /// </summary>
    /// <returns>Tuple containing agent, clients, and model deployment name</returns>
    public async Task<(AgentVersionObject agent, AgentsClient client, ResponsesClient responseClient, string modelDeploymentName)> CreateProductionReadyAgentAsync()
    {
        InitializeClients();
        var agent = CreateWorkplaceAssistant();
        return (agent, AgentsClient, ResponsesClient, _modelDeploymentName);
    }

    /// <summary>
    /// Create the Modern Workplace Assistant from Tutorial 1.
    /// This agent combines SharePoint and MCP for comprehensive workplace assistance.
    /// </summary>
    public AgentVersionObject CreateWorkplaceAssistant()
    {
        Console.WriteLine("\n🤖 Creating Modern Workplace Assistant (from Tutorial 1)...");

        var tools = new List<object>();

        // Add SharePoint tool if configured
        if (!string.IsNullOrEmpty(_sharepointResourceName))
        {
            try
            {
                var connections = new ToolProjectConnectionList();
                var groundingParams = new SharepointGroundingToolParameters
                {
                    ProjectConnections = connections
                };
                var sharepointTool = new SharepointAgentTool
                {
                    SharepointGrounding = groundingParams
                };
                tools.Add(sharepointTool);
                Console.WriteLine("   ✅ SharePoint tool configured");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"   ⚠️  SharePoint tool unavailable: {ex.Message}");
            }
        }

        // Add MCP tool
        if (!string.IsNullOrEmpty(_mcpServerUrl))
        {
            var mcpTool = new MCPTool
            {
                ServerLabel = "microsoft_learn",
                ServerUrl = _mcpServerUrl,
                AllowedTools = new List<string>()
            };
            tools.Add(mcpTool);
            Console.WriteLine("   ✅ MCP tool configured");
        }

        string instructions = @"You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search SharePoint for company-specific requirements
- For technical questions: Use Microsoft Learn for current documentation
- For implementation questions: Combine both sources
- Always cite your sources and provide step-by-step guidance
- Explain how internal requirements connect to technical implementation";

        var definition = new PromptAgentDefinition(_modelDeploymentName)
        {
            Instructions = instructions,
            Tools = tools
        };

        var agent = AgentsClient.CreateAgentVersion(
            "Modern Workplace Assistant",
            definition
        );

        Console.WriteLine($"✅ Agent created: {agent.Id}");
        return agent;
    }
}
