"""
Production Deployment Module

This module demonstrates production deployment strategies for enterprise agents.
It includes:
- Azure Container Apps endpoint configuration
- Azure AI Agent Service marketplace deployment
- Azure Logic Apps integration
- Managed identity configuration
"""

import json
from typing import Dict, Any
from azure.ai.projects import AIProjectClient

class ProductionDeployment:
    """Handles production deployment configuration for enterprise agents."""
    
    def __init__(self, project_client: AIProjectClient, agent_id: str):
        """
        Initialize the ProductionDeployment handler.
        
        Args:
            project_client: Azure AI Projects client
            agent_id: ID of the agent to deploy
        """
        self.project_client = project_client
        self.agent_id = agent_id
    
    def configure_production_endpoints(self) -> Dict[str, Any]:
        """
        Configure production deployment endpoints and strategies.
        
        Returns:
            Production endpoint configuration dictionary
        """
        print("\n" + "="*80)
        print("8. Production Deployment Endpoints")
        print("="*80)
        
        # <endpoint_configuration>
        # NOTE: This is a conceptual snippet showing production endpoint patterns.
        # Actual implementation requires Azure Container Apps, API Management, etc.
        endpoint_config = {
            "agent_id": self.agent_id,
            "deployment_targets": {
                "container_apps": {
                    "enabled": True,
                    "configuration": {
                        "name": "enterprise-agent-prod",
                        "resource_group": "rg-agents-prod",
                        "location": "eastus",
                        "environment": "managed-environment-prod",
                        "ingress": {
                            "external": True,
                            "target_port": 8000,
                            "transport": "http",
                            "allow_insecure": False
                        },
                        "scale": {
                            "min_replicas": 2,
                            "max_replicas": 10,
                            "rules": [
                                {
                                    "name": "http-scale-rule",
                                    "http": {
                                        "concurrent_requests": 100
                                    }
                                }
                            ]
                        },
                        "identity": {
                            "type": "SystemAssigned"
                        }
                    }
                },
                "marketplace": {
                    "enabled": True,
                    "configuration": {
                        "listing_name": "Enterprise Workplace Assistant",
                        "description": "AI-powered assistant for enterprise workplace scenarios",
                        "categories": ["Productivity", "Business Intelligence"],
                        "pricing_model": "usage_based",
                        "api_endpoint": "https://enterprise-agent-prod.azurecontainerapps.io"
                    }
                },
                "logic_apps": {
                    "enabled": True,
                    "configuration": {
                        "workflow_name": "enterprise-agent-workflow",
                        "trigger_type": "http_request",
                        "actions": [
                            {
                                "name": "call_agent",
                                "type": "Http",
                                "inputs": {
                                    "method": "POST",
                                    "uri": "https://enterprise-agent-prod.azurecontainerapps.io/chat",
                                    "authentication": {
                                        "type": "ManagedServiceIdentity"
                                    }
                                }
                            }
                        ],
                        "integration_points": [
                            "SharePoint",
                            "Teams",
                            "Outlook",
                            "Dynamics 365"
                        ]
                    }
                }
            },
            "security": {
                "authentication": {
                    "type": "EntraID",
                    "tenant_id": "your-tenant-id",
                    "required_roles": ["Agent.User"]
                },
                "network": {
                    "vnet_integration": True,
                    "private_endpoints": True,
                    "allowed_ip_ranges": ["10.0.0.0/8"]
                },
                "data_protection": {
                    "encryption_at_rest": True,
                    "encryption_in_transit": True,
                    "customer_managed_keys": True
                }
            },
            "monitoring": {
                "application_insights": True,
                "log_analytics_workspace": True,
                "diagnostic_settings": [
                    "ContainerAppConsoleLogs",
                    "ContainerAppSystemLogs",
                    "AllMetrics"
                ]
            }
        }
        # </endpoint_configuration>
        
        print("\n✓ Production endpoint configuration created")
        print("\nDeployment Targets:")
        print("  • Azure Container Apps: Scalable, managed hosting")
        print("  • Azure AI Agent Service Marketplace: Public/private listing")
        print("  • Azure Logic Apps: Enterprise workflow integration")
        print("\nSecurity Features:")
        print("  • Entra ID authentication")
        print("  • VNet integration with private endpoints")
        print("  • Encryption at rest and in transit")
        print("  • Customer-managed keys support")
        print("\nScaling Configuration:")
        print("  • Min replicas: 2")
        print("  • Max replicas: 10")
        print("  • Auto-scale on HTTP concurrent requests")
        
        # Save endpoint configuration
        with open("endpoint_config.json", "w") as f:
            json.dump(endpoint_config, f, indent=2)
        print("\n✓ Endpoint configuration saved to: endpoint_config.json")
        
        return endpoint_config
    
    def generate_container_apps_bicep(self) -> str:
        """
        Generate Azure Container Apps Bicep template.
        
        Returns:
            Path to generated Bicep file
        """
        bicep_content = """// Azure Container Apps deployment for Enterprise Agent
param location string = resourceGroup().location
param environmentName string = 'managed-environment-prod'
param containerAppName string = 'enterprise-agent-prod'
param containerImage string = 'mcr.microsoft.com/azureml/agents:latest'

// Container Apps Environment
resource environment 'Microsoft.App/managedEnvironments@2023-05-01' = {
  name: environmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
    }
  }
}

// Container App
resource containerApp 'Microsoft.App/containerApps@2023-05-01' = {
  name: containerAppName
  location: location
  identity: {
    type: 'SystemAssigned'
  }
  properties: {
    environmentId: environment.id
    configuration: {
      ingress: {
        external: true
        targetPort: 8000
        transport: 'http'
        allowInsecure: false
      }
      secrets: [
        {
          name: 'azure-openai-key'
          value: 'your-key-here'
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'agent-container'
          image: containerImage
          resources: {
            cpu: json('1.0')
            memory: '2Gi'
          }
          env: [
            {
              name: 'AZURE_OPENAI_ENDPOINT'
              value: 'https://your-openai.openai.azure.com'
            }
            {
              name: 'AZURE_OPENAI_KEY'
              secretRef: 'azure-openai-key'
            }
          ]
        }
      ]
      scale: {
        minReplicas: 2
        maxReplicas: 10
        rules: [
          {
            name: 'http-scale-rule'
            http: {
              metadata: {
                concurrentRequests: '100'
              }
            }
          }
        ]
      }
    }
  }
}

output fqdn string = containerApp.properties.configuration.ingress.fqdn
output systemAssignedIdentity string = containerApp.identity.principalId
"""
        
        bicep_path = "container-app-deployment.bicep"
        with open(bicep_path, "w") as f:
            f.write(bicep_content)
        
        print(f"\n✓ Container Apps Bicep template generated: {bicep_path}")
        return bicep_path
    
    def generate_logic_app_definition(self) -> str:
        """
        Generate Azure Logic Apps workflow definition.
        
        Returns:
            Path to generated Logic App definition file
        """
        logic_app_content = {
            "definition": {
                "$schema": "https://schema.management.azure.com/providers/Microsoft.Logic/schemas/2016-06-01/workflowdefinition.json#",
                "contentVersion": "1.0.0.0",
                "parameters": {},
                "triggers": {
                    "manual": {
                        "type": "Request",
                        "kind": "Http",
                        "inputs": {
                            "schema": {
                                "type": "object",
                                "properties": {
                                    "message": {
                                        "type": "string"
                                    },
                                    "user_id": {
                                        "type": "string"
                                    }
                                }
                            }
                        }
                    }
                },
                "actions": {
                    "Call_Enterprise_Agent": {
                        "type": "Http",
                        "inputs": {
                            "method": "POST",
                            "uri": "https://enterprise-agent-prod.azurecontainerapps.io/chat",
                            "headers": {
                                "Content-Type": "application/json"
                            },
                            "body": {
                                "message": "@triggerBody()?['message']",
                                "user_id": "@triggerBody()?['user_id']"
                            },
                            "authentication": {
                                "type": "ManagedServiceIdentity"
                            }
                        },
                        "runAfter": {}
                    },
                    "Parse_Response": {
                        "type": "ParseJson",
                        "inputs": {
                            "content": "@body('Call_Enterprise_Agent')",
                            "schema": {
                                "type": "object",
                                "properties": {
                                    "response": {
                                        "type": "string"
                                    },
                                    "confidence": {
                                        "type": "number"
                                    }
                                }
                            }
                        },
                        "runAfter": {
                            "Call_Enterprise_Agent": ["Succeeded"]
                        }
                    },
                    "Send_Response": {
                        "type": "Response",
                        "inputs": {
                            "statusCode": 200,
                            "body": "@body('Parse_Response')"
                        },
                        "runAfter": {
                            "Parse_Response": ["Succeeded"]
                        }
                    }
                }
            }
        }
        
        logic_app_path = "logic-app-workflow.json"
        with open(logic_app_path, "w") as f:
            json.dump(logic_app_content, f, indent=2)
        
        print(f"\n✓ Logic App workflow definition generated: {logic_app_path}")
        return logic_app_path
    
    def generate_marketplace_manifest(self) -> str:
        """
        Generate Azure Marketplace listing manifest.
        
        Returns:
            Path to generated manifest file
        """
        manifest_content = {
            "publisher": "your-company",
            "offer_id": "enterprise-workplace-assistant",
            "plan_id": "standard",
            "version": "1.0.0",
            "metadata": {
                "display_name": "Enterprise Workplace Assistant",
                "summary": "AI-powered assistant for enterprise workplace scenarios",
                "description": "A comprehensive AI agent that helps employees with workplace tasks including document analysis, meeting scheduling, and policy inquiries.",
                "categories": ["Productivity", "Business Intelligence", "AI + Machine Learning"],
                "industries": ["Financial Services", "Healthcare", "Retail", "Manufacturing"],
                "keywords": ["agent", "ai", "workplace", "assistant", "productivity"]
            },
            "pricing": {
                "model": "usage_based",
                "unit": "api_call",
                "price_per_unit": 0.01,
                "free_tier": {
                    "enabled": True,
                    "limit": 1000
                }
            },
            "technical": {
                "api_endpoint": "https://enterprise-agent-prod.azurecontainerapps.io",
                "authentication": "EntraID",
                "api_version": "2024-01-01",
                "openapi_spec_url": "https://enterprise-agent-prod.azurecontainerapps.io/openapi.json"
            },
            "support": {
                "contact_email": "support@your-company.com",
                "documentation_url": "https://docs.your-company.com/enterprise-agent",
                "support_url": "https://support.your-company.com"
            }
        }
        
        manifest_path = "marketplace-manifest.json"
        with open(manifest_path, "w") as f:
            json.dump(manifest_content, f, indent=2)
        
        print(f"\n✓ Marketplace manifest generated: {manifest_path}")
        return manifest_path
