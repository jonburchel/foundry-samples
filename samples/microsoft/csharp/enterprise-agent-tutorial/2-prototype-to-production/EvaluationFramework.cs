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
    /// Evaluation Framework module for testing agent quality and performance.
    /// This module provides:
    /// - Evaluation dataset creation and management
    /// - Baseline quality metrics calculation
    /// - Topic coverage analysis
    /// - Performance benchmarking
    /// </summary>
    public class EvaluationFramework
    {
        private readonly AgentsClient _agentsClient;
        private readonly ResponsesClient _responsesClient;
        private readonly ConversationsClient _conversationsClient;
        private readonly string _modelDeploymentName;

        public EvaluationFramework(AgentsClient agentsClient, ResponsesClient responsesClient, string modelDeploymentName)
        {
            _agentsClient = agentsClient ?? throw new ArgumentNullException(nameof(agentsClient));
            _responsesClient = responsesClient ?? throw new ArgumentNullException(nameof(responsesClient));
            _conversationsClient = agentsClient.GetConversationsClient();
            _modelDeploymentName = modelDeploymentName ?? throw new ArgumentNullException(nameof(modelDeploymentName));
        }

        /// <summary>
        /// Creates a comprehensive evaluation dataset covering various agent scenarios.
        /// </summary>
        /// <returns>Filename of the created evaluation dataset</returns>
        public async Task<string> CreateEvaluationDatasetAsync()
        {
            Console.WriteLine("\n📊 CREATING EVALUATION DATASET");
            Console.WriteLine(new string('=', 70));

            // <evaluation_dataset_structure>
            // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
            var evalDataset = new[]
            {
                new {
                    Id = "eval_001",
                    Question = "What is our company's remote work security policy?",
                    ExpectedSource = "sharepoint",
                    ExpectedTopics = new[] { "remote work", "security", "policy", "VPN", "MFA" },
                    QualityThreshold = 0.7
                },
                new {
                    Id = "eval_002",
                    Question = "How do I configure Azure AD Conditional Access?",
                    ExpectedSource = "microsoft_learn",
                    ExpectedTopics = new[] { "Azure AD", "conditional access", "configuration" },
                    QualityThreshold = 0.8
                },
                new {
                    Id = "eval_003",
                    Question = "What Azure security features should I use to comply with our data governance policy?",
                    ExpectedSource = "both",
                    ExpectedTopics = new[] { "Azure", "security", "compliance", "governance", "data" },
                    QualityThreshold = 0.75
                },
                new {
                    Id = "eval_004",
                    Question = "Explain our collaboration standards for Microsoft Teams",
                    ExpectedSource = "sharepoint",
                    ExpectedTopics = new[] { "Teams", "collaboration", "standards", "sharing" },
                    QualityThreshold = 0.7
                },
                new {
                    Id = "eval_005",
                    Question = "How do I implement multi-factor authentication in Azure?",
                    ExpectedSource = "microsoft_learn",
                    ExpectedTopics = new[] { "MFA", "authentication", "Azure", "implementation" },
                    QualityThreshold = 0.8
                }
            };
            // </evaluation_dataset_structure>

            var filename = "evaluation_dataset.jsonl";
            await using var writer = new StreamWriter(filename);
            foreach (var item in evalDataset)
            {
                await writer.WriteLineAsync(JsonSerializer.Serialize(item));
            }

            Console.WriteLine($"✅ Created evaluation dataset: {filename}");
            Console.WriteLine($"   Total questions: {evalDataset.Length}");
            Console.WriteLine($"   Coverage: SharePoint, Microsoft Learn, Combined scenarios");

            return filename;
        }

        /// <summary>
        /// Runs baseline evaluation against the agent using the provided dataset.
        /// </summary>
        /// <param name="agentName">Name of the agent to evaluate</param>
        /// <param name="datasetFile">Path to the evaluation dataset file</param>
        /// <returns>Dictionary containing evaluation results and metrics</returns>
        public async Task<Dictionary<string, object>> RunBaselineEvaluationAsync(string agentName, string datasetFile)
        {
            Console.WriteLine("\n📈 RUNNING BASELINE EVALUATION");
            Console.WriteLine(new string('=', 70));

            // Load evaluation dataset
            var evalData = new List<JsonElement>();
            foreach (var line in await File.ReadAllLinesAsync(datasetFile))
            {
                evalData.Add(JsonSerializer.Deserialize<JsonElement>(line));
            }

            var results = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["agent_name"] = agentName,
                ["dataset"] = datasetFile,
                ["total_questions"] = evalData.Count,
                ["evaluations"] = new List<object>()
            };

            var evaluations = (List<object>)results["evaluations"];
            int passedCount = 0;
            double totalQualityScore = 0;

            foreach (var item in evalData)
            {
                var id = item.GetProperty("Id").GetString();
                var question = item.GetProperty("Question").GetString();
                var expectedTopics = item.GetProperty("ExpectedTopics").EnumerateArray()
                    .Select(t => t.GetString()!).ToArray();
                var qualityThreshold = item.GetProperty("QualityThreshold").GetDouble();

                Console.WriteLine($"\n🔍 Evaluating: {id}");
                Console.WriteLine($"   Question: {question!.Substring(0, Math.Min(60, question.Length))}...");

                try
                {
                    var (responseText, _) = await ChatWithAssistantAsync(agentName, question!);

                    // <quality_metric_calculation>
                    // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
                    // Calculate quality score based on topic coverage
                    var topicsFound = expectedTopics.Count(topic => 
                        responseText.ToLower().Contains(topic.ToLower()));
                    var qualityScore = topicsFound / (double)expectedTopics.Length;

                    var passed = qualityScore >= qualityThreshold;
                    // </quality_metric_calculation>

                    var evalResult = new Dictionary<string, object>
                    {
                        ["id"] = id!,
                        ["question"] = question!,
                        ["response_length"] = responseText.Length,
                        ["topics_expected"] = expectedTopics.Length,
                        ["topics_found"] = topicsFound,
                        ["quality_score"] = qualityScore,
                        ["threshold"] = qualityThreshold,
                        ["passed"] = passed
                    };

                    evaluations.Add(evalResult);
                    totalQualityScore += qualityScore;

                    if (passed)
                    {
                        passedCount++;
                    }

                    var status = passed ? "✅ PASS" : "❌ FAIL";
                    Console.WriteLine($"   {status} - Quality: {qualityScore:F2} (threshold: {qualityThreshold})");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"   ❌ ERROR: {ex.Message}");
                    evaluations.Add(new Dictionary<string, object>
                    {
                        ["id"] = id!,
                        ["error"] = ex.Message,
                        ["passed"] = false
                    });
                }
            }

            results["pass_rate"] = (passedCount / (double)evalData.Count) * 100;
            results["avg_quality_score"] = totalQualityScore / evalData.Count;

            Console.WriteLine("\n" + new string('=', 70));
            Console.WriteLine($"📊 BASELINE EVALUATION COMPLETE");
            Console.WriteLine($"   Pass Rate: {results["pass_rate"]:F1}%");
            Console.WriteLine($"   Avg Quality Score: {results["avg_quality_score"]:F2}");
            Console.WriteLine($"   Passed: {passedCount}/{evalData.Count}");

            await File.WriteAllTextAsync("baseline_evaluation.json",
                JsonSerializer.Serialize(results, new JsonSerializerOptions { WriteIndented = true }));
            Console.WriteLine($"   📄 Results saved to: baseline_evaluation.json");

            return results;
        }

        /// <summary>
        /// Performs advanced topic coverage analysis on agent responses.
        /// </summary>
        /// <param name="response">The agent's response text</param>
        /// <param name="expectedTopics">List of expected topics to find</param>
        /// <returns>Detailed topic coverage analysis</returns>
        public Dictionary<string, object> AnalyzeTopicCoverage(string response, string[] expectedTopics)
        {
            var responseLower = response.ToLower();
            var topicAnalysis = new Dictionary<string, object>
            {
                ["total_topics"] = expectedTopics.Length,
                ["topics_found"] = 0,
                ["topics_missing"] = new List<string>(),
                ["coverage_score"] = 0.0,
                ["detailed_coverage"] = new Dictionary<string, bool>()
            };

            var topicsFound = 0;
            var topicsMissing = (List<string>)topicAnalysis["topics_missing"];
            var detailedCoverage = (Dictionary<string, bool>)topicAnalysis["detailed_coverage"];

            foreach (var topic in expectedTopics)
            {
                var found = responseLower.Contains(topic.ToLower());
                detailedCoverage[topic] = found;

                if (found)
                {
                    topicsFound++;
                }
                else
                {
                    topicsMissing.Add(topic);
                }
            }

            topicAnalysis["topics_found"] = topicsFound;
            topicAnalysis["coverage_score"] = topicsFound / (double)expectedTopics.Length;

            return topicAnalysis;
        }

        /// <summary>
        /// Compares current evaluation results against a baseline to detect regressions.
        /// </summary>
        /// <param name="currentResults">Current evaluation results</param>
        /// <param name="baselineResults">Baseline evaluation results to compare against</param>
        /// <returns>Regression analysis results</returns>
        public Dictionary<string, object> DetectRegressions(
            Dictionary<string, object> currentResults,
            Dictionary<string, object> baselineResults)
        {
            var currentPassRate = (double)currentResults["pass_rate"];
            var baselinePassRate = (double)baselineResults["pass_rate"];
            var currentQualityScore = (double)currentResults["avg_quality_score"];
            var baselineQualityScore = (double)baselineResults["avg_quality_score"];

            var passRateDelta = currentPassRate - baselinePassRate;
            var qualityScoreDelta = currentQualityScore - baselineQualityScore;

            var hasRegression = passRateDelta < -5.0 || qualityScoreDelta < -0.05;

            var regressionAnalysis = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["has_regression"] = hasRegression,
                ["metrics"] = new Dictionary<string, object>
                {
                    ["pass_rate"] = new Dictionary<string, object>
                    {
                        ["current"] = currentPassRate,
                        ["baseline"] = baselinePassRate,
                        ["delta"] = passRateDelta,
                        ["status"] = passRateDelta >= -5.0 ? "OK" : "REGRESSED"
                    },
                    ["quality_score"] = new Dictionary<string, object>
                    {
                        ["current"] = currentQualityScore,
                        ["baseline"] = baselineQualityScore,
                        ["delta"] = qualityScoreDelta,
                        ["status"] = qualityScoreDelta >= -0.05 ? "OK" : "REGRESSED"
                    }
                }
            };

            return regressionAnalysis;
        }

        /// <summary>
        /// Generates a comprehensive evaluation report with visualizations.
        /// </summary>
        /// <param name="evaluationResults">The evaluation results to report on</param>
        /// <returns>Formatted evaluation report string</returns>
        public string GenerateEvaluationReport(Dictionary<string, object> evaluationResults)
        {
            var report = new System.Text.StringBuilder();
            report.AppendLine("╔════════════════════════════════════════════════════════════════════╗");
            report.AppendLine("║           AGENT EVALUATION REPORT                                  ║");
            report.AppendLine("╚════════════════════════════════════════════════════════════════════╝");
            report.AppendLine();

            report.AppendLine($"Agent: {evaluationResults["agent_name"]}");
            report.AppendLine($"Evaluation Date: {evaluationResults["timestamp"]}");
            report.AppendLine($"Dataset: {evaluationResults["dataset"]}");
            report.AppendLine();

            report.AppendLine("Overall Metrics:");
            report.AppendLine(new string('-', 70));
            report.AppendLine($"Pass Rate: {evaluationResults["pass_rate"]:F1}%");
            report.AppendLine($"Average Quality Score: {evaluationResults["avg_quality_score"]:F2}");
            report.AppendLine($"Total Questions: {evaluationResults["total_questions"]}");
            report.AppendLine();

            var evaluations = (List<object>)evaluationResults["evaluations"];
            report.AppendLine("Question Results:");
            report.AppendLine(new string('-', 70));

            foreach (var evaluation in evaluations)
            {
                var evalDict = (Dictionary<string, object>)evaluation;
                var id = evalDict["id"];
                var passed = evalDict.ContainsKey("passed") && (bool)evalDict["passed"];
                var symbol = passed ? "✅" : "❌";

                if (evalDict.ContainsKey("quality_score"))
                {
                    var qualityScore = (double)evalDict["quality_score"];
                    report.AppendLine($"{symbol} {id}: Quality Score = {qualityScore:F2}");
                }
                else
                {
                    report.AppendLine($"{symbol} {id}: ERROR");
                }
            }

            return report.ToString();
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
