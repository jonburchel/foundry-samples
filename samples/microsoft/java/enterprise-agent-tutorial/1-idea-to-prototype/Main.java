/**
 * Azure AI Foundry Agent Sample - Tutorial 1: Modern Workplace Assistant
 * 
 * This sample demonstrates enterprise agent patterns using Java.
 * Combines SharePoint and MCP integration for real business scenarios.
 */

import com.azure.ai.projects.AIProjectClient;
import com.azure.ai.projects.AIProjectClientBuilder;
import com.azure.ai.projects.models.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    private static AIProjectClient projectClient;
    private static Dotenv dotenv;
    
    static {
        // Load environment variables
        dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String aiFoundryTenantId = dotenv.get("AI_FOUNDRY_TENANT_ID");
        if (aiFoundryTenantId != null && !aiFoundryTenantId.isEmpty()) {
            System.out.println("🔐 Using AI Foundry tenant: " + aiFoundryTenantId);
        }
        
        String projectEndpoint = dotenv.get("PROJECT_ENDPOINT");
        projectClient = new AIProjectClientBuilder()
            .endpoint(projectEndpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
    }
    
    static class AgentConfiguration {
        Agent agent;
        boolean hasSharePoint;
        
        AgentConfiguration(Agent agent, boolean hasSharePoint) {
            this.agent = agent;
            this.hasSharePoint = hasSharePoint;
        }
    }
    
    static class BusinessScenario {
        String title;
        String question;
        String context;
        String expectedSource;
        String learningPoint;
        
        BusinessScenario(String title, String question, String context, String expectedSource, String learningPoint) {
            this.title = title;
            this.question = question;
            this.context = context;
            this.expectedSource = expectedSource;
            this.learningPoint = learningPoint;
        }
    }
    
    static class ChatResponse {
        String response;
        String status;
        
        ChatResponse(String response, String status) {
            this.response = response;
            this.status = status;
        }
    }
    
    static AgentConfiguration createWorkplaceAssistant() {
        System.out.println("🤖 Creating Modern Workplace Assistant...");
        
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL");
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        
        System.out.println("📁 Configuring SharePoint integration...");
        System.out.println("   Connection: " + sharepointResourceName);
        
        boolean hasSharePoint = false;
        List<Map<String, Object>> tools = new ArrayList<>();
        
        try {
            // Try to get SharePoint connection
            var connection = projectClient.getConnection(sharepointResourceName);
            if (connection != null) {
                Map<String, Object> sharepointTool = new HashMap<>();
                sharepointTool.put("type", "sharepoint");
                sharepointTool.put("connection_id", connection.getId());
                tools.add(sharepointTool);
                hasSharePoint = true;
                System.out.println("✅ SharePoint successfully connected");
            }
        } catch (Exception e) {
            System.out.println("⚠️  SharePoint connection failed: " + e.getMessage());
            System.out.println("   Agent will operate in technical guidance mode only");
        }
        
        System.out.println("📚 Configuring Microsoft Learn MCP integration...");
        Map<String, Object> mcpTool = new HashMap<>();
        mcpTool.put("type", "mcp");
        mcpTool.put("server_label", "microsoft_learn");
        mcpTool.put("server_url", mcpServerUrl);
        mcpTool.put("require_approval", "never");
        tools.add(mcpTool);
        System.out.println("✅ Microsoft Learn MCP connected: " + mcpServerUrl);
        
        String instructions = hasSharePoint ?
            "You are a Modern Workplace Assistant for Contoso Corporation.\n\n" +
            "CAPABILITIES:\n" +
            "- Search SharePoint for company policies, procedures, and internal documentation\n" +
            "- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance\n" +
            "- Provide comprehensive solutions combining internal requirements with external implementation\n\n" +
            "RESPONSE STRATEGY:\n" +
            "- For policy questions: Search SharePoint for company-specific requirements\n" +
            "- For technical questions: Use Microsoft Learn for Azure/M365 documentation\n" +
            "- For implementation questions: Combine both sources\n" +
            "- Always cite sources and provide step-by-step guidance" :
            "You are a Technical Assistant with access to Microsoft Learn documentation.\n\n" +
            "CAPABILITIES:\n" +
            "- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance\n" +
            "- Provide detailed implementation steps and best practices\n\n" +
            "LIMITATIONS:\n" +
            "- SharePoint integration is not available\n" +
            "- Cannot access company-specific policies\n\n" +
            "RESPONSE STRATEGY:\n" +
            "- Provide comprehensive technical guidance from Microsoft Learn\n" +
            "- Include step-by-step implementation instructions";
        
        System.out.println("🛠️  Configuring agent tools...");
        System.out.println("   Available tools: " + tools.size());
        
        Agent agent = projectClient.createAgent(
            modelDeploymentName,
            "Modern Workplace Assistant",
            instructions,
            tools
        );
        
        System.out.println("✅ Agent created successfully: " + agent.getId());
        return new AgentConfiguration(agent, hasSharePoint);
    }
    
    static void demonstrateBusinessScenarios(AgentConfiguration config) throws InterruptedException {
        BusinessScenario[] scenarios = {
            new BusinessScenario(
                "📋 Company Policy Question",
                "What is our remote work security policy regarding multi-factor authentication?",
                "Employee needs to understand company MFA requirements",
                "SharePoint",
                "Internal policy retrieval and interpretation"
            ),
            new BusinessScenario(
                "🔧 Technical Implementation Question",
                "How do I set up Azure Active Directory conditional access policies?",
                "IT administrator needs technical implementation steps",
                "Microsoft Learn MCP",
                "External technical documentation access"
            ),
            new BusinessScenario(
                "🔄 Combined Business Implementation Question",
                "Our company security policy requires multi-factor authentication for remote workers. How do I implement this requirement using Azure AD?",
                "Need to combine policy requirements with technical implementation",
                "Both SharePoint and MCP",
                "Multi-source intelligence combining internal requirements with external implementation"
            )
        };
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🏢 MODERN WORKPLACE ASSISTANT - BUSINESS SCENARIO DEMONSTRATION");
        System.out.println("=".repeat(70));
        System.out.println("This demonstration shows how AI agents solve real business problems");
        System.out.println("by combining internal company knowledge with external technical guidance.");
        System.out.println("=".repeat(70));
        
        for (int i = 0; i < scenarios.length; i++) {
            BusinessScenario scenario = scenarios[i];
            System.out.println("\n📊 SCENARIO " + (i + 1) + "/3: " + scenario.title);
            System.out.println("-".repeat(50));
            System.out.println("❓ QUESTION: " + scenario.question);
            System.out.println("🎯 BUSINESS CONTEXT: " + scenario.context);
            System.out.println("📚 EXPECTED SOURCE: " + scenario.expectedSource);
            System.out.println("🎓 LEARNING POINT: " + scenario.learningPoint);
            System.out.println("-".repeat(50));
            
            System.out.println("🤖 ASSISTANT RESPONSE:");
            ChatResponse response = chatWithAssistant(config.agent.getId(), scenario.question);
            
            if ("completed".equals(response.status) && response.response != null && 
                response.response.trim().length() > 10) {
                String preview = response.response.length() > 300 ? 
                    response.response.substring(0, 300) + "..." : response.response;
                System.out.println("✅ SUCCESS: " + preview);
                if (response.response.length() > 300) {
                    System.out.println("   📏 Full response: " + response.response.length() + " characters");
                }
            } else {
                System.out.println("⚠️  LIMITED RESPONSE: " + response.response);
                if (!config.hasSharePoint && scenario.expectedSource.contains("SharePoint")) {
                    System.out.println("   💡 This demonstrates graceful degradation when SharePoint is unavailable");
                }
            }
            
            System.out.println("📈 STATUS: " + response.status);
            System.out.println("-".repeat(50));
        }
        
        System.out.println("\n✅ DEMONSTRATION COMPLETED!");
        System.out.println("🎓 Key Learning Outcomes:");
        System.out.println("   • Multi-source data integration in enterprise AI");
        System.out.println("   • Robust error handling and graceful degradation");
        System.out.println("   • Real business value through combined intelligence");
        System.out.println("   • Foundation for governance and monitoring (Tutorials 2-3)");
    }
    
    static ChatResponse chatWithAssistant(String agentId, String message) throws InterruptedException {
        try {
            AgentThread thread = projectClient.createThread();
            projectClient.createMessage(thread.getId(), "user", message);
            ThreadRun run = projectClient.createRun(thread.getId(), agentId);
            
            // Poll for completion
            while ("queued".equals(run.getStatus()) || 
                   "in_progress".equals(run.getStatus()) ||
                   "requires_action".equals(run.getStatus())) {
                TimeUnit.MILLISECONDS.sleep(500);
                run = projectClient.getRun(thread.getId(), run.getId());
            }
            
            List<ThreadMessage> messages = projectClient.listMessages(thread.getId());
            StringBuilder responseParts = new StringBuilder();
            
            for (ThreadMessage msg : messages) {
                if ("assistant".equals(msg.getRole())) {
                    for (MessageContent content : msg.getContent()) {
                        if (content instanceof MessageTextContent) {
                            responseParts.append(((MessageTextContent) content).getText());
                        }
                    }
                }
            }
            
            return new ChatResponse(responseParts.toString(), run.getStatus());
        } catch (Exception e) {
            return new ChatResponse("Error in conversation: " + e.getMessage(), "failed");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("🚀 Azure AI Foundry - Modern Workplace Assistant");
        System.out.println("Tutorial 1: Building Enterprise Agents with SharePoint + MCP Integration");
        System.out.println("=".repeat(70));
        
        try {
            AgentConfiguration agentConfig = createWorkplaceAssistant();
            demonstrateBusinessScenarios(agentConfig);
            
            System.out.println("\n🎉 Sample completed successfully!");
            System.out.println("📚 This foundation supports Tutorial 2 (Governance) and Tutorial 3 (Production)");
            System.out.println("🔗 Next: Add evaluation metrics, monitoring, and production deployment");
        } catch (Exception e) {
            System.err.println("❌ Sample failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
