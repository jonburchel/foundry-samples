# Modern Workplace Assistant - TypeScript Implementation

Enterprise agent combining SharePoint internal knowledge with Microsoft Learn external guidance using the Azure AI Foundry SDK.

## Prerequisites

- Node.js 18 or later
- Azure AI Foundry project with deployed model (e.g., `gpt-4o-mini`)
- SharePoint connection configured in your project (optional)
- Azure CLI authentication (`az login`)

## Setup

1. **Install dependencies**:
   ```bash
   npm install
   ```

2. **Configure environment**:
   ```bash
   cp .env.template .env
   # Edit .env with your Azure AI Foundry project details
   ```

3. **Upload SharePoint documents** (optional):
   - Follow instructions in `SAMPLE_SHAREPOINT_CONTENT.md`
   - Upload sample business documents to your SharePoint site
   - Ensure SharePoint connection is configured in Azure AI Foundry

## Environment Variables

```env
# Azure AI Foundry Configuration
PROJECT_ENDPOINT=https://<your-project>.aiservices.azure.com
MODEL_DEPLOYMENT_NAME=gpt-4o-mini
AI_FOUNDRY_TENANT_ID=<your-tenant-id>

# Microsoft Learn MCP Server
MCP_SERVER_URL=https://learn.microsoft.com/api/mcp

# SharePoint Integration (Optional)
SHAREPOINT_RESOURCE_NAME=your-sharepoint-connection
SHAREPOINT_SITE_URL=https://your-company.sharepoint.com/teams/your-site
```

## Running the Sample

### Main Agent Demo

Run the Modern Workplace Assistant with three business scenarios:

```bash
npm start
```

This demonstrates:
- **Policy Question**: "What is our remote work policy regarding security requirements?"
- **Technical Question**: "How do I set up Azure Active Directory conditional access?"
- **Implementation Question**: "Our security policy requires MFA - how do I implement this in Azure AD?"

### Batch Evaluation

Run the evaluation framework against test questions:

```bash
npm run evaluate
```

This:
- Loads questions from `questions.jsonl`
- Tests agent responses against expected sources
- Generates `evaluation_results.json` with detailed metrics
- Reports pass/fail status for each question

## Project Structure

```
typescript/enterprise-agent-tutorial/1-idea-to-prototype/
├── src/
│   ├── main.ts              # Main agent implementation
│   └── evaluate.ts          # Evaluation framework
├── package.json             # Dependencies and scripts
├── tsconfig.json            # TypeScript configuration
├── .env.template            # Environment variables template
├── questions.jsonl          # Evaluation test questions
├── MCP_SERVERS.md           # MCP server configuration guide
├── SAMPLE_SHAREPOINT_CONTENT.md  # Sample business documents
└── README.md                # This file
```

## Implementation Notes

### Tool Definitions

This TypeScript implementation uses inline JSON tool definitions since the SDK doesn't yet have `SharepointTool` and `McpTool` classes:

```typescript
const tools: any[] = [];

// SharePoint tool
if (sharepointAvailable && sharepointResourceName) {
    tools.push({
        type: "sharepoint_grounding",
        sharepoint_grounding: {
            connection_id: sharepointResourceName
        }
    });
}

// MCP tool
tools.push({
    type: "mcp",
    mcp: {
        server_url: mcpServerUrl,
        server_name: "microsoft_learn"
    }
});
```

Once the TypeScript SDK achieves feature parity (expected post-Microsoft Ignite), this can be replaced with dedicated tool classes.

### Graceful Degradation

The agent gracefully handles missing SharePoint connections:

```typescript
if (sharepointAvailable) {
    // Full instructions with both SharePoint and MCP
    instructions += "\n\nYou have access to:\n1. SharePoint for company policies\n2. Microsoft Learn for technical guidance";
} else {
    // MCP-only instructions
    instructions += "\n\nYou have access to Microsoft Learn for technical guidance.";
}
```

## Expected Output

### Successful Run with SharePoint

```
🤖 Creating Modern Workplace Assistant...
✅ SharePoint connected: YourConnection
✅ Agent created: asst_abc123

📋 Policy Question 1/3
❓ What is our remote work policy regarding security requirements?
🤖 According to our remote work policy...

🔧 Technical Question 2/3
❓ How do I set up Azure Active Directory conditional access?
🤖 To set up Azure AD Conditional Access...

🔄 Implementation Question 3/3
❓ Our security policy requires MFA - how do I implement this in Azure AD?
🤖 Based on our security policy and Azure documentation...
```

### Evaluation Results

```
📊 Starting Business Evaluation
✅ Agent created for evaluation: asst_xyz789
📝 Loaded 4 evaluation questions

Question 1/4: What is our remote work policy regarding security requirements?
✅ PASS - Found sources: SharePoint

Question 2/4: How do I set up Azure Active Directory conditional access?
✅ PASS - Found sources: Microsoft Learn

Question 3/4: What are the approved collaboration tools for remote teams?
✅ PASS - Found sources: SharePoint

Question 4/4: Our security policy requires MFA - how do I implement this in Azure AD?
✅ PASS - Found sources: SharePoint, Microsoft Learn

==================================================
📊 Evaluation Complete: 4/4 passed
==================================================

💾 Results saved to evaluation_results.json
```

## Troubleshooting

### Module Not Found Errors

If you see TypeScript errors about missing modules:

```bash
npm install
```

### Authentication Issues

Ensure you're logged in to Azure CLI:

```bash
az login
az account show
```

### SharePoint Connection Not Found

If SharePoint connection fails:
1. Verify connection name in Azure AI Foundry portal
2. Update `SHAREPOINT_RESOURCE_NAME` in `.env`
3. Agent will still work with Microsoft Learn only

## Next Steps

- **Tutorial 2**: Add governance, monitoring, and advanced evaluation
- **Tutorial 3**: Deploy to production with scaling and observability
- **Enhancements**: Add more data sources, custom tools, conversation memory

## Related Documentation

- [Azure AI Foundry Agent Service](https://learn.microsoft.com/azure/ai-services/agents/)
- [SharePoint Tool Documentation](https://learn.microsoft.com/azure/ai-services/agents/how-to/tools/sharepoint)
- [MCP Tool Integration](https://learn.microsoft.com/azure/ai-services/agents/how-to/tools/model-context-protocol)
- [Multi-Agent Patterns](https://learn.microsoft.com/azure/ai-services/agents/how-to/connected-agents)
