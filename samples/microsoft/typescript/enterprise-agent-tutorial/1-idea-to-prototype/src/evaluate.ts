// #region evaluation_functions
import { AIProjectsClient } from "@azure/ai-projects";
import { DefaultAzureCredential } from "@azure/identity";
import * as dotenv from "dotenv";
import * as fs from "fs";
import * as readline from "readline";

dotenv.config();

const projectEndpoint = process.env.PROJECT_ENDPOINT!;
const modelDeploymentName = process.env.MODEL_DEPLOYMENT_NAME!;
const sharepointResourceName = process.env.SHAREPOINT_RESOURCE_NAME;
const mcpServerUrl = process.env.MCP_SERVER_URL || "https://learn.microsoft.com/api/mcp";

interface EvaluationQuestion {
    question: string;
    expected_sources: string[];
}

interface EvaluationResult {
    question: string;
    response: string;
    expected_sources: string[];
    found_sources: string[];
    passed: boolean;
    response_length: number;
}

function evaluateResponse(response: string, expectedSources: string[]): { foundSources: string[], passed: boolean } {
    const lowerResponse = response.toLowerCase();
    const foundSources = expectedSources.filter(source => {
        const lowerSource = source.toLowerCase();
        return lowerResponse.includes(lowerSource) || 
               (lowerSource.includes("sharepoint") && (lowerResponse.includes("policy") || lowerResponse.includes("internal"))) ||
               (lowerSource.includes("microsoft learn") && (lowerResponse.includes("azure") || lowerResponse.includes("documentation")));
    });
    
    const hasContent = response.length > 50;
    const passed = foundSources.length > 0 && hasContent;
    
    return { foundSources, passed };
}
// #endregion evaluation_functions

async function runEvaluation(): Promise<void> {
    console.log("📊 Starting Business Evaluation\n");

    const credential = new DefaultAzureCredential();
    const client = new AIProjectsClient(projectEndpoint, credential);

    // Check SharePoint availability
    let sharepointAvailable = false;
    if (sharepointResourceName) {
        try {
            const connections = await client.connections.list();
            sharepointAvailable = connections.some(conn => conn.name === sharepointResourceName);
        } catch {
            sharepointAvailable = false;
        }
    }

    // Define tools
    const tools: any[] = [];
    if (sharepointAvailable && sharepointResourceName) {
        tools.push({
            type: "sharepoint_grounding",
            sharepoint_grounding: {
                connection_id: sharepointResourceName
            }
        });
    }

    tools.push({
        type: "mcp",
        mcp: {
            server_url: mcpServerUrl,
            server_name: "microsoft_learn"
        }
    });

    // Build instructions
    let instructions = "You are a Modern Workplace Assistant helping employees with company policies and technical implementation.";
    if (sharepointAvailable) {
        instructions += "\n\nYou have access to:\n1. SharePoint for company policies\n2. Microsoft Learn for technical guidance";
    } else {
        instructions += "\n\nYou have access to Microsoft Learn for technical guidance.";
    }

    // Create agent
    const agent = await client.agents.createAgent(modelDeploymentName, {
        name: "Modern Workplace Assistant Evaluator",
        instructions: instructions,
        tools: tools
    });

    console.log(`✅ Agent created for evaluation: ${agent.id}\n`);

    // Load questions
    const fileStream = fs.createReadStream("questions.jsonl");
    const rl = readline.createInterface({
        input: fileStream,
        crlfDelay: Infinity
    });

    const questions: EvaluationQuestion[] = [];
    for await (const line of rl) {
        if (line.trim()) {
            questions.push(JSON.parse(line));
        }
    }

    console.log(`📝 Loaded ${questions.length} evaluation questions\n`);

    // Evaluate each question
    const results: EvaluationResult[] = [];
    
    for (let i = 0; i < questions.length; i++) {
        const q = questions[i];
        console.log(`Question ${i + 1}/${questions.length}: ${q.question}`);

        const thread = await client.agents.createThread();
        await client.agents.createMessage(thread.id, "user", q.question);
        
        const run = await client.agents.createRun(thread.id, agent.id);
        
        // Poll for completion
        let runStatus = await client.agents.getRun(thread.id, run.id);
        while (runStatus.status === "in_progress" || runStatus.status === "queued") {
            await new Promise(resolve => setTimeout(resolve, 1000));
            runStatus = await client.agents.getRun(thread.id, run.id);
        }

        let response = "";
        if (runStatus.status === "completed") {
            const messages = await client.agents.listMessages(thread.id);
            const lastMessage = messages.data[0];
            if (lastMessage.content && lastMessage.content.length > 0) {
                const content = lastMessage.content[0];
                if (content.type === "text") {
                    response = content.text.value;
                }
            }
        }

        const { foundSources, passed } = evaluateResponse(response, q.expected_sources);
        
        results.push({
            question: q.question,
            response: response,
            expected_sources: q.expected_sources,
            found_sources: foundSources,
            passed: passed,
            response_length: response.length
        });

        console.log(`${passed ? "✅ PASS" : "❌ FAIL"} - Found sources: ${foundSources.join(", ") || "none"}\n`);

        await client.agents.deleteThread(thread.id);
    }

    // Summary
    const passedCount = results.filter(r => r.passed).length;
    console.log("=" .repeat(50));
    console.log(`📊 Evaluation Complete: ${passedCount}/${results.length} passed`);
    console.log("=" .repeat(50));

    // Save results
    fs.writeFileSync("evaluation_results.json", JSON.stringify(results, null, 2));
    console.log("\n💾 Results saved to evaluation_results.json");

    // Cleanup
    await client.agents.deleteAgent(agent.id);
}

runEvaluation().catch(console.error);
