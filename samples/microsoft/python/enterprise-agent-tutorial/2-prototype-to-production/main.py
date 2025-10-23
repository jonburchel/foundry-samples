"""
Enterprise Agent Tutorial: From Prototype to Production

This tutorial demonstrates how to transform a prototype AI agent into a production-ready
enterprise solution. It covers eight critical production features:

1. Agent Creation & Authentication
2. Safety Assessment with Red-Teaming
3. Evaluation Framework with Dataset
4. Governance Policies & Model Comparison
5. Fleet Monitoring Configuration
6. CI/CD Integration with Quality Gates
7. Production Deployment Endpoints

The code is organized into modular components for better maintainability and follows
enterprise best practices for AI agent development.
"""

import os
from dotenv import load_dotenv

# Import modular components
from agent_creation import create_enterprise_agent
from safety_assessment import SafetyAssessment
from evaluation_framework import EvaluationFramework
from governance_policies import GovernancePolicies
from monitoring_cicd import MonitoringCICD
from production_deployment import ProductionDeployment
import helpers

def main():
    """
    Main execution flow for the enterprise agent production pipeline.
    """
    # Load environment variables
    load_dotenv()
    
    # Display tutorial header
    helpers.print_header("Enterprise Agent Tutorial: Prototype to Production")
    print("\nThis tutorial demonstrates production-ready features for enterprise AI agents:")
    helpers.print_list_items([
        "Safety Assessment & Red-Teaming",
        "Evaluation Framework with Datasets",
        "Governance Policies & Model Comparison",
        "Fleet Monitoring Configuration",
        "CI/CD Integration with Quality Gates",
        "Production Deployment Endpoints"
    ])
    
    try:
        # Step 1: Create the enterprise agent
        helpers.print_header("Step 1: Agent Creation & Authentication")
        project_client, agent = create_enterprise_agent()
        agent_id = agent.id
        helpers.print_success(f"Enterprise agent created successfully with ID: {agent_id}")
        
        # Step 2: Safety Assessment with Red-Teaming
        helpers.print_header("Step 2: Safety Assessment & Red-Teaming")
        safety = SafetyAssessment(project_client, agent_id)
        
        helpers.print_info("Running red-team scenarios...")
        red_team_results = safety.run_red_team_scenarios()
        
        helpers.print_info("Evaluating safety responses...")
        safety_results = safety.evaluate_safety_responses(red_team_results)
        helpers.print_safety_summary(safety_results)
        
        # Step 3: Evaluation Framework
        helpers.print_header("Step 3: Evaluation Framework")
        evaluation = EvaluationFramework(project_client, agent_id)
        
        helpers.print_info("Creating evaluation dataset...")
        dataset = evaluation.create_evaluation_dataset()
        helpers.print_success(f"Created dataset with {len(dataset['questions'])} questions")
        
        helpers.print_info("Running evaluation...")
        eval_results = evaluation.run_evaluation(dataset)
        helpers.print_evaluation_summary(eval_results)
        
        # Step 4: Governance Policies & Model Comparison
        helpers.print_header("Step 4: Governance Policies")
        governance = GovernancePolicies(project_client, agent_id)
        
        helpers.print_info("Applying governance policies...")
        policy_results = governance.apply_governance_policies()
        helpers.print_success("Governance policies applied successfully")
        
        helpers.print_info("Comparing model versions...")
        comparison_results = governance.compare_model_versions()
        helpers.print_model_comparison(comparison_results)
        
        # Step 5: Fleet Monitoring
        helpers.print_header("Step 5: Fleet Monitoring Configuration")
        monitoring = MonitoringCICD(project_client, agent_id)
        
        helpers.print_info("Setting up fleet monitoring...")
        monitoring_config = monitoring.setup_fleet_monitoring()
        helpers.print_success("Monitoring configuration created")
        
        # Step 6: CI/CD Integration
        helpers.print_header("Step 6: CI/CD Integration")
        
        helpers.print_info("Creating CI/CD configuration...")
        cicd_config = monitoring.create_cicd_evaluation_config()
        helpers.print_success("CI/CD pipeline configuration created")
        
        helpers.print_info("Generating GitHub Actions workflow...")
        monitoring.generate_github_actions_workflow()
        
        helpers.print_info("Generating Azure DevOps pipeline...")
        monitoring.generate_azure_devops_pipeline()
        
        # Step 7: Production Deployment
        helpers.print_header("Step 7: Production Deployment")
        deployment = ProductionDeployment(project_client, agent_id)
        
        helpers.print_info("Configuring production endpoints...")
        endpoint_config = deployment.configure_production_endpoints()
        helpers.print_success("Production endpoint configuration created")
        
        helpers.print_info("Generating Container Apps Bicep template...")
        deployment.generate_container_apps_bicep()
        
        helpers.print_info("Generating Logic App workflow...")
        deployment.generate_logic_app_definition()
        
        helpers.print_info("Generating Marketplace manifest...")
        deployment.generate_marketplace_manifest()
        
        # Summary
        helpers.print_header("Tutorial Completion Summary")
        print("\n✓ All production features configured successfully!\n")
        
        print("Generated Artifacts:")
        helpers.print_list_items([
            "red_team_results.json - Safety assessment results",
            "safety_evaluation.json - Safety evaluation metrics",
            "evaluation_dataset.json - Evaluation questions dataset",
            "evaluation_results.json - Evaluation performance metrics",
            "governance_policies.json - Applied governance policies",
            "model_comparison.json - Model version comparison",
            "monitoring_config.json - Fleet monitoring configuration",
            "cicd_config.json - CI/CD pipeline configuration",
            ".github/workflows/agent-cicd.yml - GitHub Actions workflow",
            "azure-pipelines.yml - Azure DevOps pipeline",
            "endpoint_config.json - Production endpoint configuration",
            "container-app-deployment.bicep - Container Apps template",
            "logic-app-workflow.json - Logic Apps workflow",
            "marketplace-manifest.json - Marketplace listing"
        ])
        
        print("\nNext Steps:")
        helpers.print_list_items([
            "Review safety assessment results and address any concerns",
            "Validate evaluation metrics meet your quality thresholds",
            "Customize governance policies for your organization",
            "Configure monitoring dashboards in Azure Portal",
            "Set up CI/CD pipelines in your repository",
            "Deploy to Container Apps or Azure AI Agent Service",
            "Enable production monitoring and alerting"
        ])
        
        print("\nProduction Readiness Checklist:")
        checklist_items = [
            ("Safety Evaluation", safety_results.get("passed", False)),
            ("Quality Evaluation", eval_results.get("passed", False)),
            ("Governance Policies", True),
            ("Monitoring Setup", True),
            ("CI/CD Configuration", True),
            ("Deployment Ready", True)
        ]
        
        for item, status in checklist_items:
            status_icon = "✓" if status else "✗"
            print(f"  {status_icon} {item}")
        
        all_passed = all(status for _, status in checklist_items)
        
        if all_passed:
            helpers.print_success("\nAgent is ready for production deployment! 🚀")
        else:
            helpers.print_warning("\nPlease address failed checks before production deployment.")
        
    except Exception as e:
        helpers.print_error(f"An error occurred: {str(e)}")
        raise

if __name__ == "__main__":
    main()
