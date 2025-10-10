import com.azure.ai.projects.AIProjectClient;
import com.azure.ai.projects.AIProjectClientBuilder;
import com.azure.ai.projects.models.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String projectEndpoint = dotenv.get("PROJECT_ENDPOINT");
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        String mcpServerUrl = dotenv.get("MCP_SERVER_URL", "https://learn.microsoft.com/api/mcp");
        
        AIProjectClient client = new AIProjectClientBuilder()
            .endpoint(projectEndpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
        
        System.out.println("🤖 Creating Modern Workplace Assistant...\n");
        
        boolean hasSharePoint = false;
        List<ToolDefinition> tools = new ArrayList<>();
        
        if (sharepointResourceName != null && !sharepointResourceName.isEmpty()) {
            try {
                ConnectionResponse connection = client.getConnection(sharepointResourceName);
                if (connection != null) {
                    tools.add(new SharePointToolDefinition(connection.getId()));
                    hasSharePoint = true;
                    System.out.println("✅ SharePoint connected: " + sharepointResourceName);
                }
            } catch (Exception e) {
                System.out.println("⚠️  SharePoint connection not found: " + e.getMessage());
                System.out.println("   Agent will operate in technical guidance mode only");
            }
        }
        
        System.out.println("📚 Configuring Microsoft Learn MCP integration...");
        tools.add(new McpToolDefinition("microsoft_learn", mcpServerUrl));
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
            "- For implementation questions: Combine both sources to show how company policies map to technical implementation" :
            "You are a Technical Assistant with access to Microsoft Learn documentation.\n\n" +
            "CAPABILITIES:\n" +
            "- Access Microsoft Learn for current Azure and Microsoft 365 technical guidance\n" +
            "- Provide detailed implementation steps and best practices\n\n" +
            "LIMITATIONS:\n" +
            "- SharePoint integration is not available\n" +
            "- Cannot access company-specific policies";
        
        System.out.println("\n🛠️  Creating agent with " + tools.size() + " tool(s)...");
        
        Agent agent = client.createAgent(
            modelDeploymentName,
            new AgentCreationOptions()
                .setName("Modern Workplace Assistant")
                .setInstructions(instructions)
                .setTools(tools)
        );
        
        System.out.println("✅ Agent created: " + agent.getId() + "\n");
        
        String[][] scenarios = {
            {"📋 Policy Question", "What is our remote work policy regarding security requirements?"},
            {"🔧 Technical Question", "How do I set up Azure Active Directory conditional access?"},
            {"🔄 Implementation Question", "Our security policy requires MFA - how do I implement this in Azure AD?"}
        };
        
        for (int i = 0; i < scenarios.length; i++) {
            String title = scenarios[i][0];
            String question = scenarios[i][1];
            
            System.out.println(title + " " + (i + 1) + "/" + scenarios.length);
            System.out.println("❓ " + question);
            
            AgentThread thread = client.createThread();
            client.createMessage(thread.getId(), MessageRole.USER, question);
            
            ThreadRun run = client.createRun(thread.getId(), agent.getId());
            
            while (run.getStatus() == RunStatus.QUEUED || run.getStatus() == RunStatus.IN_PROGRESS) {
                TimeUnit.MILLISECONDS.sleep(1000);
                run = client.getRun(thread.getId(), run.getId());
            }
            
            if (run.getStatus() == RunStatus.COMPLETED) {
                List<ThreadMessage> messages = client.listMessages(thread.getId(), 
                    new ListMessagesOptions().setOrder(ListSortOrder.DESCENDING));
                
                for (ThreadMessage message : messages) {
                    if (message.getRole() == MessageRole.ASSISTANT) {
                        for (MessageContent content : message.getContentItems()) {
                            if (content instanceof MessageTextContent) {
                                System.out.println("🤖 " + ((MessageTextContent) content).getText() + "\n");
                                break;
                            }
                        }
                        break;
                    }
                }
            } else {
                System.out.println("❌ Run failed with status: " + run.getStatus() + "\n");
            }
            
            client.deleteThread(thread.getId());
        }
        
        System.out.println("\n💡 Interactive Mode");
        System.out.println("The agent is ready. In a production scenario, you would integrate this with your application's user interface.");
        System.out.println("Users could ask questions combining company policies with technical implementation guidance.\n");
        
        client.deleteAgent(agent.getId());
    }
}
