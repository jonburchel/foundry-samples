# Modern Workplace Assistant - C# Implementation

Enterprise agent tutorial demonstrating SharePoint and MCP integration patterns using C#.

## Prerequisites

- .NET 8.0 or later
- Azure AI Foundry project with deployed model
- Azure CLI (`az login`)
- SharePoint connection (optional)
- Access to Microsoft Learn MCP server

## Setup

1. **Install .NET SDK** (if not already installed):
   ```bash
   # Download from https://dotnet.microsoft.com/download
   ```

2. **Configure environment**:
   ```bash
   cp .env.template .env
   # Edit .env with your Azure AI Foundry project details
   ```

3. **Restore dependencies**:
   ```bash
   dotnet restore
   ```

## Environment Variables

Required in `.env` file:

```bash
PROJECT_ENDPOINT=https://<your-project>.aiservices.azure.com
MODEL_DEPLOYMENT_NAME=gpt-4o-mini
AI_FOUNDRY_TENANT_ID=<your-tenant-id>  # Optional
MCP_SERVER_URL=https://learn.microsoft.com/api/mcp
SHAREPOINT_RESOURCE_NAME=your-sharepoint-connection  # Optional
SHAREPOINT_SITE_URL=https://your-company.sharepoint.com/teams/your-site  # Optional
```

## Running the Sample

### Main Application

```bash
dotnet run
```

This demonstrates:
- Agent creation with dynamic capabilities
- Three business scenarios (policy, technical, combined)
- Graceful degradation when services unavailable
- Interactive testing mode

### Evaluation

```bash
dotnet run --project Evaluate.cs
```

This runs batch evaluation against test questions and generates `evaluation_results.json`.

## Project Structure

```
csharp/enterprise-agent-tutorial/1-idea-to-prototype/
├── Program.cs                 # Main agent implementation
├── Evaluate.cs               # Evaluation framework  
├── ModernWorkplaceAssistant.csproj  # Project file
├── questions.jsonl           # Test questions
├── .env.template             # Environment template
├── README.md                 # This file
├── MCP_SERVERS.md           # MCP server options
└── SAMPLE_SHAREPOINT_CONTENT.md  # Sample policies
```

## Sample Code Patterns

### Create Agent with Tools

```csharp
var agent = await projectClient.GetAgentsClient().CreateAgentAsync(
    model: "gpt-4o-mini",
    name: "Modern Workplace Assistant",
    instructions: "You are a helpful assistant...",
    tools: new List<ToolDefinition>
    {
        new ToolDefinition { Type = "mcp", ServerUrl = mcpUrl }
    }
);
```

### Chat with Agent

```csharp
var thread = await agentsClient.CreateThreadAsync();
await agentsClient.CreateMessageAsync(thread.Value.Id, MessageRole.User, "Your question");
var run = await agentsClient.CreateRunAsync(thread.Value.Id, agent.Id);

// Poll for completion
while (run.Value.Status == RunStatus.InProgress)
{
    await Task.Delay(500);
    run = await agentsClient.GetRunAsync(thread.Value.Id, run.Value.Id);
}
```

## Troubleshooting

### Build Errors

If you see missing SDK errors:
```bash
dotnet clean
dotnet restore
dotnet build
```

### Runtime Errors

1. **Authentication**: Ensure `az login` is successful
2. **Environment**: Verify all required variables in `.env`
3. **Endpoint**: Check PROJECT_ENDPOINT format (https://...)
4. **Model**: Confirm MODEL_DEPLOYMENT_NAME matches your deployment

## Notes

- This sample uses preview SDK features (beta.4)
- SharePoint/MCP tool classes pending SDK release
- Current implementation uses inline tool definitions
- Will be updated with full SDK support post-Ignite

## Learn More

- [Azure AI Foundry Agent Documentation](../../developer-journey-stage-1-idea-to-prototype.md)
- [C# SDK Reference](https://learn.microsoft.com/dotnet/api/azure.ai.projects)
- [Tutorial Series Overview](../../../enterprise-agent-tutorial/README.md)
