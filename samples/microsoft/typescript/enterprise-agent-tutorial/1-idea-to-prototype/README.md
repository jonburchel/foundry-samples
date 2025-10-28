# Modern Workplace Assistant - TypeScript Implementation

This sample demonstrates a complete business scenario using Azure AI Agents SDK v2 in TypeScript:
- Agent creation with the new SDK
- Thread and message management  
- SharePoint integration for internal company knowledge
- Microsoft Learn MCP integration for external technical guidance
- Robust error handling and graceful degradation

## Prerequisites

- Node.js 18 or later
- Azure CLI installed and authenticated (`az login`)
- Azure AI Foundry project with:
  - GPT-4 model deployment
  - (Optional) SharePoint connection configured
  - (Optional) MCP server URL for Microsoft Learn

## Setup

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Configure environment variables:**
   
   Copy the `.env.template` file from the shared directory and update with your values:
   ```bash
   cp ../.env.template .env
   ```

   Required variables:
   - `PROJECT_ENDPOINT`: Your Azure AI Foundry project endpoint
   - `MODEL_DEPLOYMENT_NAME`: Your GPT-4 deployment name

   Optional variables (for full functionality):
   - `SHAREPOINT_RESOURCE_NAME`: Name of your SharePoint connection
   - `MCP_SERVER_URL`: URL for Microsoft Learn MCP server

## Running the Sample

```bash
# Build the TypeScript code
npm run build

# Run the application
npm start

# Or build and run in one command
npm run dev
```

## What It Does

The Modern Workplace Assistant demonstrates:

1. **Company Policy Questions** - Searches SharePoint for internal policies
2. **Technical Documentation** - Accesses Microsoft Learn for implementation guidance  
3. **Combined Implementation** - Maps company policies to technical steps

The sample showcases:
- Dynamic agent capabilities based on available resources
- Graceful degradation when tools are unavailable
- Production-ready error handling
- Interactive testing mode

## Architecture

- **Agent SDK v2**: Uses latest Azure AI Agents patterns
- **SharePoint Tool**: Accesses internal company documentation
- **MCP Tool**: Retrieves Microsoft Learn documentation
- **Thread Management**: Proper conversation state handling
- **Auto-approval**: MCP tool calls are automatically approved

## Educational Value

This tutorial teaches:
- Enterprise AI patterns with Agent SDK v2
- Real-world business scenarios
- Production-ready error handling  
- Foundation for governance and monitoring (Tutorials 2-3)

## Next Steps

- **Tutorial 2**: Add evaluation metrics and governance
- **Tutorial 3**: Production deployment and monitoring

## Troubleshooting

If you encounter issues:
1. Verify Azure credentials: `az login`
2. Check PROJECT_ENDPOINT is correct
3. Ensure MODEL_DEPLOYMENT_NAME is deployed
4. For SharePoint: Verify connection exists in Azure AI Foundry portal
5. Check that @azure/ai-agents v2 is installed

## Learn More

- [Azure AI Foundry Documentation](https://learn.microsoft.com/azure/ai-foundry/)
- [Azure AI Agents SDK](https://github.com/Azure/azure-sdk-for-js/tree/main/sdk/ai/ai-agents)
- [MCP Protocol](https://modelcontextprotocol.io/)
