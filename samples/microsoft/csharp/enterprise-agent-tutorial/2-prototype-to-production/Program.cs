using System;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;

namespace Microsoft.Azure.Samples.PrototypeToProduction
{
    /*
     * Azure AI Foundry Agent Sample - Tutorial 2: Prototype to Production
     * 
     * This sample builds on Tutorial 1, demonstrating enterprise-ready agent deployment with:
     * - Safety assessment and red-teaming
     * - Evaluation datasets and baseline metrics
     * - Organization-wide governance policies
     * - Model comparison and iteration
     * - Fleet monitoring and observability
     * - CI/CD integration for offline evaluation
     * - Production endpoint deployment
     * 
     * Educational Focus:
     * - Enterprise safety and governance patterns
     * - Production-ready evaluation frameworks
     * - Multi-agent fleet management
     * - Continuous integration and deployment
     * - External consumption through endpoints
     */

    class Program
    {
        static async Task Main(string[] args)
        {
            Console.WriteLine("=" + new string('=', 69));
            Console.WriteLine("🚀 Azure AI Foundry - Prototype to Production");
            Console.WriteLine("Tutorial 2: Enterprise Agent Deployment");
            Console.WriteLine("=" + new string('=', 69));
            Console.WriteLine("\nThis tutorial demonstrates:");
            Console.WriteLine("• Safety assessment and red-teaming");
            Console.WriteLine("• Evaluation datasets and baseline metrics");
            Console.WriteLine("• Organization-wide governance policies");
            Console.WriteLine("• Model iteration and comparison");
            Console.WriteLine("• Fleet monitoring and observability");
            Console.WriteLine("• CI/CD integration for offline evaluation");
            Console.WriteLine("• Production endpoint deployment");
            Console.WriteLine("=" + new string('=', 69));

            try
            {
                // Step 1: Create and initialize agent
                var agentCreation = new AgentCreation();
                var (agent, client, openAIClient, modelDeploymentName) = await agentCreation.CreateProductionReadyAgentAsync();
                var agentName = agent.Name;

                // Step 2: Initialize all modules
                var safetyAssessment = new SafetyAssessment(client, openAIClient, modelDeploymentName);
                var evaluationFramework = new EvaluationFramework(client, openAIClient, modelDeploymentName);
                var governancePolicies = new GovernancePolicies();
                var monitoringCICD = new MonitoringCICD(client, openAIClient, modelDeploymentName);
                var productionDeployment = new ProductionDeployment();

                // Execute all production readiness steps
                Helpers.PrintSectionHeader("🎯 PRODUCTION READINESS WORKFLOW");

                // 1. Safety Assessment
                var safetyResults = await safetyAssessment.AssessAgentSafetyAsync(agentName);

                // 2. Create Evaluation Dataset and Run Baseline
                var datasetFile = await evaluationFramework.CreateEvaluationDatasetAsync();
                var evalResults = await evaluationFramework.RunBaselineEvaluationAsync(agentName, datasetFile);

                // 3. Apply Governance Policies
                var governanceResults = await governancePolicies.ApplyGovernancePoliciesAsync(agentName);

                // 4. Compare Model Versions
                var comparisonResults = await monitoringCICD.CompareModelVersionsAsync(agentName);

                // 5. Setup Fleet Monitoring
                var monitoringResults = await monitoringCICD.SetupFleetMonitoringAsync(new[] { agentName });

                // 6. Create CI/CD Configuration
                var cicdResults = await monitoringCICD.CreateCICDEvaluationConfigAsync();

                // 7. Create Production Endpoint
                var endpointResults = await productionDeployment.CreateProductionEndpointAsync(agentName);

                // Summary
                Helpers.PrintSectionHeader("🎉 TUTORIAL 2 COMPLETE - PRODUCTION READY!");
                
                Console.WriteLine($"\n📊 Summary:");
                Console.WriteLine($"   Safety Score: {safetyResults["safety_score"]:F1}%");
                Console.WriteLine($"   Evaluation Pass Rate: {evalResults["pass_rate"]:F1}%");
                Console.WriteLine($"   Governance: {governanceResults["compliance_status"]}");
                Console.WriteLine($"   Monitoring: Configured for {monitoringResults["fleet_size"]} agent(s)");
                Console.WriteLine($"   CI/CD: {((JsonElement)cicdResults["stages"]).GetArrayLength()} pipeline stages");
                Console.WriteLine($"   Endpoint: {((JsonElement)endpointResults["endpoint"])["url"]}");

                Console.WriteLine($"\n📄 Artifacts Created:");
                var artifacts = new[] {
                    "safety_assessment.json",
                    "evaluation_dataset.jsonl",
                    "baseline_evaluation.json",
                    "governance_policies.json",
                    "model_comparison.json",
                    "fleet_monitoring.json",
                    "cicd_config.json",
                    ".github-workflows-agent-evaluation.yml",
                    "azure-pipelines.yml",
                    "endpoint_config.json",
                    "logic_app_definition.json"
                };
                foreach (var artifact in artifacts)
                {
                    Helpers.PrintSuccess(artifact);
                }

                Console.WriteLine($"\n🔗 Next Steps:");
                Console.WriteLine($"   1. Review safety_assessment.json and address any concerns");
                Console.WriteLine($"   2. Integrate CI/CD pipelines into your DevOps workflow");
                Console.WriteLine($"   3. Deploy agent to Container Apps using endpoint_config.json");
                Console.WriteLine($"   4. Configure monitoring dashboards in Azure Portal");
                Console.WriteLine($"   5. Move to Tutorial 3 for advanced production scenarios");
            }
            catch (Exception ex)
            {
                Helpers.PrintError($"An error occurred: {ex.Message}");
                Console.WriteLine($"\nStack trace:\n{ex.StackTrace}");
                Environment.Exit(1);
            }
        }
    }
}
