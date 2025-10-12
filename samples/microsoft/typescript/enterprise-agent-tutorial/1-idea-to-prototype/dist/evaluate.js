"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
const ai_projects_1 = require("@azure/ai-projects");
const identity_1 = require("@azure/identity");
const dotenv = __importStar(require("dotenv"));
const fs = __importStar(require("fs"));
const readline = __importStar(require("readline"));
dotenv.config();
const projectEndpoint = process.env.PROJECT_ENDPOINT;
const modelDeploymentName = process.env.MODEL_DEPLOYMENT_NAME;
const sharepointResourceName = process.env.SHAREPOINT_RESOURCE_NAME;
const mcpServerUrl = process.env.MCP_SERVER_URL || "https://learn.microsoft.com/api/mcp";
async function runEvaluation() {
    console.log("🧪 Modern Workplace Assistant Evaluation\n");
    const credential = new identity_1.DefaultAzureCredential();
    const client = new ai_projects_1.AIProjectClient(projectEndpoint, credential);
    let sharepointAvailable = false;
    if (sharepointResourceName) {
        try {
            const connections = await client.connections.list();
            const connectionsList = [];
            for await (const conn of connections) {
                connectionsList.push(conn);
            }
            sharepointAvailable = connectionsList.some((conn) => conn.name === sharepointResourceName);
            if (sharepointAvailable) {
                console.log("✅ SharePoint configured for evaluation\n");
            }
        }
        catch {
            console.log("⚠️  SharePoint unavailable\n");
        }
    }
    const tools = [];
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
    const instructions = "You are a Modern Workplace Assistant for Contoso Corporation. " +
        "Answer questions using available tools and provide specific, detailed responses.";
    const agent = await client.agents.createAgent(modelDeploymentName, {
        name: "Evaluation Agent",
        instructions: instructions,
        tools: tools
    });
    console.log(`✅ Agent created for evaluation: ${agent.id}\n`);
    const fileStream = fs.createReadStream("questions.jsonl");
    const rl = readline.createInterface({
        input: fileStream,
        crlfDelay: Infinity
    });
    const questions = [];
    for await (const line of rl) {
        if (line.trim()) {
            questions.push(JSON.parse(line));
        }
    }
    console.log(`Running ${questions.length} evaluation questions...\n`);
    const results = [];
    for (let i = 0; i < questions.length; i++) {
        const q = questions[i];
        console.log(`Question ${i + 1}/${questions.length}: ${q.question}`);
        const thread = await client.agents.threads.create();
        await client.agents.messages.create(thread.id, "user", q.question);
        const run = await client.agents.runs.create(thread.id, agent.id);
        let runStatus = await client.agents.runs.get(thread.id, run.id);
        while (runStatus.status === "in_progress" || runStatus.status === "queued") {
            await new Promise(resolve => setTimeout(resolve, 1000));
            runStatus = await client.agents.runs.get(thread.id, run.id);
        }
        let response = "";
        if (runStatus.status === "completed") {
            const messages = client.agents.messages.list(thread.id);
            for await (const message of messages) {
                if (message.role === "assistant") {
                    if (message.content && message.content.length > 0) {
                        const content = message.content[0];
                        if (content.type === "text" && "text" in content) {
                            response = content.text.value;
                        }
                    }
                    break;
                }
            }
        }
        let passed = response.length > 50;
        if (q.expected_keywords) {
            const keywords = q.expected_keywords.split(',');
            const hasKeyword = keywords.some(k => response.toLowerCase().includes(k.trim().toLowerCase()));
            passed = passed && hasKeyword;
        }
        console.log(`   Status: ${passed ? "✅ PASS" : "❌ FAIL"}`);
        console.log(`   Response length: ${response.length} characters\n`);
        results.push({
            question: q.question,
            response: response,
            passed: passed,
            response_length: response.length
        });
        await client.agents.threads.delete(thread.id);
    }
    await client.agents.deleteAgent(agent.id);
    const passedCount = results.filter(r => r.passed).length;
    const failedCount = results.length - passedCount;
    const summary = {
        total_questions: questions.length,
        passed: passedCount,
        failed: failedCount,
        results: results
    };
    fs.writeFileSync("evaluation_results.json", JSON.stringify(summary, null, 2));
    console.log("📊 Evaluation Complete:");
    console.log(`   Total: ${questions.length}`);
    console.log(`   Passed: ${passedCount}`);
    console.log(`   Failed: ${failedCount}`);
    console.log("\n📄 Results saved to evaluation_results.json");
}
runEvaluation().catch(console.error);
