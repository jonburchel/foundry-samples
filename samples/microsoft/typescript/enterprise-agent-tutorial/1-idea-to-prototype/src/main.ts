// #region imports_and_setup
import { AIProjectsClient } from "@azure/ai-projects";
import { DefaultAzureCredential } from "@azure/identity";
import * as dotenv from "dotenv";

dotenv.config();

const projectEndpoint = process.env.PROJECT_ENDPOINT!;
const modelDeploymentName = process.env.MODEL_DEPLOYMENT_NAME!;
const sharepointResourceName = process.env.SHAREPOINT_RESOURCE_NAME;
const mcpServerUrl = process.env.MCP_SERVER_URL || "https://learn.microsoft.com/api/mcp";
// #endregion imports_and_setup

// #region create_workplace_assistant
async function createWorkplaceAssistant(): Promise<void> {
    console.log("🤖 Creating Modern Workplace Assistant...\n");

    const credential = new DefaultAzureCredential();
    const client = new AIProjectsClient(projectEndpoint, credential);

    // Check SharePoint availability
    let sharepointAvailable = false;
    if (sharepointResourceName) {
        try {
            const connections = await client.connections.list();
            sharepointAvailable = connections.some(conn => conn.name === sharepointResourceName);
            
            if (sharepointAvailable) {
                console.log(`✅ SharePoint connected: ${sharepointResourceName}`);
            } else {
                console.log(`⚠️  SharePoint connection not found: Connection '${sharepointResourceName}' not found`);
                console.log("Agent will operate without SharePoint integration.\n");
            }
        } catch (error) {
            console.log(`⚠️  Could not verify SharePoint connection: ${error}`);
            console.log("Agent will operate without SharePoint integration.\n");
        }
    }

    // Define tools using inline JSON (workaround until TypeScript SDK has SharepointTool/McpTool classes)
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

    // Build dynamic instructions
    let instructions = "You are a Modern Workplace Assistant helping employees with company policies and technical implementation.";
    
    if (sharepointAvailable) {
        instructions += "\n\nYou have access to:\n1. SharePoint for company policies and internal documentation\n2. Microsoft Learn for technical implementation guidance";
        instructions += "\n\nFor questions:\n- Use SharePoint to find company policies and internal procedures\n- Use Microsoft Learn for Azure and Microsoft 365 technical documentation\n- Combine both sources when helping employees implement technical solutions that align with company policies";
    } else {
        instructions += "\n\nYou have access to Microsoft Learn for technical guidance. SharePoint integration is not currently available, so you cannot access company-specific policies.";
        instructions += "\n\nFor technical questions about Azure, Microsoft 365, or other Microsoft products, use the Microsoft Learn MCP server to provide accurate, up-to-date information with reference links.";
    }

    // Create the agent
    const agent = await client.agents.createAgent(modelDeploymentName, {
        name: "Modern Workplace Assistant",
        instructions: instructions,
        tools: tools
    });

    console.log(`✅ Agent created: ${agent.id}\n`);

    // Business scenarios
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

        const thread = await client.agents.createThread();
        await client.agents.createMessage(thread.id, "user", scenario.question);
        
        const run = await client.agents.createRun(thread.id, agent.id);
        
        // Poll for completion
        let runStatus = await client.agents.getRun(thread.id, run.id);
        while (runStatus.status === "in_progress" || runStatus.status === "queued") {
            await new Promise(resolve => setTimeout(resolve, 1000));
            runStatus = await client.agents.getRun(thread.id, run.id);
        }

        if (runStatus.status === "completed") {
            const messages = await client.agents.listMessages(thread.id);
            const lastMessage = messages.data[0];
            if (lastMessage.content && lastMessage.content.length > 0) {
                const content = lastMessage.content[0];
                if (content.type === "text") {
                    console.log(`🤖 ${content.text.value}\n`);
                }
            }
        } else {
            console.log(`❌ Run failed with status: ${runStatus.status}\n`);
        }

        await client.agents.deleteThread(thread.id);
    }

    console.log("\n💡 Interactive Mode");
    console.log("The agent is ready. In a production scenario, you would integrate this with your application's user interface.");
    console.log("Users could ask questions combining company policies with technical implementation guidance.\n");

    // Cleanup
    await client.agents.deleteAgent(agent.id);
}
// #endregion create_workplace_assistant

// Run the assistant
createWorkplaceAssistant().catch(console.error);
