using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading.Tasks;

namespace Microsoft.Azure.Samples.PrototypeToProduction
{
    /// <summary>
    /// Production Deployment module for endpoint configuration and external consumption.
    /// This module provides:
    /// - Container Apps endpoint configuration
    /// - Azure Marketplace integration
    /// - Logic Apps connector setup
    /// - Managed identity and authentication
    /// - API Management integration
    /// </summary>
    public class ProductionDeployment
    {
        /// <summary>
        /// Creates production endpoint configuration for deploying the agent.
        /// </summary>
        /// <param name="agentName">Name of the agent to deploy</param>
        /// <returns>Endpoint configuration dictionary</returns>
        public async Task<Dictionary<string, object>> CreateProductionEndpointAsync(string agentName)
        {
            Console.WriteLine("\n🚀 PRODUCTION ENDPOINT DEPLOYMENT");
            Console.WriteLine(new string('=', 70));

            // <endpoint_configuration>
            // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
            var endpointConfig = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["agent_name"] = agentName,
                ["endpoint"] = new Dictionary<string, object>
                {
                    ["type"] = "container_app",
                    ["url"] = $"https://{agentName.ToLower().Replace(" ", "-")}.azurecontainerapps.io",
                    ["api_version"] = "2025-05-15-preview",
                    ["authentication"] = "managed_identity"
                },
                ["marketplace"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["listing_name"] = "Modern Workplace Assistant",
                    ["category"] = "AI + Machine Learning",
                    ["pricing_model"] = "consumption_based",
                    ["description"] = "Enterprise AI assistant combining internal knowledge with external guidance"
                },
                ["logic_apps"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["connector_name"] = "workplace-assistant-connector",
                    ["actions"] = new[] {
                        "ask_question",
                        "search_policy",
                        "get_technical_guidance"
                    },
                    ["triggers"] = new[] {
                        "new_question_received",
                        "policy_update_needed"
                    }
                },
                ["identity"] = new Dictionary<string, object>
                {
                    ["type"] = "managed_identity",
                    ["principal_id"] = "auto-generated",
                    ["tenant_id"] = Environment.GetEnvironmentVariable("AZURE_TENANT_ID") ?? "your-tenant-id",
                    ["permissions"] = new[] {
                        "Agent.Read",
                        "Agent.Execute",
                        "Conversation.Create",
                        "Response.Create"
                    }
                },
            // </endpoint_configuration>
                ["api_management"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["rate_limiting"] = new Dictionary<string, object>
                    {
                        ["requests_per_minute"] = 100,
                        ["requests_per_hour"] = 5000
                    },
                    ["cors"] = new Dictionary<string, object>
                    {
                        ["enabled"] = true,
                        ["allowed_origins"] = new[] { "https://*.microsoft.com", "https://*.azure.com" }
                    },
                    ["api_key_required"] = true
                }
            };

            var endpoint = (Dictionary<string, object>)endpointConfig["endpoint"];
            var marketplace = (Dictionary<string, object>)endpointConfig["marketplace"];
            var logicApps = (Dictionary<string, object>)endpointConfig["logic_apps"];
            var identity = (Dictionary<string, object>)endpointConfig["identity"];

            Console.WriteLine("🌐 Endpoint Configuration:");
            Console.WriteLine($"   Type: {endpoint["type"]}");
            Console.WriteLine($"   URL: {endpoint["url"]}");
            Console.WriteLine($"   Authentication: {endpoint["authentication"]}");

            Console.WriteLine($"\n📦 Marketplace Integration:");
            Console.WriteLine($"   Status: {((bool)marketplace["enabled"] ? "✅ ENABLED" : "❌ DISABLED")}");
            Console.WriteLine($"   Listing: {marketplace["listing_name"]}");
            Console.WriteLine($"   Pricing: {marketplace["pricing_model"]}");

            Console.WriteLine($"\n🔗 Logic Apps Integration:");
            Console.WriteLine($"   Status: {((bool)logicApps["enabled"] ? "✅ ENABLED" : "❌ DISABLED")}");
            Console.WriteLine($"   Connector: {logicApps["connector_name"]}");
            Console.WriteLine($"   Actions: {((string[])logicApps["actions"]).Length}");
            Console.WriteLine($"   Triggers: {((string[])logicApps["triggers"]).Length}");

            Console.WriteLine($"\n🔐 Identity & Access:");
            Console.WriteLine($"   Type: {identity["type"]}");
            Console.WriteLine($"   Permissions: {((string[])identity["permissions"]).Length}");

            var logicAppDefinition = new Dictionary<string, object>
            {
                ["definition"] = new Dictionary<string, object>
                {
                    ["$schema"] = "https://schema.management.azure.com/providers/Microsoft.Logic/schemas/2016-06-01/workflowdefinition.json#",
                    ["actions"] = new Dictionary<string, object>
                    {
                        ["Query_Workplace_Assistant"] = new Dictionary<string, object>
                        {
                            ["type"] = "Http",
                            ["inputs"] = new Dictionary<string, object>
                            {
                                ["method"] = "POST",
                                ["uri"] = $"{endpoint["url"]}/api/query",
                                ["headers"] = new Dictionary<string, string>
                                {
                                    ["Content-Type"] = "application/json"
                                },
                                ["body"] = new Dictionary<string, string>
                                {
                                    ["question"] = "@triggerBody()['question']",
                                    ["conversation_id"] = "@triggerBody()['conversation_id']"
                                },
                                ["authentication"] = new Dictionary<string, string>
                                {
                                    ["type"] = "ManagedServiceIdentity"
                                }
                            }
                        }
                    },
                    ["triggers"] = new Dictionary<string, object>
                    {
                        ["manual"] = new Dictionary<string, object>
                        {
                            ["type"] = "Request",
                            ["kind"] = "Http",
                            ["inputs"] = new Dictionary<string, object>
                            {
                                ["schema"] = new Dictionary<string, object>
                                {
                                    ["type"] = "object",
                                    ["properties"] = new Dictionary<string, object>
                                    {
                                        ["question"] = new Dictionary<string, string> { ["type"] = "string" },
                                        ["conversation_id"] = new Dictionary<string, string> { ["type"] = "string" }
                                    }
                                }
                            }
                        }
                    }
                }
            };

            await File.WriteAllTextAsync("endpoint_config.json",
                JsonSerializer.Serialize(endpointConfig, new JsonSerializerOptions { WriteIndented = true }));
            await File.WriteAllTextAsync("logic_app_definition.json",
                JsonSerializer.Serialize(logicAppDefinition, new JsonSerializerOptions { WriteIndented = true }));

            Console.WriteLine($"\n✅ Production endpoint configured successfully");
            Console.WriteLine($"   📄 Configuration saved to: endpoint_config.json");
            Console.WriteLine($"   📄 Logic App definition: logic_app_definition.json");
            Console.WriteLine($"\n📚 Next Steps:");
            Console.WriteLine($"   1. Deploy agent to Container Apps");
            Console.WriteLine($"   2. Configure managed identity permissions");
            Console.WriteLine($"   3. Register connector in Logic Apps");
            Console.WriteLine($"   4. Submit to Azure Marketplace (optional)");

            return endpointConfig;
        }

        /// <summary>
        /// Generates a deployment script for Container Apps.
        /// </summary>
        /// <param name="agentName">Name of the agent</param>
        /// <param name="endpointConfig">Endpoint configuration</param>
        /// <returns>Deployment script content</returns>
        public string GenerateDeploymentScript(string agentName, Dictionary<string, object> endpointConfig)
        {
            var endpoint = (Dictionary<string, object>)endpointConfig["endpoint"];
            var identity = (Dictionary<string, object>)endpointConfig["identity"];

            var script = $@"#!/bin/bash
# Deployment script for {agentName}

# Variables
RESOURCE_GROUP=""your-resource-group""
LOCATION=""eastus""
CONTAINER_APP_NAME=""{agentName.ToLower().Replace(" ", "-")}""
CONTAINER_APP_ENV=""agent-env""
IMAGE=""mcr.microsoft.com/azure-ai/agents:latest""

# Create Container Apps environment
echo ""Creating Container Apps environment...""
az containerapp env create \
  --name $CONTAINER_APP_ENV \
  --resource-group $RESOURCE_GROUP \
  --location $LOCATION

# Create Container App with managed identity
echo ""Creating Container App...""
az containerapp create \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --environment $CONTAINER_APP_ENV \
  --image $IMAGE \
  --target-port 8080 \
  --ingress external \
  --system-assigned

# Get managed identity principal ID
PRINCIPAL_ID=$(az containerapp show \
  --name $CONTAINER_APP_NAME \
  --resource-group $RESOURCE_GROUP \
  --query identity.principalId -o tsv)

echo ""Managed Identity Principal ID: $PRINCIPAL_ID""

# Assign permissions (example for AI Foundry)
echo ""Assigning permissions...""
az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role ""Cognitive Services User"" \
  --scope /subscriptions/$SUBSCRIPTION_ID/resourceGroups/$RESOURCE_GROUP

echo ""Deployment complete!""
echo ""Endpoint URL: {endpoint["url"]}""
";

            return script;
        }

        /// <summary>
        /// Validates endpoint configuration for production readiness.
        /// </summary>
        /// <param name="endpointConfig">Endpoint configuration to validate</param>
        /// <returns>Validation results</returns>
        public Dictionary<string, object> ValidateEndpointConfiguration(Dictionary<string, object> endpointConfig)
        {
            var issues = new List<string>();
            var warnings = new List<string>();

            var endpoint = (Dictionary<string, object>)endpointConfig["endpoint"];
            var identity = (Dictionary<string, object>)endpointConfig["identity"];
            var apiManagement = (Dictionary<string, object>)endpointConfig["api_management"];

            // Check authentication
            if (endpoint["authentication"].ToString() != "managed_identity")
            {
                warnings.Add("Managed identity authentication is recommended for production");
            }

            // Check API management
            if (!(bool)apiManagement["enabled"])
            {
                warnings.Add("API Management should be enabled for production");
            }

            // Check rate limiting
            var rateLimiting = (Dictionary<string, object>)apiManagement["rate_limiting"];
            if ((int)(long)rateLimiting["requests_per_minute"] < 10)
            {
                issues.Add("Rate limit too restrictive for production use");
            }

            // Check CORS
            var cors = (Dictionary<string, object>)apiManagement["cors"];
            if (!(bool)cors["enabled"])
            {
                warnings.Add("CORS should be configured for web applications");
            }

            var isValid = issues.Count == 0;

            return new Dictionary<string, object>
            {
                ["is_valid"] = isValid,
                ["issues"] = issues,
                ["warnings"] = warnings,
                ["timestamp"] = DateTime.UtcNow.ToString("O")
            };
        }

        /// <summary>
        /// Generates API documentation for the production endpoint.
        /// </summary>
        /// <param name="agentName">Name of the agent</param>
        /// <param name="endpointConfig">Endpoint configuration</param>
        /// <returns>API documentation in markdown format</returns>
        public string GenerateAPIDocumentation(string agentName, Dictionary<string, object> endpointConfig)
        {
            var endpoint = (Dictionary<string, object>)endpointConfig["endpoint"];
            var logicApps = (Dictionary<string, object>)endpointConfig["logic_apps"];

            var doc = $@"# {agentName} API Documentation

## Overview
Production endpoint for {agentName} agent.

## Base URL
```
{endpoint["url"]}
```

## Authentication
Type: {endpoint["authentication"]}

Required Headers:
- `Authorization`: Bearer token or managed identity
- `Content-Type`: application/json

## Endpoints

### POST /api/query
Query the agent with a question.

**Request Body:**
```json
{{
  ""question"": ""Your question here"",
  ""conversation_id"": ""optional-conversation-id"",
  ""user_id"": ""optional-user-id""
}}
```

**Response:**
```json
{{
  ""response"": ""Agent's response"",
  ""conversation_id"": ""conversation-id"",
  ""status"": ""completed"",
  ""metadata"": {{
    ""latency_ms"": 1234,
    ""tokens_used"": 567
  }}
}}
```

### GET /health
Health check endpoint.

**Response:**
```json
{{
  ""status"": ""healthy"",
  ""timestamp"": ""2025-10-23T19:00:00Z""
}}
```

## Logic Apps Integration

### Available Actions
";

            var actions = (string[])logicApps["actions"];
            foreach (var action in actions)
            {
                doc += $"- `{action}`\n";
            }

            doc += "\n### Available Triggers\n";
            var triggers = (string[])logicApps["triggers"];
            foreach (var trigger in triggers)
            {
                doc += $"- `{trigger}`\n";
            }

            doc += @"

## Rate Limits
- 100 requests per minute
- 5000 requests per hour

## Error Codes
- `400`: Bad Request - Invalid input
- `401`: Unauthorized - Missing or invalid authentication
- `429`: Too Many Requests - Rate limit exceeded
- `500`: Internal Server Error - Agent processing error

## Support
For support and questions, contact: support@example.com
";

            return doc;
        }
    }
}
