/**
 * Azure AI Foundry Agent Sample - Tutorial 1: Modern Workplace Assistant
 * 
 * This sample demonstrates a complete business scenario using Azure AI Agents SDK v2:
 * - Agent creation with the new SDK
 * - Thread and message management
 * - Robust error handling and graceful degradation
 * 
 * Educational Focus:
 * - Enterprise AI patterns with Agent SDK v2
 * - Real-world business scenarios that enterprises face daily
 * - Production-ready error handling and diagnostics
 * - Foundation for governance, evaluation, and monitoring (Tutorials 2-3)
 * 
 * Business Scenario:
 * An employee needs to implement Azure AD multi-factor authentication. They need:
 * 1. Company security policy requirements
 * 2. Technical implementation steps
 * 3. Combined guidance showing how policy requirements map to technical implementation
 */

// <imports_and_includes>
import { DefaultAzureCredential } from '@azure/identity';
import { AIProjectClient } from '@azure/ai-projects';
import { 
    Agent, 
    AgentThread, 
    ThreadMessage,
    ThreadRun,
    CreateAgentOptions,
    CreateThreadOptions,
    CreateMessageOptions,
    CreateAndProcessRunOptions,
    SharepointToolDefinition,
    McpToolDefinition,
    ToolDefinition
} from '@azure/ai-agents';
import { config } from 'dotenv';
import * as readline from 'readline';
// </imports_and_includes>

config({ path: '../.env' });

// ============================================================================
// AUTHENTICATION SETUP
// ============================================================================
// <agent_authentication>
// Support default Azure credentials
const credential = new DefaultAzureCredential();

const endpoint = process.env.PROJECT_ENDPOINT!;
const projectClient = new AIProjectClient(endpoint, credential);

console.log(`✅ Connected to Azure AI Foundry: ${endpoint}`);
// </agent_authentication>

/**
 * Create a Modern Workplace Assistant using Agent SDK v2.
 * 
 * This demonstrates enterprise AI patterns:
 * 1. Agent creation with the new SDK
 * 2. Robust error handling with graceful degradation
 * 3. Dynamic agent capabilities based on available resources
 * 4. Clear diagnostic information for troubleshooting
 * 
 * Educational Value:
 * - Shows real-world complexity of enterprise AI systems
 * - Demonstrates how to handle partial system failures
 * - Provides patterns for agent creation with Agent SDK v2
 */
async function createWorkplaceAssistant(): Promise<Agent> {
    console.log('\n🤖 Creating Modern Workplace Assistant...');

    const modelDeploymentName = process.env.MODEL_DEPLOYMENT_NAME!;

    // ========================================================================
    // SHAREPOINT INTEGRATION SETUP
    // ========================================================================
    // <sharepoint_connection_resolution>
    const sharepointResourceName = process.env.SHAREPOINT_RESOURCE_NAME;
    let sharepointTool: SharepointToolDefinition | null = null;

    if (sharepointResourceName) {
        console.log('📁 Configuring SharePoint integration...');
        console.log(`   Connection name: ${sharepointResourceName}`);

        try {
            // Resolve the connection name to its full ARM resource ID
            console.log('   🔍 Resolving connection name to ARM resource ID...');

            // List all connections and find the one we need
            const connections = projectClient.connections.list();
            let connectionId: string | null = null;

            for await (const conn of connections) {
                if (conn.name === sharepointResourceName) {
                    connectionId = conn.id;
                    console.log(`   ✅ Resolved to: ${connectionId}`);
                    break;
                }
            }

            if (!connectionId) {
                throw new Error(`Connection '${sharepointResourceName}' not found in project`);
            }

            // Create SharePoint tool with the full ARM resource ID
            sharepointTool = new SharepointToolDefinition(connectionId);
            console.log('✅ SharePoint tool configured successfully');

        } catch (error) {
            console.log(`⚠️  SharePoint connection unavailable: ${error}`);
            console.log('   Possible causes:');
            console.log(`   - Connection '${sharepointResourceName}' doesn't exist in the project`);
            console.log('   - Insufficient permissions to access the connection');
            console.log('   - Connection configuration is incomplete');
            console.log('   Agent will operate without SharePoint access');
            sharepointTool = null;
        }
    } else {
        console.log('📁 SharePoint integration skipped (SHAREPOINT_RESOURCE_NAME not set)');
    }
    // </sharepoint_connection_resolution>

    // ========================================================================
    // MICROSOFT LEARN MCP INTEGRATION SETUP
    // ========================================================================
    // <mcp_tool_setup>
    const mcpServerUrl = process.env.MCP_SERVER_URL;
    let mcpTool: McpToolDefinition | null = null;

    if (mcpServerUrl) {
        console.log('📚 Configuring Microsoft Learn MCP integration...');
        console.log(`   Server URL: ${mcpServerUrl}`);

        try {
            // Create MCP tool for Microsoft Learn documentation access
            // server_label must match pattern: ^[a-zA-Z0-9_]+$ (alphanumeric and underscores only)
            mcpTool = new McpToolDefinition(
                mcpServerUrl,
                'Microsoft_Learn_Documentation'
            );
            console.log('✅ MCP tool configured successfully');
        } catch (error) {
            console.log(`⚠️  MCP tool unavailable: ${error}`);
            console.log('   Agent will operate without Microsoft Learn access');
            mcpTool = null;
        }
    } else {
        console.log('📚 MCP integration skipped (MCP_SERVER_URL not set)');
    }
    // </mcp_tool_setup>

    // ========================================================================
    // AGENT CREATION WITH DYNAMIC CAPABILITIES
    // ========================================================================
    let instructions: string;
    
    if (sharepointTool && mcpTool) {
        instructions = `You are a Modern Workplace Assistant for Contoso Corporation.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide comprehensive solutions combining internal requirements with external implementation

RESPONSE STRATEGY:
- For policy questions: Search SharePoint for company-specific requirements and guidelines
- For technical questions: Use Microsoft Learn for current Azure/M365 documentation and best practices
- For implementation questions: Combine both sources to show how company policies map to technical implementation
- Always cite your sources and provide step-by-step guidance
- Explain how internal requirements connect to external implementation steps

EXAMPLE SCENARIOS:
- "What is our MFA policy?" → Search SharePoint for security policies
- "How do I configure Azure AD Conditional Access?" → Use Microsoft Learn for technical steps
- "Our policy requires MFA - how do I implement this?" → Combine policy requirements with implementation guidance`;
    } else if (sharepointTool) {
        instructions = `You are a Modern Workplace Assistant with access to Contoso Corporation's SharePoint.

CAPABILITIES:
- Search SharePoint for company policies, procedures, and internal documentation
- Provide detailed technical guidance based on your knowledge
- Combine company policies with general best practices

RESPONSE STRATEGY:
- Search SharePoint for company-specific requirements
- Provide technical guidance based on Azure and M365 best practices
- Explain how to align implementations with company policies`;
    } else if (mcpTool) {
        instructions = `You are a Technical Assistant with access to Microsoft Learn documentation.

CAPABILITIES:
- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance
- Provide detailed implementation steps and best practices
- Explain Azure services, features, and configuration options

RESPONSE STRATEGY:
- Use Microsoft Learn for technical documentation
- Provide comprehensive implementation guidance
- Reference official documentation and best practices`;
    } else {
        instructions = `You are a Technical Assistant specializing in Azure and Microsoft 365 guidance.

CAPABILITIES:
- Provide detailed Azure and Microsoft 365 technical guidance
- Explain implementation steps and best practices
- Help with Azure AD, Conditional Access, MFA, and security configurations

RESPONSE STRATEGY:
- Provide comprehensive technical guidance
- Include step-by-step implementation instructions
- Reference best practices and security considerations`;
    }

    // <create_agent_with_tools>
    console.log(`🛠️  Creating agent with model: ${modelDeploymentName}`);

    // Build tools list
    const tools: ToolDefinition[] = [];

    if (sharepointTool) {
        tools.push(sharepointTool);
        console.log('   ✓ SharePoint tool added');
    }

    if (mcpTool) {
        tools.push(mcpTool);
        console.log('   ✓ MCP tool added');
    }

    console.log(`   Total tools: ${tools.length}`);

    // Create agent with or without tools
    const agentOptions: CreateAgentOptions = {
        name: 'Modern Workplace Assistant',
        instructions: instructions
    };

    if (tools.length > 0) {
        agentOptions.tools = tools;
    }

    const agent = await projectClient.agents.createAgent(
        modelDeploymentName,
        agentOptions
    );

    console.log(`✅ Agent created successfully: ${agent.id}`);
    return agent;
    // </create_agent_with_tools>
}

interface BusinessScenario {
    title: string;
    question: string;
    context: string;
    learningPoint: string;
}

/**
 * Demonstrate realistic business scenarios with Agent SDK v2.
 * 
 * This function showcases the practical value of the Modern Workplace Assistant
 * by walking through scenarios that enterprise employees face regularly.
 * 
 * Educational Value:
 * - Shows real business problems that AI agents can solve
 * - Demonstrates proper thread and message management
 * - Illustrates Agent SDK v2 conversation patterns
 */
async function demonstrateBusinessScenarios(agent: Agent): Promise<void> {
    const scenarios: BusinessScenario[] = [
        {
            title: '📋 Company Policy Question (SharePoint Only)',
            question: "What is Contoso's remote work policy?",
            context: 'Employee needs to understand company-specific remote work requirements',
            learningPoint: 'SharePoint tool retrieves internal company policies'
        },
        {
            title: '📚 Technical Documentation Question (MCP Only)',
            question: 'According to Microsoft Learn, what is the correct way to implement Azure AD Conditional Access policies? Please include reference links to the official documentation.',
            context: 'IT administrator needs authoritative Microsoft technical guidance',
            learningPoint: 'MCP tool accesses Microsoft Learn for official documentation with links'
        },
        {
            title: '🔄 Combined Implementation Question (SharePoint + MCP)',
            question: "Based on our company's remote work security policy, how should I configure my Azure environment to comply? Please include links to Microsoft documentation showing how to implement each requirement.",
            context: 'Need to map company policy to technical implementation with official guidance',
            learningPoint: 'Both tools work together: SharePoint for policy + MCP for implementation docs'
        }
    ];

    console.log('\n' + '='.repeat(70));
    console.log('🏢 MODERN WORKPLACE ASSISTANT - BUSINESS SCENARIO DEMONSTRATION');
    console.log('='.repeat(70));
    console.log('This demonstration shows how AI agents solve real business problems');
    console.log('using the Azure AI Agents SDK v2.');
    console.log('='.repeat(70));

    for (let i = 0; i < scenarios.length; i++) {
        const scenario = scenarios[i];
        console.log(`\n📊 SCENARIO ${i + 1}/${scenarios.length}: ${scenario.title}`);
        console.log('-'.repeat(50));
        console.log(`❓ QUESTION: ${scenario.question}`);
        console.log(`🎯 BUSINESS CONTEXT: ${scenario.context}`);
        console.log(`🎓 LEARNING POINT: ${scenario.learningPoint}`);
        console.log('-'.repeat(50));

        // <agent_conversation>
        console.log('🤖 ASSISTANT RESPONSE:');
        const result = await chatWithAssistant(agent.id, scenario.question);
        // </agent_conversation>

        // Display response with analysis
        if (result.status === 'completed' && result.response && result.response.length > 10) {
            const preview = result.response.length > 300 
                ? result.response.substring(0, 300) + '...' 
                : result.response;
            console.log(`✅ SUCCESS: ${preview}`);
            if (result.response.length > 300) {
                console.log(`   📏 Full response: ${result.response.length} characters`);
            }
        } else {
            console.log(`⚠️  RESPONSE: ${result.response}`);
        }

        console.log(`📈 STATUS: ${result.status}`);
        console.log('-'.repeat(50));

        // Small delay between scenarios
        await new Promise(resolve => setTimeout(resolve, 1000));
    }

    console.log('\n✅ DEMONSTRATION COMPLETED!');
    console.log('🎓 Key Learning Outcomes:');
    console.log('   • Agent SDK v2 usage for enterprise AI');
    console.log('   • Proper thread and message management');
    console.log('   • Real business value through AI assistance');
    console.log('   • Foundation for governance and monitoring (Tutorials 2-3)');
}

interface ChatResult {
    response: string;
    status: string;
}

/**
 * Execute a conversation with the workplace assistant using Agent SDK v2.
 * 
 * This function demonstrates the conversation pattern for Azure AI Agents SDK v2
 * including MCP tool approval handling.
 * 
 * Educational Value:
 * - Shows proper conversation management with Agent SDK v2
 * - Demonstrates thread creation and message handling
 * - Illustrates MCP approval with auto-approval pattern
 * - Includes timeout and error management patterns
 */
async function chatWithAssistant(agentId: string, message: string): Promise<ChatResult> {
    try {
        // Create a thread for the conversation
        const thread = await projectClient.agents.threads.create();

        // Create a message in the thread
        await projectClient.agents.messages.create(
            thread.id,
            'user',
            message
        );

        // <mcp_approval_usage>
        // Create and process run with auto-approval for MCP tools
        // This is the recommended pattern for MCP tools in Agent SDK v2
        const runOptions: CreateAndProcessRunOptions = {
            autoApproveMcpTools: true // Auto-approve MCP tool calls
        };

        const run = await projectClient.agents.runs.createAndProcess(
            thread.id,
            agentId,
            runOptions
        );
        // </mcp_approval_usage>

        // Retrieve messages
        if (run.status === 'completed') {
            const messages = projectClient.agents.messages.list(thread.id);
            
            // Get the assistant's response (last message from assistant)
            const messageList: ThreadMessage[] = [];
            for await (const msg of messages) {
                messageList.push(msg);
            }

            for (let i = messageList.length - 1; i >= 0; i--) {
                const msg = messageList[i];
                if (msg.role === 'assistant' && msg.content && msg.content.length > 0) {
                    const textContent = msg.content[0];
                    if ('text' in textContent && textContent.text) {
                        return {
                            response: textContent.text.value,
                            status: 'completed'
                        };
                    }
                }
            }

            return { response: 'No response from assistant', status: 'completed' };
        } else {
            return { response: `Run ended with status: ${run.status}`, status: run.status };
        }

    } catch (error) {
        return { 
            response: `Error in conversation: ${error}`, 
            status: 'failed' 
        };
    }
}

/**
 * Interactive mode for testing the workplace assistant.
 * 
 * This provides a simple interface for users to test the agent with their own questions
 * and see how it provides comprehensive technical guidance.
 */
async function interactiveMode(agent: Agent): Promise<void> {
    console.log('\n' + '='.repeat(60));
    console.log('💬 INTERACTIVE MODE - Test Your Workplace Assistant!');
    console.log('='.repeat(60));
    console.log('Ask questions about Azure, M365, security, and technical implementation:');
    console.log("• 'How do I configure Azure AD conditional access?'");
    console.log("• 'What are MFA best practices for remote workers?'");
    console.log("• 'How do I set up secure SharePoint access?'");
    console.log("Type 'quit' to exit.");
    console.log('-'.repeat(60));

    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    const question = (prompt: string): Promise<string> => {
        return new Promise(resolve => {
            rl.question(prompt, resolve);
        });
    };

    while (true) {
        try {
            const userQuestion = await question('\n❓ Your question: ');
            const trimmedQuestion = userQuestion.trim();

            if (['quit', 'exit', 'bye'].includes(trimmedQuestion.toLowerCase())) {
                break;
            }

            if (!trimmedQuestion) {
                console.log('💡 Please ask a question about Azure or M365 technical implementation.');
                continue;
            }

            process.stdout.write('\n🤖 Workplace Assistant: ');
            const result = await chatWithAssistant(agent.id, trimmedQuestion);
            console.log(result.response);

            if (result.status !== 'completed') {
                console.log(`\n⚠️  Response status: ${result.status}`);
            }

            console.log('-'.repeat(60));

        } catch (error) {
            console.log(`\n❌ Error: ${error}`);
            console.log('-'.repeat(60));
        }
    }

    rl.close();
    console.log('\n👋 Thank you for testing the Modern Workplace Assistant!');
}

/**
 * Main execution flow demonstrating the complete sample.
 */
async function main(): Promise<void> {
    console.log('🚀 Azure AI Foundry - Modern Workplace Assistant');
    console.log('Tutorial 1: Building Enterprise Agents with Agent SDK v2');
    console.log('='.repeat(70));

    try {
        // Create the agent with full diagnostic output
        const agent = await createWorkplaceAssistant();

        // Demonstrate business scenarios
        await demonstrateBusinessScenarios(agent);

        // Offer interactive testing
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });

        const answer = await new Promise<string>(resolve => {
            rl.question('\n🎯 Try interactive mode? (y/n): ', resolve);
        });
        rl.close();

        if (answer.toLowerCase().startsWith('y')) {
            await interactiveMode(agent);
        }

        console.log('\n🎉 Sample completed successfully!');
        console.log('📚 This foundation supports Tutorial 2 (Governance) and Tutorial 3 (Production)');
        console.log('🔗 Next: Add evaluation metrics, monitoring, and production deployment');

    } catch (error) {
        console.log(`\n❌ Error: ${error}`);
        console.log('Please check your .env configuration and ensure:');
        console.log('  - PROJECT_ENDPOINT is correct');
        console.log('  - MODEL_DEPLOYMENT_NAME is deployed');
        console.log('  - Azure credentials are configured (az login)');
        throw error;
    }
}

// Run the main function
main().catch(console.error);
