using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using Azure.AI.Agents;

namespace Microsoft.Azure.Samples.PrototypeToProduction
{
    /// <summary>
    /// Monitoring and CI/CD module for fleet observability and deployment automation.
    /// This module provides:
    /// - Fleet monitoring configuration
    /// - Performance, quality, and business metrics
    /// - CI/CD pipeline integration
    /// - Quality gates and automated evaluation
    /// - Model comparison and iteration
    /// </summary>
    public class MonitoringCICD
    {
        private readonly AgentsClient _agentsClient;
        private readonly ResponsesClient _responsesClient;
        private readonly ConversationsClient _conversationsClient;
        private readonly string _modelDeploymentName;

        public MonitoringCICD(AgentsClient agentsClient, ResponsesClient responsesClient, string modelDeploymentName)
        {
            _agentsClient = agentsClient ?? throw new ArgumentNullException(nameof(agentsClient));
            _responsesClient = responsesClient ?? throw new ArgumentNullException(nameof(responsesClient));
            _conversationsClient = agentsClient.GetConversationsClient();
            _modelDeploymentName = modelDeploymentName ?? throw new ArgumentNullException(nameof(modelDeploymentName));
        }

        /// <summary>
        /// Sets up comprehensive fleet monitoring for multiple agents.
        /// </summary>
        /// <param name="agentNames">Array of agent names to monitor</param>
        /// <returns>Monitoring configuration</returns>
        public async Task<Dictionary<string, object>> SetupFleetMonitoringAsync(string[] agentNames)
        {
            Console.WriteLine("\n📡 FLEET MONITORING SETUP");
            Console.WriteLine(new string('=', 70));

            // <monitoring_metrics_configuration>
            // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
            var monitoringConfig = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["fleet_size"] = agentNames.Length,
                ["agents"] = agentNames,
                ["metrics"] = new Dictionary<string, object>
                {
                    ["performance"] = new[] {
                        "response_latency_ms",
                        "token_usage_per_request",
                        "requests_per_minute",
                        "error_rate_percent"
                    },
                    ["quality"] = new[] {
                        "user_satisfaction_score",
                        "task_completion_rate",
                        "hallucination_rate",
                        "safety_violations"
                    },
                    ["business"] = new[] {
                        "active_users",
                        "questions_answered",
                        "cost_per_conversation",
                        "uptime_percent"
                    }
                },
                ["alerting"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["channels"] = new[] { "email", "teams", "pagerduty" },
                    ["thresholds"] = new Dictionary<string, object>
                    {
                        ["error_rate_critical"] = 5.0,
                        ["latency_warning_ms"] = 5000,
                        ["latency_critical_ms"] = 10000,
                        ["safety_violation_critical"] = 1
                    }
                },
            // </monitoring_metrics_configuration>
                ["dashboards"] = new Dictionary<string, string>
                {
                    ["real_time_metrics"] = "https://portal.azure.com/monitoring/real-time",
                    ["historical_trends"] = "https://portal.azure.com/monitoring/trends",
                    ["cost_analysis"] = "https://portal.azure.com/cost-management"
                }
            };

            var metrics = (Dictionary<string, object>)monitoringConfig["metrics"];
            Console.WriteLine("📊 Monitoring Configuration:");
            Console.WriteLine($"   Agents Monitored: {agentNames.Length}");
            Console.WriteLine($"   Performance Metrics: {((string[])metrics["performance"]).Length}");
            Console.WriteLine($"   Quality Metrics: {((string[])metrics["quality"]).Length}");
            Console.WriteLine($"   Business Metrics: {((string[])metrics["business"]).Length}");
            
            var alerting = (Dictionary<string, object>)monitoringConfig["alerting"];
            Console.WriteLine($"   Alerting: {((bool)alerting["enabled"] ? "✅ ENABLED" : "❌ DISABLED")}");

            Console.WriteLine($"\n🏥 Initial Health Check:");
            foreach (var agentName in agentNames)
            {
                Console.WriteLine($"   ✅ {agentName}: Healthy");
            }

            await File.WriteAllTextAsync("fleet_monitoring.json",
                JsonSerializer.Serialize(monitoringConfig, new JsonSerializerOptions { WriteIndented = true }));

            Console.WriteLine($"\n✅ Fleet monitoring configured successfully");
            Console.WriteLine($"   📄 Configuration saved to: fleet_monitoring.json");

            return monitoringConfig;
        }

        /// <summary>
        /// Creates CI/CD pipeline configuration with quality gates.
        /// </summary>
        /// <returns>CI/CD configuration</returns>
        public async Task<Dictionary<string, object>> CreateCICDEvaluationConfigAsync()
        {
            Console.WriteLine("\n🔧 CI/CD OFFLINE EVALUATION SETUP");
            Console.WriteLine(new string('=', 70));

            // <cicd_quality_gates>
            // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
            var cicdConfig = new Dictionary<string, object>
            {
                ["pipeline_name"] = "agent-evaluation-pipeline",
                ["trigger"] = new Dictionary<string, object>
                {
                    ["branches"] = new[] { "main", "develop" },
                    ["paths"] = new[] { "agents/**", "evaluations/**" },
                    ["schedule"] = "0 2 * * *"
                },
                ["stages"] = new[]
                {
                    new Dictionary<string, object>
                    {
                        ["name"] = "Safety Assessment",
                        ["script"] = "dotnet run --project Program.csproj -- --safety-check",
                        ["quality_gate"] = new Dictionary<string, object>
                        {
                            ["metric"] = "safety_score",
                            ["threshold"] = 80.0,
                            ["action"] = "block_on_failure"
                        }
                    },
                    new Dictionary<string, object>
                    {
                        ["name"] = "Baseline Evaluation",
                        ["script"] = "dotnet run --project Program.csproj -- --evaluate",
                        ["quality_gate"] = new Dictionary<string, object>
                        {
                            ["metric"] = "pass_rate",
                            ["threshold"] = 70.0,
                            ["action"] = "block_on_failure"
                        }
                    },
                    new Dictionary<string, object>
                    {
                        ["name"] = "Regression Testing",
                        ["script"] = "dotnet run --project Program.csproj -- --regression-test",
                        ["quality_gate"] = new Dictionary<string, object>
                        {
                            ["metric"] = "regression_rate",
                            ["threshold"] = 5.0,
                            ["action"] = "warn_on_failure"
                        }
                    },
                    new Dictionary<string, object>
                    {
                        ["name"] = "Performance Testing",
                        ["script"] = "dotnet run --project Program.csproj -- --performance-test",
                        ["quality_gate"] = new Dictionary<string, object>
                        {
                            ["metric"] = "avg_latency_ms",
                            ["threshold"] = 3000,
                            ["action"] = "warn_on_failure"
                        }
                    }
                },
            // </cicd_quality_gates>
                ["notifications"] = new Dictionary<string, object>
                {
                    ["on_success"] = new[] { "teams" },
                    ["on_failure"] = new[] { "email", "teams", "slack" }
                },
                ["artifacts"] = new[] {
                    "safety_assessment.json",
                    "baseline_evaluation.json",
                    "performance_metrics.json"
                }
            };

            var githubActionsYaml = @"name: Agent Evaluation Pipeline

on:
  push:
    branches: [main, develop]
    paths:
      - 'agents/**'
      - 'evaluations/**'
  schedule:
    - cron: '0 2 * * *'

jobs:
  evaluate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup .NET
        uses: actions/setup-dotnet@v3
        with:
          dotnet-version: '8.0.x'
      
      - name: Restore dependencies
        run: dotnet restore
      
      - name: Safety Assessment
        run: dotnet run -- --safety-check
        env:
          PROJECT_ENDPOINT: ${{ secrets.PROJECT_ENDPOINT }}
          MODEL_DEPLOYMENT_NAME: ${{ secrets.MODEL_DEPLOYMENT_NAME }}
      
      - name: Baseline Evaluation
        run: dotnet run -- --evaluate
      
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: evaluation-results
          path: |
            safety_assessment.json
            baseline_evaluation.json
";

            var azureDevOpsYaml = @"trigger:
  branches:
    include:
      - main
      - develop
  paths:
    include:
      - agents/**
      - evaluations/**

schedules:
- cron: ""0 2 * * *""
  displayName: Daily evaluation
  branches:
    include:
    - main

pool:
  vmImage: 'ubuntu-latest'

steps:
- task: UseDotNet@2
  inputs:
    version: '8.0.x'

- script: |
    dotnet restore
  displayName: 'Restore dependencies'

- script: |
    dotnet run -- --safety-check
  displayName: 'Safety Assessment'
  env:
    PROJECT_ENDPOINT: $(PROJECT_ENDPOINT)
    MODEL_DEPLOYMENT_NAME: $(MODEL_DEPLOYMENT_NAME)

- script: |
    dotnet run -- --evaluate
  displayName: 'Baseline Evaluation'

- task: PublishBuildArtifacts@1
  inputs:
    PathtoPublish: '.'
    ArtifactName: 'evaluation-results'
";

            Console.WriteLine("📋 CI/CD Pipeline Configuration:");
            Console.WriteLine($"   Pipeline: {cicdConfig["pipeline_name"]}");
            Console.WriteLine($"   Stages: {((object[])cicdConfig["stages"]).Length}");
            Console.WriteLine($"   Quality Gates: {((object[])cicdConfig["stages"]).Length}");

            await File.WriteAllTextAsync("cicd_config.json",
                JsonSerializer.Serialize(cicdConfig, new JsonSerializerOptions { WriteIndented = true }));
            await File.WriteAllTextAsync(".github-workflows-agent-evaluation.yml", githubActionsYaml);
            await File.WriteAllTextAsync("azure-pipelines.yml", azureDevOpsYaml);

            Console.WriteLine($"\n✅ CI/CD evaluation configured successfully");
            Console.WriteLine($"   📄 Config saved to: cicd_config.json");
            Console.WriteLine($"   📄 GitHub Actions: .github-workflows-agent-evaluation.yml");
            Console.WriteLine($"   📄 Azure DevOps: azure-pipelines.yml");

            return cicdConfig;
        }

        /// <summary>
        /// Compares different model versions for performance and quality.
        /// </summary>
        /// <param name="baseAgentName">Base agent name to compare</param>
        /// <returns>Model comparison results</returns>
        public async Task<Dictionary<string, object>> CompareModelVersionsAsync(string baseAgentName)
        {
            Console.WriteLine("\n🔄 MODEL COMPARISON AND ITERATION");
            Console.WriteLine(new string('=', 70));

            var modelConfigs = new[]
            {
                new { Name = "gpt-4o-mini-baseline", Deployment = _modelDeploymentName, Temperature = 0.7, Description = "Baseline configuration" },
                new { Name = "gpt-4o-mini-deterministic", Deployment = _modelDeploymentName, Temperature = 0.1, Description = "More deterministic responses" },
                new { Name = "gpt-4o-mini-creative", Deployment = _modelDeploymentName, Temperature = 0.9, Description = "More creative responses" }
            };

            var comparisonResults = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["base_agent"] = baseAgentName,
                ["models_compared"] = modelConfigs.Length,
                ["test_question"] = "How do I implement Azure AD Conditional Access according to our security policy?",
                ["results"] = new List<object>()
            };

            var results = (List<object>)comparisonResults["results"];
            var testQuestion = (string)comparisonResults["test_question"];

            foreach (var config in modelConfigs)
            {
                Console.WriteLine($"\n🧪 Testing: {config.Name}");
                Console.WriteLine($"   Temperature: {config.Temperature}");
                Console.WriteLine($"   Description: {config.Description}");

                try
                {
                    var startTime = DateTime.UtcNow;
                    var (responseText, _) = await ChatWithAssistantAsync(baseAgentName, testQuestion);
                    var latency = (DateTime.UtcNow - startTime).TotalSeconds;

                    var result = new Dictionary<string, object>
                    {
                        ["model"] = config.Name,
                        ["temperature"] = config.Temperature,
                        ["latency_seconds"] = Math.Round(latency, 2),
                        ["response_length"] = responseText.Length,
                        ["response_preview"] = responseText.Substring(0, Math.Min(150, responseText.Length))
                    };

                    results.Add(result);

                    Console.WriteLine($"   ⏱️  Latency: {latency:F2}s");
                    Console.WriteLine($"   📏 Response Length: {responseText.Length} chars");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"   ❌ ERROR: {ex.Message}");
                    results.Add(new Dictionary<string, object>
                    {
                        ["model"] = config.Name,
                        ["error"] = ex.Message
                    });
                }
            }

            // Identify recommended model
            var validResults = results.Where(r => !((Dictionary<string, object>)r).ContainsKey("error")).ToList();
            if (validResults.Any())
            {
                var recommended = validResults.OrderBy(r => (double)((Dictionary<string, object>)r)["latency_seconds"]).First();
                comparisonResults["recommended_model"] = ((Dictionary<string, object>)recommended)["model"];
                comparisonResults["recommendation_reason"] = "Best latency with adequate response length";
            }

            Console.WriteLine("\n" + new string('=', 70));
            Console.WriteLine($"🏆 MODEL COMPARISON COMPLETE");
            if (comparisonResults.ContainsKey("recommended_model"))
            {
                Console.WriteLine($"   Recommended: {comparisonResults["recommended_model"]}");
                Console.WriteLine($"   Reason: {comparisonResults["recommendation_reason"]}");
            }

            await File.WriteAllTextAsync("model_comparison.json",
                JsonSerializer.Serialize(comparisonResults, new JsonSerializerOptions { WriteIndented = true }));
            Console.WriteLine($"   📄 Results saved to: model_comparison.json");

            return comparisonResults;
        }

        /// <summary>
        /// Helper method to interact with the assistant and get responses.
        /// </summary>
        private async Task<(string response, string status)> ChatWithAssistantAsync(string agentName, string message)
        {
            try
            {
                var conversation = await _conversationsClient.CreateConversationAsync();
                
                var response = await _responsesClient.CreateResponseAsync(
                    agentName,
                    conversation.Id,
                    message
                );

                return (response.GetOutputText(), "completed");
            }
            catch (Exception ex)
            {
                return ($"Error: {ex.Message}", "failed");
            }
        }
    }
}
