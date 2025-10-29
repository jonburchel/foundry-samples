/**
 * Azure AI Foundry Agent Sample - Tutorial 1: Modern Workplace Assistant
 *
 * This sample demonstrates a complete business scenario using Azure AI Agents SDK v2:
 * - Agent creation with the new SDK
 * - Conversation and response management
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
import { AgentsClient } from '@azure/ai-agents-ii';
import { DefaultAzureCredential, getBearerTokenProvider } from '@azure/identity';
import OpenAI from 'openai';
import { config } from 'dotenv';
import * as readline from 'readline';
// </imports_and_includes>
config({ path: '.env' });
// ============================================================================
// AUTHENTICATION SETUP
// ============================================================================
// <agent_authentication>
const credential = new DefaultAzureCredential();
const projectEndpoint = process.env.PROJECT_ENDPOINT;
const modelDeploymentName = process.env.MODEL_DEPLOYMENT_NAME;
const agentsClient = new AgentsClient(projectEndpoint, credential, {
    apiVersion: '2025-05-15-preview'
});
console.log(`✅ Connected to Azure AI Foundry: ${projectEndpoint}`);
// </agent_authentication>
// Create OpenAI client for conversations and responses
const scope = 'https://ai.azure.com/.default';
let openAIClient;
async function initializeOpenAIClient() {
    const azureADTokenProvider = getBearerTokenProvider(credential, scope);
    openAIClient = new OpenAI({
        apiKey: azureADTokenProvider,
        baseURL: `${projectEndpoint}/openai`,
        defaultQuery: { 'api-version': '2025-05-15-preview' }
    });
}
/**
 * Create a Modern Workplace Assistant using Agent SDK v2.
 *
 * This demonstrates enterprise AI patterns:
 * 1. Agent creation with the new SDK
 * 2. Robust error handling with graceful degradation
 * 3. Clear diagnostic information for troubleshooting
 *
 * Educational Value:
 * - Shows real-world complexity of enterprise AI systems
 * - Demonstrates how to handle partial system failures
 * - Provides patterns for agent creation with Agent SDK v2
 *
 * Note: Tool integration (SharePoint, MCP) is being explored for SDK v2 beta.
 * This version demonstrates the core agent functionality.
 */
async function createWorkplaceAssistant() {
    console.log('\n🤖 Creating Modern Workplace Assistant...');
    // ========================================================================
    // AGENT CREATION
    // ========================================================================
    const instructions = `You are a Technical Assistant specializing in Azure and Microsoft 365 guidance.

CAPABILITIES:
- Provide detailed Azure and Microsoft 365 technical guidance
- Explain implementation steps and best practices
- Help with Azure AD, Conditional Access, MFA, and security configurations

RESPONSE STRATEGY:
- Provide comprehensive technical guidance
- Include step-by-step implementation instructions
- Reference best practices and security considerations
- For policy questions, explain common enterprise policies and how to implement them
- For technical questions, provide detailed Azure/M365 implementation steps

EXAMPLE SCENARIOS:
- "What is a typical enterprise MFA policy?" → Explain common MFA policies and their implementation
- "How do I configure Azure AD Conditional Access?" → Provide detailed technical steps
- "What are the best practices for remote work security?" → Combine policy recommendations with implementation guidance`;
    // <create_agent>
    console.log(`🛠️  Creating agent with model: ${modelDeploymentName}`);
    const agent = await agentsClient.createVersion('Modern_Workplace_Assistant', {
        kind: 'prompt',
        model: modelDeploymentName,
        instructions: instructions
    });
    console.log('✅ Agent created successfully');
    console.log(`   Agent ID: ${agent.id}`);
    console.log(`   Agent Name: ${agent.name}`);
    console.log(`   Agent Version: ${agent.version}`);
    console.log('\n⚠️  Note: SDK v2 beta status:');
    console.log('   - Core agent conversation functionality working');
    console.log('   - Tool integration patterns being finalized');
    return agent;
    // </create_agent>
}
/**
 * Demonstrate realistic business scenarios with Agent SDK v2.
 *
 * This function showcases the practical value of the Modern Workplace Assistant
 * by walking through scenarios that enterprise employees face regularly.
 *
 * Educational Value:
 * - Shows real business problems that AI agents can solve
 * - Demonstrates proper conversation and response management
 * - Illustrates Agent SDK v2 conversation patterns
 */
async function demonstrateBusinessScenarios(agent) {
    const scenarios = [
        {
            title: '📋 Enterprise Policy Question',
            question: 'What is a typical enterprise remote work policy for security?',
            context: 'Employee needs to understand common enterprise remote work requirements',
            learningPoint: 'Agent provides general guidance on enterprise policies'
        },
        {
            title: '📚 Technical Documentation Question',
            question: 'What is the correct way to implement Azure AD Conditional Access policies?',
            context: 'IT administrator needs technical implementation guidance',
            learningPoint: 'Agent provides detailed Azure technical implementation steps'
        },
        {
            title: '🔄 Combined Implementation Question',
            question: 'How should I configure my Azure environment for secure remote work with MFA?',
            context: 'Need practical implementation combining security best practices',
            learningPoint: 'Agent combines policy guidance with technical implementation'
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
        const result = await chatWithAssistant(agent, scenario.question);
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
        }
        else {
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
    console.log('   • Proper conversation and response management');
    console.log('   • Real business value through AI assistance');
    console.log('   • Foundation for governance and monitoring (Tutorials 2-3)');
}
/**
 * Execute a conversation with the workplace assistant using Agent SDK v2.
 *
 * This function demonstrates the conversation pattern for Azure AI Agents SDK v2.
 *
 * Educational Value:
 * - Shows proper conversation management with Agent SDK v2
 * - Demonstrates conversation creation and message handling
 * - Includes error management patterns
 */
async function chatWithAssistant(agent, message) {
    try {
        // <create_conversation>
        // Create a conversation with initial message
        const conversation = await openAIClient.conversations.create({
            items: [
                {
                    type: 'message',
                    role: 'user',
                    content: message
                }
            ]
        });
        // </create_conversation>
        // <create_response>
        // Create response using the agent
        const response = await openAIClient.responses.create({
            conversation: conversation.id
        }, {
            headers: {
                'accept-encoding': 'deflate'
            },
            body: {
                agent: {
                    type: 'agent_reference',
                    name: agent.name
                }
            }
        });
        // </create_response>
        // Extract response text
        if (response && response.output_text) {
            return {
                response: response.output_text,
                status: 'completed'
            };
        }
        return { response: 'No response from assistant', status: 'completed' };
    }
    catch (error) {
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
async function interactiveMode(agent) {
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
    const question = (prompt) => {
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
            const result = await chatWithAssistant(agent, trimmedQuestion);
            console.log(result.response);
            if (result.status !== 'completed') {
                console.log(`\n⚠️  Response status: ${result.status}`);
            }
            console.log('-'.repeat(60));
        }
        catch (error) {
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
async function main() {
    console.log('🚀 Azure AI Foundry - Modern Workplace Assistant');
    console.log('Tutorial 1: Building Enterprise Agents with Agent SDK v2');
    console.log('='.repeat(70));
    try {
        // Initialize OpenAI client
        await initializeOpenAIClient();
        // Create the agent with full diagnostic output
        const agent = await createWorkplaceAssistant();
        // Demonstrate business scenarios
        await demonstrateBusinessScenarios(agent);
        // Offer interactive testing
        const rl = readline.createInterface({
            input: process.stdin,
            output: process.stdout
        });
        const answer = await new Promise(resolve => {
            rl.question('\n🎯 Try interactive mode? (y/n): ', resolve);
        });
        rl.close();
        if (answer.toLowerCase().startsWith('y')) {
            await interactiveMode(agent);
        }
        console.log('\n🎉 Sample completed successfully!');
        console.log('📚 This foundation supports Tutorial 2 (Governance) and Tutorial 3 (Production)');
        console.log('🔗 Next: Add evaluation metrics, monitoring, and production deployment');
    }
    catch (error) {
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
//# sourceMappingURL=main.js.map