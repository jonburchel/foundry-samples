using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using Azure;
using Azure.AI.Agents.Persistent;
using Azure.Identity;
using DotNetEnv;

class EvaluateProgram
{
    static async Task Main(string[] args)
    {
        Env.Load();

        var projectEndpoint = Environment.GetEnvironmentVariable("PROJECT_ENDPOINT");
        var modelDeploymentName = Environment.GetEnvironmentVariable("MODEL_DEPLOYMENT_NAME");
        var sharepointSiteUrl = Environment.GetEnvironmentVariable("SHAREPOINT_SITE_URL");

        PersistentAgentsClient client = new(projectEndpoint, new DefaultAzureCredential());

        Console.WriteLine("🧪 Modern Workplace Assistant Evaluation\n");

        List<ToolDefinition> tools = new();
        ToolResources toolResources = null;

        if (!string.IsNullOrEmpty(sharepointSiteUrl))
        {
            try
            {
                VectorStoreDataSource dataSource = new(
                    assetIdentifier: sharepointSiteUrl,
                    assetType: VectorStoreDataSourceAssetType.UriAsset
                );

                PersistentAgentsVectorStore vectorStore = await client.VectorStores.CreateVectorStoreAsync(
                    name: "company_policies_eval",
                    storeConfiguration: new VectorStoreConfiguration(dataSources: new[] { dataSource })
                );

                FileSearchToolResource fileSearchResource = new(new[] { vectorStore.Id }, null);
                toolResources = new ToolResources { FileSearch = fileSearchResource };
                tools.Add(new FileSearchToolDefinition());

                Console.WriteLine("✅ SharePoint configured for evaluation\n");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"⚠️  SharePoint unavailable: {ex.Message}\n");
            }
        }

        var instructions = @"You are a Modern Workplace Assistant for Contoso Corporation.
Answer questions using available tools and provide specific, detailed responses.";

        PersistentAgent agent = await client.Administration.CreateAgentAsync(
            model: modelDeploymentName,
            name: "Evaluation Agent",
            instructions: instructions,
            tools: tools,
            toolResources: toolResources
        );

        var questions = File.ReadAllLines("questions.jsonl")
            .Select(line => JsonSerializer.Deserialize<Dictionary<string, string>>(line))
            .ToList();

        var results = new List<object>();

        Console.WriteLine($"Running {questions.Count} evaluation questions...\n");

        for (int i = 0; i < questions.Count; i++)
        {
            var q = questions[i];
            var question = q["question"];
            var expectedKeywords = q.ContainsKey("expected_keywords") ? q["expected_keywords"].Split(',') : Array.Empty<string>();
            
            Console.WriteLine($"Question {i + 1}/{questions.Count}: {question}");

            PersistentAgentThread thread = await client.Threads.CreateThreadAsync();
            await client.Messages.CreateMessageAsync(thread.Id, MessageRole.User, question);
            
            ThreadRun run = await client.Runs.CreateRunAsync(thread.Id, agent.Id);
            
            do
            {
                await Task.Delay(TimeSpan.FromMilliseconds(1000));
                run = await client.Runs.GetRunAsync(thread.Id, run.Id);
            }
            while (run.Status == RunStatus.Queued || run.Status == RunStatus.InProgress);

            string response = "";
            if (run.Status == RunStatus.Completed)
            {
                AsyncPageable<PersistentThreadMessage> messages = client.Messages.GetMessagesAsync(
                    threadId: thread.Id,
                    order: ListSortOrder.Descending
                );

                await foreach (PersistentThreadMessage message in messages)
                {
                    if (message.Role == MessageRole.Agent)
                    {
                        foreach (MessageContent content in message.ContentItems)
                        {
                            if (content is MessageTextContent textContent)
                            {
                                response = textContent.Text;
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            bool passed = response.Length > 50;
            if (expectedKeywords.Length > 0)
            {
                passed = passed && expectedKeywords.Any(k => response.Contains(k, StringComparison.OrdinalIgnoreCase));
            }

            Console.WriteLine($"   Status: {(passed ? "✅ PASS" : "❌ FAIL")}");
            Console.WriteLine($"   Response length: {response.Length} characters\n");

            results.Add(new
            {
                question,
                response,
                passed,
                response_length = response.Length
            });

            await client.Threads.DeleteThreadAsync(thread.Id);
        }

        await client.Administration.DeleteAgentAsync(agent.Id);

        var summary = new
        {
            total_questions = questions.Count,
            passed = results.Count(r => ((dynamic)r).passed),
            failed = results.Count(r => !((dynamic)r).passed),
            results
        };

        var json = JsonSerializer.Serialize(summary, new JsonSerializerOptions { WriteIndented = true });
        File.WriteAllText("evaluation_results.json", json);

        Console.WriteLine($"📊 Evaluation Complete:");
        Console.WriteLine($"   Total: {summary.total_questions}");
        Console.WriteLine($"   Passed: {summary.passed}");
        Console.WriteLine($"   Failed: {summary.failed}");
        Console.WriteLine($"\n📄 Results saved to evaluation_results.json");
    }
}
