#!/usr/bin/env python3
"""
Agent Creation Module

Handles Azure AI client initialization and agent creation.
Returns initialized clients and the created agent.
"""

import os
from azure.ai.projects import AIProjectClient
from azure.identity import DefaultAzureCredential
from azure.ai.agents.models import (
    SharepointAgentTool, 
    SharepointGroundingToolParameters, 
    ToolProjectConnectionList, 
    MCPTool,
    PromptAgentDefinition,
)


class AgentCreation:
    """Manages agent creation and client initialization."""
    
    def __init__(self):
        """Initialize with environment configuration."""
        self.project_endpoint = os.environ["PROJECT_ENDPOINT"]
        self.model_deployment = os.environ["MODEL_DEPLOYMENT_NAME"]
        self.sharepoint_resource = os.environ.get("SHAREPOINT_RESOURCE_NAME")
        self.mcp_server_url = os.environ["MCP_SERVER_URL"]
    
    def create_production_ready_agent(self):
        """
        Initialize Azure AI clients and create the Modern Workplace Assistant.
        
        Returns:
            Tuple of (project_client, agent)
        """
        print("🔐 Initializing Azure AI clients...")
        
        # <authentication_to_azure>
        # NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        credential = DefaultAzureCredential()
        project_client = AIProjectClient(
            endpoint=self.project_endpoint,
            credential=credential,
        )
        # </authentication_to_azure>
        
        print("✅ Clients initialized successfully")
        
        # Create the Modern Workplace Assistant
        agent = self._create_workplace_assistant(project_client)
        
        return project_client, agent
    
    def _create_workplace_assistant(self, project_client):
        """
        Create the Modern Workplace Assistant from Tutorial 1.
        This agent combines SharePoint and MCP for comprehensive workplace assistance.
        """
        print("\n🤖 Creating Modern Workplace Assistant (from Tutorial 1)...")
        
        # Configure SharePoint tool
        sharepoint_tool = None
        try:
            if self.sharepoint_resource:
                sharepoint_conn = project_client.connections.get(name=self.sharepoint_resource)
                sharepoint_grounding_params = SharepointGroundingToolParameters(
                    project_connections=ToolProjectConnectionList(connections=[{"id": sharepoint_conn.id}])
                )
                sharepoint_tool = SharepointAgentTool(sharepoint_grounding=sharepoint_grounding_params)
                print(f"   ✅ SharePoint connected: {self.sharepoint_resource}")
        except Exception as e:
            print(f"   ⚠️  SharePoint unavailable (continuing without): {e}")
        
        # Configure MCP tool
        mcp_tool = MCPTool(
            server_label="microsoft_learn",
            server_url=self.mcp_server_url,
            allowed_tools=[]
        )
        print(f"   ✅ Microsoft Learn MCP connected")
        
        # Create agent with available tools
        tools = [tool for tool in [sharepoint_tool, mcp_tool] if tool is not None]
        
        agent = project_client.agents.create_version(
            agent_name="Modern Workplace Assistant",
            definition=PromptAgentDefinition(
                model=self.model_deployment,
                instructions="""You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search SharePoint for company-specific requirements
- For technical questions: Use Microsoft Learn for current documentation
- For implementation questions: Combine both sources
- Always cite your sources and provide step-by-step guidance
- Explain how internal requirements connect to technical implementation
""",
                tools=tools,
            )
        )
        
        print(f"✅ Agent created: {agent.id}")
        return agent
