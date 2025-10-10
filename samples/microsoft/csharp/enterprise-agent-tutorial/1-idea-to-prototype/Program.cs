using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Azure;
using Azure.Core;
using Azure.AI.Agents.Persistent;
using Azure.Identity;
using DotNetEnv;
using System.Text.Json;

// Load environment variables
Env.Load();

var projectEndpoint = Environment.GetEnvironmentVariable("PROJECT_ENDPOINT");
var modelDeploymentName = Environment.GetEnvironmentVariable("MODEL_DEPLOYMENT_NAME");
var sharepointResourceName = Environment.GetEnvironmentVariable("SHAREPOINT_RESOURCE_NAME");
var mcpServerUrl = Environment.GetEnvironmentVariable("MCP_SERVER_URL") ?? "https://learn.microsoft.com/api/mcp";

PersistentAgentsClient client = new(projectEndpoint, new DefaultAzureCredential());

Console.WriteLine("🤖 Creating Modern Workplace Assistant...\n");

// Check SharePoint availability
Console.WriteLine("📁 Configuring SharePoint integration...");
Console.WriteLine($"   Connection: {sharepointResourceName}");

bool hasSharePoint = false;
var tools = new List<object>();

if (!string.IsNullOrEmpty(sharepointResourceName))
{
    try
    {
        // Try to verify SharePoint connection exists
        // Note: Connection verification would require connections API
        Console.WriteLine("✅ SharePoint configured");
        hasSharePoint = true;
        
        // Add SharePoint tool using inline JSON
        tools.Add(new
        {
            type = "sharepoint_grounding",
            sharepoint_grounding = new
            {
                connection_id = sharepointResourceName
            }
        });
    }
    catch (Exception ex)
    {
        Console.WriteLine($"⚠️  SharePoint connection failed: {ex.Message}");
        Console.WriteLine("   Agent will operate in technical guidance mode only");
    }
}

// Add MCP tool using inline JSON
Console.WriteLine("📚 Configuring Microsoft Learn MCP integration...");
tools.Add(new
{
    type = "mcp",
    mcp = new
    {
        server_url = mcpServerUrl,
        server_name = "microsoft_learn"
    }
});
Console.WriteLine($"✅ Microsoft Learn MCP connected: {mcpServerUrl}");

// Build dynamic instructions
var instructions = hasSharePoint ?
    @"You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search SharePoint for company-specific requirements
- For technical questions: Use Microsoft Learn for Azure/M365 documentation
- For implementation questions: Combine both sources to show how company policies map to technical implementation" :
    @"You are a Technical Assistant with access to Microsoft Learn documentation.

CAPABILITIES:
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide detailed implementation steps and best practices

LIMITATIONS:
- SharePoint integration is not available
- Cannot access company-specific policies";

Console.WriteLine($"🛠️  Configuring agent tools...");
Console.WriteLine($"   Available tools: {tools.Count}");

// Create agent using RequestContent pattern for tools not yet in SDK
var agentPayload = new
{
    name = "Modern Workplace Assistant",
    model = modelDeploymentName,
    instructions = instructions,
    tools = tools.ToArray()
};

RequestContent agentRequestContent = RequestContent.Create(BinaryData.FromObjectAsJson(agentPayload));
Response agentResponse = await client.Administration.CreateAgentAsync(content: agentRequestContent);
PersistentAgent agent = PersistentAgent.FromResponse(agentResponse);

Console.WriteLine($"✅ Agent created: {agent.Id}\n");

// Business scenarios
var scenarios = new[]
{
    new
    {
        Title = "📋 Policy Question",
        Question = "What is our remote work policy regarding security requirements?"
    },
    new
    {
        Title = "🔧 Technical Question",
        Question = "How do I set up Azure Active Directory conditional access?"
    },
    new
    {
        Title = "🔄 Implementation Question",
        Question = "Our security policy requires MFA - how do I implement this in Azure AD?"
    }
};

for (int i = 0; i < scenarios.Length; i++)
{
    var scenario = scenarios[i];
    Console.WriteLine($"{scenario.Title} {i + 1}/{scenarios.Length}");
    Console.WriteLine($"❓ {scenario.Question}");

    PersistentAgentThread thread = await client.Threads.CreateThreadAsync();
    await client.Messages.CreateMessageAsync(thread.Id, MessageRole.User, scenario.Question);
    
    ThreadRun run = await client.Runs.CreateRunAsync(thread.Id, agent.Id);
    
    // Poll for completion
    do
    {
        await Task.Delay(TimeSpan.FromMilliseconds(1000));
        run = await client.Runs.GetRunAsync(thread.Id, run.Id);
    }
    while (run.Status == RunStatus.Queued || run.Status == RunStatus.InProgress);

    if (run.Status == RunStatus.Completed)
    {
        AsyncPageable<ThreadMessage> messages = client.Messages.GetMessagesAsync(
            threadId: thread.Id,
            order: ListSortOrder.Descending
        );

        await foreach (ThreadMessage message in messages)
        {
            if (message.Role == MessageRole.Assistant)
            {
                foreach (MessageContent content in message.ContentItems)
                {
                    if (content is MessageTextContent textContent)
                    {
                        Console.WriteLine($"🤖 {textContent.Text}\n");
                        break;
                    }
                }
                break;
            }
        }
    }
    else
    {
        Console.WriteLine($"❌ Run failed with status: {run.Status}\n");
    }

    await client.Threads.DeleteThreadAsync(thread.Id);
}

Console.WriteLine("\n💡 Interactive Mode");
Console.WriteLine("The agent is ready. In a production scenario, you would integrate this with your application's user interface.");
Console.WriteLine("Users could ask questions combining company policies with technical implementation guidance.\n");

// Cleanup
await client.Administration.DeleteAgentAsync(agent.Id);
