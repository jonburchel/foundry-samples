import com.azure.ai.agents.persistent.PersistentAgentsClient;
import com.azure.ai.agents.persistent.PersistentAgentsClientBuilder;
import com.azure.ai.agents.persistent.PersistentAgentsAdministrationClient;
import com.azure.ai.agents.persistent.models.*;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Modern Workplace Assistant - Java Implementation
 * 
 * Note: SharePoint and MCP tool integration are not yet supported in the Java SDK (azure-ai-agents-persistent v1.0.0-beta.2).
 * This sample demonstrates the agent framework without these integrations.
 * For full SharePoint and MCP functionality, use the Python or C# implementations.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String projectEndpoint = dotenv.get("PROJECT_ENDPOINT");
        String modelDeploymentName = dotenv.get("MODEL_DEPLOYMENT_NAME");
        String sharepointResourceName = dotenv.get("SHAREPOINT_RESOURCE_NAME");
        
        System.out.println("🤖 Creating Modern Workplace Assistant...\n");
        
        // Note: SharePoint and MCP tools not yet available in Java SDK v1.0.0-beta.2
        if (sharepointResourceName != null && !sharepointResourceName.isEmpty()) {
            System.out.println("⚠️  SharePoint grounding not yet supported in Java SDK v1.0.0-beta.2");
            System.out.println("   Connection '" + sharepointResourceName + "' configured but cannot be used");
            System.out.println("   Agent will operate in general assistance mode");
            System.out.println("   For full SharePoint integration, use Python or C# implementations\n");
        }
        
        PersistentAgentsClient agentsClient = new PersistentAgentsClientBuilder()
            .endpoint(projectEndpoint)
            .credential(new DefaultAzureCredentialBuilder().build())
            .buildClient();
            
        PersistentAgentsAdministrationClient adminClient = 
            agentsClient.getPersistentAgentsAdministrationClient();
        
        String instructions = 
            "You are a helpful Technical Assistant specializing in Azure and Microsoft 365.\n\n" +
            "CAPABILITIES:\n" +
            "- Provide detailed technical guidance on Azure and Microsoft 365 services\n" +
            "- Explain implementation steps and best practices\n" +
            "- Answer questions about security, identity, and cloud architecture\n\n" +
            "NOTE: This Java SDK version does not yet support SharePoint or MCP tool integration.\n" +
            "For full enterprise features including SharePoint grounding, use Python or C# SDKs.";
        
        System.out.println("🛠️  Creating agent...");
        
        PersistentAgent agent = adminClient.createAgent(
            new CreateAgentOptions(modelDeploymentName)
                .setName("Modern Workplace Assistant (Java)")
                .setInstructions(instructions)
        );
        
        System.out.println("✅ Agent created: " + agent.getId());
        System.out.println("📝 Model: " + agent.getModel() + "\n");
        
        String[][] scenarios = {
            {"📋 General Question", "What are best practices for securing remote work environments?"},
            {"🔧 Technical Question", "How do I set up Azure Active Directory conditional access?"},
            {"🔄 Implementation Question", "What are the steps to implement MFA in Azure AD?"}
        };
        
        for (int i = 0; i < scenarios.length; i++) {
            String title = scenarios[i][0];
            String question = scenarios[i][1];
            
            System.out.println(title + " " + (i + 1) + "/" + scenarios.length);
            System.out.println("❓ " + question);
            
            AgentThread thread = adminClient.createThread();
            adminClient.createMessage(thread.getId(), MessageRole.USER, question);
            
            ThreadRun run = agentsClient.createRun(thread.getId(), agent.getId());
            
            while (run.getStatus() == RunStatus.QUEUED || run.getStatus() == RunStatus.IN_PROGRESS) {
                TimeUnit.MILLISECONDS.sleep(1000);
                run = agentsClient.getRun(thread.getId(), run.getId());
            }
            
            if (run.getStatus() == RunStatus.COMPLETED) {
                PagedIterable<ThreadMessage> messages = adminClient.listMessages(thread.getId());
                
                for (ThreadMessage message : messages) {
                    if (message.getRole() == MessageRole.ASSISTANT) {
                        List<MessageContent> contents = message.getContentItems();
                        if (!contents.isEmpty() && contents.get(0) instanceof MessageTextContent) {
                            MessageTextContent textContent = (MessageTextContent) contents.get(0);
                            System.out.println("🤖 " + textContent.getText().getValue() + "\n");
                        }
                        break;
                    }
                }
            } else {
                System.out.println("❌ Run failed with status: " + run.getStatus() + "\n");
            }
            
            adminClient.deleteThread(thread.getId());
        }
        
        System.out.println("\n💡 Interactive Mode");
        System.out.println("The agent is ready for general technical guidance.");
        System.out.println("Note: For SharePoint and MCP integration, use Python or C# implementations.\n");
        
        adminClient.deleteAgent(agent.getId());
        System.out.println("✅ All scenarios completed successfully");
    }
}
