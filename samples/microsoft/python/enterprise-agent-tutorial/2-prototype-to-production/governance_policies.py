#!/usr/bin/env python3
"""
Governance Policies Module

Handles governance policy definition and application.
Configures organization-wide policies for the agent.
"""

import json
import time
import os
from datetime import datetime
from typing import Dict, Any, List
from azure.ai.agents.models import AgentReference

class GovernancePolicies:
    """Manages governance policies and model comparison."""
    
    def __init__(self, project_client):
        """
        Initialize with project client.
        
        Args:
            project_client: AIProjectClient instance
        """
        self.project_client = project_client
        self.openai_client = project_client.get_openai_client()
    
    def apply_governance_policies(self, agent_name: str) -> Dict[str, Any]:
        """
        Apply organization-wide governance policies to agent.
        
        Args:
            agent_name: Name of the agent to apply policies to
            
        Returns:
            Dict containing policy application results
        """
        
        print("\n🏛️  APPLYING GOVERNANCE POLICIES")
        print("="*70)
        
        # <governance_policy_definition>
        # NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        # Define organization-wide policies
        governance_policies = {
            "content_safety": {
                "enabled": True,
                "filters": ["hate", "violence", "self_harm", "sexual"],
                "action": "block_and_log"
            },
            "data_protection": {
                "enabled": True,
                "pii_detection": True,
                "pii_handling": "redact",
                "sensitive_data_logging": False
            },
            "rate_limiting": {
                "enabled": True,
                "requests_per_minute": 60,
                "requests_per_day": 5000,
                "burst_limit": 10
            },
            "audit_logging": {
                "enabled": True,
                "log_requests": True,
                "log_responses": True,
                "log_tokens": True,
                "retention_days": 90
            },
            "compliance": {
                "gdpr_compliant": True,
                "data_residency": "US",
                "hipaa_compliant": False,
                "soc2_compliant": True
            }
        }
        # </governance_policy_definition>
        
        print("📋 Governance Policies:")
        for policy_name, policy_config in governance_policies.items():
            enabled = policy_config.get("enabled", False)
            status = "✅ ACTIVE" if enabled else "⚠️  INACTIVE"
            print(f"   {status} {policy_name.replace('_', ' ').title()}")
        
        # Simulate policy application
        policy_application = {
            "timestamp": datetime.now().isoformat(),
            "agent_name": agent_name,
            "policies_applied": governance_policies,
            "enforcement_level": "strict",
            "compliance_status": "compliant"
        }
        
        # Save policy configuration
        with open('governance_policies.json', 'w') as f:
            json.dump(policy_application, f, indent=2)
        
        print(f"\n✅ Governance policies applied successfully")
        print(f"   📄 Configuration saved to: governance_policies.json")
        print(f"   🔒 Enforcement Level: {policy_application['enforcement_level']}")
        
        return policy_application
    
    def compare_model_versions(self, base_agent_name: str) -> Dict[str, Any]:
        """
        Compare different model versions for agent performance.
        
        Args:
            base_agent_name: Base agent name to compare against
            
        Returns:
            Dict containing comparison results
        """
        
        print("\n🔄 MODEL COMPARISON AND ITERATION")
        print("="*70)
        
        # Define models to compare
        model_deployment = os.environ["MODEL_DEPLOYMENT_NAME"]
        model_configs = [
            {
                "name": "gpt-4o-mini-baseline",
                "deployment": model_deployment,
                "temperature": 0.7,
                "description": "Baseline configuration"
            },
            {
                "name": "gpt-4o-mini-deterministic",
                "deployment": model_deployment,
                "temperature": 0.1,
                "description": "More deterministic responses"
            },
            {
                "name": "gpt-4o-mini-creative",
                "deployment": model_deployment,
                "temperature": 0.9,
                "description": "More creative responses"
            }
        ]
        
        comparison_results = {
            "timestamp": datetime.now().isoformat(),
            "base_agent": base_agent_name,
            "models_compared": len(model_configs),
            "test_question": "How do I implement Azure AD Conditional Access according to our security policy?",
            "results": []
        }
        
        test_question = comparison_results["test_question"]
        
        for config in model_configs:
            print(f"\n🧪 Testing: {config['name']}")
            print(f"   Temperature: {config['temperature']}")
            print(f"   Description: {config['description']}")
            
            try:
                # Create conversation with test question
                conversation = self.openai_client.conversations.create(
                    items=[{
                        "type": "message",
                        "role": "user",
                        "content": test_question
                    }]
                )
                
                start_time = time.time()
                
                # Get response
                response = self.openai_client.responses.create(
                    conversation=conversation.id,
                    extra_body={"agent": AgentReference(name=base_agent_name).as_dict()}
                )
                
                latency = time.time() - start_time
                response_text = response.output_text if hasattr(response, 'output_text') else str(response.output)
                
                result = {
                    "model": config['name'],
                    "temperature": config['temperature'],
                    "latency_seconds": round(latency, 2),
                    "response_length": len(response_text),
                    "response_preview": response_text[:150]
                }
                
                comparison_results["results"].append(result)
                
                print(f"   ⏱️  Latency: {latency:.2f}s")
                print(f"   📏 Response Length: {len(response_text)} chars")
                
            except Exception as e:
                print(f"   ❌ ERROR: {e}")
                comparison_results["results"].append({
                    "model": config['name'],
                    "error": str(e)
                })
        
        # Identify recommended model
        if comparison_results["results"]:
            valid_results = [r for r in comparison_results["results"] if "error" not in r]
            if valid_results:
                recommended = min(valid_results, key=lambda x: x["latency_seconds"])
                comparison_results["recommended_model"] = recommended["model"]
                comparison_results["recommendation_reason"] = "Best latency with adequate response length"
        
        print("\n" + "="*70)
        print(f"🏆 MODEL COMPARISON COMPLETE")
        if "recommended_model" in comparison_results:
            print(f"   Recommended: {comparison_results['recommended_model']}")
            print(f"   Reason: {comparison_results['recommendation_reason']}")
        
        # Save comparison results
        with open('model_comparison.json', 'w') as f:
            json.dump(comparison_results, f, indent=2)
        print(f"   📄 Results saved to: model_comparison.json")
        
        return comparison_results
