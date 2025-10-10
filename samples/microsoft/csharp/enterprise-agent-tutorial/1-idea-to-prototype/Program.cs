using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Azure;
using Azure.AI.Agents.Persistent;
using Azure.Identity;
using DotNetEnv;

Env.Load();

var projectEndpoint = Environment.GetEnvironmentVariable("PROJECT_ENDPOINT");
var modelDeploymentName = Environment.GetEnvironmentVariable("MODEL_DEPLOYMENT_NAME");
var sharepointSiteUrl = Environment.GetEnvironmentVariable("SHAREPOINT_SITE_URL");
var mcpServerUrl = Environment.GetEnvironmentVariable("MCP_SERVER_URL") ?? "https://learn.microsoft.com/api/mcp";

PersistentAgentsClient client = new(projectEndpoint, new DefaultAzureCredential());

Console.WriteLine("🤖 Creating Modern Workplace Assistant...\n");

bool hasSharePoint = false;
List<ToolDefinition> tools = new();
ToolResources toolResources = null;

if (!string.IsNullOrEmpty(sharepointSiteUrl))
{
    try
    {
        Console.WriteLine("📁 Configuring SharePoint integration...");
        Console.WriteLine($"   Site URL: {sharepointSiteUrl}");

        VectorStoreDataSource dataSource = new(
            assetIdentifier: sharepointSiteUrl,
            assetType: VectorStoreDataSourceAssetType.UriAsset
        );

        PersistentAgentsVectorStore vectorStore = await client.VectorStores.CreateVectorStoreAsync(
            name: "company_policies",
            storeConfiguration: new VectorStoreConfiguration(dataSources: new[] { dataSource })
        );

        FileSearchToolResource fileSearchResource = new(new[] { vectorStore.Id }, null);
        toolResources = new ToolResources { FileSearch = fileSearchResource };
        tools.Add(new FileSearchToolDefinition());

        Console.WriteLine("✅ SharePoint connected via Enterprise File Search");
        hasSharePoint = true;
    }
    catch (Exception ex)
    {
        Console.WriteLine($"⚠️  SharePoint connection failed: {ex.Message}");
        Console.WriteLine("   Agent will operate in technical guidance mode only");
    }
}

Console.WriteLine("📚 Microsoft Learn integration via MCP...");
Console.WriteLine($"   MCP URL: {mcpServerUrl}");
Console.WriteLine("   Note: MCP integration requires runtime support");

var instructions = hasSharePoint ?
    @"You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search company policies and procedures in SharePoint
- Access Microsoft Learn for Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search company documents for specific requirements
- For technical questions: Use Microsoft Learn for Azure/M365 documentation
- For implementation questions: Combine both sources to show how company policies map to technical implementation" :
    @"You are a Technical Assistant with access to Microsoft Learn documentation.

CAPABILITIES:
- Provide Azure and Microsoft 365 technical guidance
- Offer detailed implementation steps and best practices

LIMITATIONS:
- SharePoint integration is not available
- Cannot access company-specific policies";

Console.WriteLine($"\n🛠️  Creating agent with {tools.Count} tool(s)...");

PersistentAgent agent = await client.Administration.CreateAgentAsync(
    model: modelDeploymentName,
    name: "Modern Workplace Assistant",
    instructions: instructions,
    tools: tools,
    toolResources: toolResources
);

Console.WriteLine($"✅ Agent created: {agent.Id}\n");

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
    
    do
    {
        await Task.Delay(TimeSpan.FromMilliseconds(1000));
        run = await client.Runs.GetRunAsync(thread.Id, run.Id);
    }
    while (run.Status == RunStatus.Queued || run.Status == RunStatus.InProgress);

    if (run.Status == RunStatus.Completed)
    {
        AsyncPageable<PersistentThreadMessage> messages = client.Messages.GetMessagesAsync(
            threadId: thread.Id,
            order: ListSortOrder.Descending
        );

        await foreach (PersistentThreadMessage message in messages)
        {
            if (message.Role == MessageRole.Agent)
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

await client.Administration.DeleteAgentAsync(agent.Id);
