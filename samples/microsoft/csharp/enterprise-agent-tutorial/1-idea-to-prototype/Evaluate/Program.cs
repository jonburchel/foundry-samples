using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;
using Azure;
using Azure.AI.Agents.Persistent;
using Azure.Core;
using Azure.Identity;
using DotNetEnv;

class EvaluateProgram
{
    static async Task Main(string[] args)
    {
        // Load environment variables from shared directory
        Env.Load("../shared/.env");

        var projectEndpoint = Environment.GetEnvironmentVariable("PROJECT_ENDPOINT");
        var modelDeploymentName = Environment.GetEnvironmentVariable("MODEL_DEPLOYMENT_NAME");
        var sharepointConnectionId = Environment.GetEnvironmentVariable("SHAREPOINT_CONNECTION_ID");
        var mcpServerUrl = Environment.GetEnvironmentVariable("MCP_SERVER_URL");
        var tenantId = Environment.GetEnvironmentVariable("AI_FOUNDRY_TENANT_ID");

        // Use tenant-specific credential if provided
        TokenCredential credential;
        if (!string.IsNullOrEmpty(tenantId))
        {
            credential = new AzureCliCredential(new AzureCliCredentialOptions { TenantId = tenantId });
        }
        else
        {
            credential = new DefaultAzureCredential();
        }

        PersistentAgentsClient client = new(projectEndpoint, credential);

        Console.WriteLine("🧪 Modern Workplace Assistant Evaluation\n");

        List<ToolDefinition> tools = new();

        // Add SharePoint tool if configured
        if (!string.IsNullOrEmpty(sharepointConnectionId))
        {
            try
            {
                SharepointToolDefinition sharepointTool = new(new SharepointGroundingToolParameters(sharepointConnectionId));
                tools.Add(sharepointTool);
                Console.WriteLine("✅ SharePoint configured for evaluation");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"⚠️  SharePoint unavailable: {ex.Message}");
            }
        }

        // Add MCP tool if configured
        if (!string.IsNullOrEmpty(mcpServerUrl))
        {
            try
            {
                MCPToolDefinition mcpTool = new("microsoft_learn", mcpServerUrl);
                tools.Add(mcpTool);
                Console.WriteLine("✅ MCP configured for evaluation");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"⚠️  MCP unavailable: {ex.Message}");
            }
        }

        Console.WriteLine();

        var instructions = @"You are a Modern Workplace Assistant for Contoso Corporation.
Answer questions using available tools and provide specific, detailed responses.";

        PersistentAgent agent = await client.Administration.CreateAgentAsync(
            model: modelDeploymentName,
            name: "Evaluation Agent",
            instructions: instructions,
            tools: tools
        );

        var questions = File.ReadAllLines("../shared/questions.jsonl")
            .Select(line => JsonSerializer.Deserialize<JsonElement>(line))
            .ToList();

        var results = new List<object>();

        Console.WriteLine($"Running {questions.Count} evaluation questions...\n");

        for (int i = 0; i < questions.Count; i++)
        {
            var q = questions[i];
            var question = q.GetProperty("question").GetString()!;
            
            string[] expectedKeywords = Array.Empty<string>();
            if (q.TryGetProperty("expected_keywords", out var keywordsElem))
            {
                expectedKeywords = keywordsElem.EnumerateArray()
                    .Select(e => e.GetString()!)
                    .ToArray();
            }
            
            Console.WriteLine($"Question {i + 1}/{questions.Count}: {question}");

            PersistentAgentThread thread = await client.Threads.CreateThreadAsync();
            await client.Messages.CreateMessageAsync(thread.Id, MessageRole.User, question);
            
            ThreadRun run = await client.Runs.CreateRunAsync(thread.Id, agent.Id);
            
            // Poll for completion with MCP approval handling
            while (run.Status == RunStatus.Queued || 
                   run.Status == RunStatus.InProgress || 
                   run.Status == RunStatus.RequiresAction)
            {
                await Task.Delay(TimeSpan.FromMilliseconds(1000));
                run = await client.Runs.GetRunAsync(thread.Id, run.Id);

                // Handle MCP tool approval requests
                if (run.Status == RunStatus.RequiresAction && 
                    run.RequiredAction is SubmitToolApprovalAction toolApprovalAction)
                {
                    var toolApprovals = new List<ToolApproval>();
                    foreach (var toolCall in toolApprovalAction.SubmitToolApproval.ToolCalls)
                    {
                        if (toolCall is RequiredMcpToolCall mcpToolCall)
                        {
                            toolApprovals.Add(new ToolApproval(mcpToolCall.Id, approve: true));
                        }
                    }

                    if (toolApprovals.Count > 0)
                    {
                        run = await client.Runs.SubmitToolOutputsToRunAsync(
                            thread.Id, 
                            run.Id, 
                            toolApprovals: toolApprovals
                        );
                    }
                }
            }

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
