# C# SDK Limitations - SharePoint Integration

## Issue Summary

The C# implementation successfully demonstrates MCP integration but encounters a SharePoint configuration issue that differs from the Python SDK behavior.

## Current Status

✅ **Working**: MCP (Model Context Protocol) integration
- Successfully connects to Microsoft Learn MCP server
- Properly handles tool approvals with `MCPApproval("never")`
- Returns complete technical responses

❌ **Issue**: SharePoint integration
- Tool definition creates successfully
- Agent accepts the SharePoint tool
- **Runtime Error**: `sharepoint_tool_misconfigured - connection_id must have matching 'connection-id' in 'custom_connections'`

## Technical Details

### Error Message
```
Error: sharepoint_tool_misconfigured; 1 validation error for SharepointToolConfigKwarg
Value error, [Sharepoint-tool] The connection_id ContosoPolicies for tool_connections[0] 
must have a matching 'connection-id' in 'custom_connections'.
```

### Root Cause

The C# SDK requires SharePoint connections to be explicitly provided in a `custom_connections` structure within `ToolResources` during run creation, but this API is not currently exposed in the public SDK.

### Python vs C# Difference

**Python (Working)**:
```python
sharepoint_tool = SharepointTool(connection_id=sharepoint_conn.id)
agent = project_client.agents.create_agent(tools=[sharepoint_tool])
# Run with only MCP resources - SharePoint works automatically
run = project_client.agents.runs.stream(tool_resources=mcp_tool.resources)
```

**C# (Not Working)**:
```csharp
SharepointToolDefinition sharepointTool = new(new SharepointGroundingToolParameters(connectionId));
PersistentAgent agent = await client.Administration.CreateAgentAsync(tools: [sharepointTool]);
// SharePoint requires custom_connections in ToolResources, but API not available
ThreadRun run = await client.Runs.CreateRunAsync(thread, agent);
```

## Attempted Solutions

1. ✅ Retrieved actual connection object with `client.Connections.GetConnectionAsync()`
2. ✅ Used connection.Id from retrieved connection
3. ✅ Tried without passing any ToolResources (like official SharePoint sample)
4. ❌ Cannot access `custom_connections` API in current SDK version

## Workaround

Until the C# SDK exposes the `custom_connections` API for SharePoint, the sample demonstrates:

1. **Full MCP integration** - Shows external data source integration
2. **Graceful degradation** - Agent works with MCP-only when SharePoint unavailable
3. **Proper error handling** - Clear diagnostic messages
4. **Production patterns** - Error handling, logging, configuration management

## Recommendation

For production SharePoint + MCP integration in C#, consider:

1. **Wait for SDK update** that exposes `custom_connections` configuration
2. **Use Python implementation** which fully supports SharePoint + MCP
3. **Contact Azure AI Foundry team** for C# SharePoint + MCP integration guidance
4. **Monitor SDK releases** for updates to SharePoint tool resource configuration

## References

- Official C# SharePoint Sample: [Sample21_PersistentAgents_Sharepoint.md](https://github.com/Azure/azure-sdk-for-net/blob/main/sdk/ai/Azure.AI.Agents.Persistent/samples/Sample21_PersistentAgents_Sharepoint.md)
- Official C# MCP Sample: [Sample32_PersistentAgents_MCP.md](https://github.com/Azure/azure-sdk-for-net/blob/main/sdk/ai/Azure.AI.Agents.Persistent/samples/Sample32_PersistentAgents_MCP.md)
- Python Implementation: `python/enterprise-agent-tutorial/1-idea-to-prototype/main.py` (fully working)

## Last Updated
2025-10-13
