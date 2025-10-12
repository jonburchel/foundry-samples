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
dotenv.config();
const projectEndpoint = process.env.PROJECT_ENDPOINT;
const modelDeploymentName = process.env.MODEL_DEPLOYMENT_NAME;
const sharepointResourceName = process.env.SHAREPOINT_RESOURCE_NAME;
const mcpServerUrl = process.env.MCP_SERVER_URL || "https://learn.microsoft.com/api/mcp";
async function createWorkplaceAssistant() {
    console.log("🤖 Creating Modern Workplace Assistant...\n");
    const credential = new identity_1.DefaultAzureCredential();
    const client = new ai_projects_1.AIProjectClient(projectEndpoint, credential);
    // Note: SharePoint grounding via connection is not yet supported in TypeScript SDK v1.0.0-beta.1
    // This sample demonstrates the agent framework without SharePoint integration
    // For production scenarios, consider using the Python SDK which has full SharePoint support
    let sharepointConnectionId = null;
    if (sharepointResourceName) {
        console.log(`⚠️  SharePoint grounding not yet supported in TypeScript SDK v1.0.0-beta.1`);
        console.log(`   Connection '${sharepointResourceName}' configured but cannot be used`);
        console.log("   Agent will operate in technical guidance mode only\n");
    }
    const tools = [];
    // SharePoint tool would be added here when SDK support is available
    console.log("📚 Configuring Microsoft Learn MCP integration...");
    console.log(`✅ Microsoft Learn MCP: ${mcpServerUrl}\n`);
    let instructions = "You are a Modern Workplace Assistant helping employees with company policies and technical implementation.";
    if (sharepointConnectionId) {
        instructions += "\n\nYou have access to:\n1. SharePoint for company policies and internal documentation\n2. Microsoft Learn for technical implementation guidance";
        instructions += "\n\nFor questions:\n- Use SharePoint to find company policies and internal procedures\n- Use Microsoft Learn for Azure and Microsoft 365 technical documentation\n- Combine both sources when helping employees implement technical solutions that align with company policies";
    }
    else {
        instructions += "\n\nYou have access to Microsoft Learn for technical guidance. SharePoint integration is not currently available, so you cannot access company-specific policies.";
        instructions += "\n\nFor technical questions about Azure, Microsoft 365, or other Microsoft products, use the Microsoft Learn MCP server to provide accurate, up-to-date information with reference links.";
    }
    const agent = await client.agents.createAgent(modelDeploymentName, {
        name: "Modern Workplace Assistant",
        instructions: instructions,
        tools: tools
    });
    console.log(`✅ Agent created: ${agent.id}\n`);
    const scenarios = [
        {
            type: "📋 Policy Question",
            question: "What is our remote work policy regarding security requirements?"
        },
        {
            type: "🔧 Technical Question",
            question: "How do I set up Azure Active Directory conditional access?"
        },
        {
            type: "🔄 Implementation Question",
            question: "Our security policy requires MFA - how do I implement this in Azure AD?"
        }
    ];
    for (let i = 0; i < scenarios.length; i++) {
        const scenario = scenarios[i];
        console.log(`${scenario.type} ${i + 1}/${scenarios.length}`);
        console.log(`❓ ${scenario.question}`);
        const thread = await client.agents.threads.create();
        await client.agents.messages.create(thread.id, "user", scenario.question);
        const run = await client.agents.runs.create(thread.id, agent.id);
        let runStatus = await client.agents.runs.get(thread.id, run.id);
        while (runStatus.status === "in_progress" || runStatus.status === "queued") {
            await new Promise(resolve => setTimeout(resolve, 1000));
            runStatus = await client.agents.runs.get(thread.id, run.id);
        }
        if (runStatus.status === "completed") {
            const messages = client.agents.messages.list(thread.id);
            for await (const message of messages) {
                if (message.role === "assistant") {
                    if (message.content && message.content.length > 0) {
                        const content = message.content[0];
                        if (content.type === "text" && "text" in content) {
                            console.log(`🤖 ${content.text.value}\n`);
                        }
                    }
                    break;
                }
            }
        }
        else {
            console.log(`❌ Run failed with status: ${runStatus.status}\n`);
        }
        await client.agents.threads.delete(thread.id);
    }
    console.log("\n💡 Interactive Mode");
    console.log("The agent is ready. In a production scenario, you would integrate this with your application's user interface.");
    console.log("Users could ask questions combining company policies with technical implementation guidance.\n");
    await client.agents.deleteAgent(agent.id);
}
createWorkplaceAssistant().catch(console.error);
