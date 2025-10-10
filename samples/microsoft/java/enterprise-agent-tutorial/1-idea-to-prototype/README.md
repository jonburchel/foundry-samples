# Modern Workplace Assistant - Java Implementation

Enterprise agent tutorial demonstrating SharePoint and MCP integration patterns using Java.

## Prerequisites

- Java 17 or later
- Maven 3.8 or later
- Azure AI Foundry project with deployed model
- Azure CLI (`az login`)
- SharePoint connection (optional)
- Access to Microsoft Learn MCP server

## Setup

1. **Install Java & Maven** (if not already installed):
   ```bash
   # Download from https://adoptium.net/ and https://maven.apache.org/
   ```

2. **Configure environment**:
   ```bash
   cp .env.template .env
   # Edit .env with your Azure AI Foundry project details
   ```

3. **Build project**:
   ```bash
   mvn clean compile
   ```

## Environment Variables

Required in `.env` file:

```bash
PROJECT_ENDPOINT=https://<your-project>.aiservices.azure.com
MODEL_DEPLOYMENT_NAME=gpt-4o-mini
AI_FOUNDRY_TENANT_ID=<your-tenant-id>  # Optional
MCP_SERVER_URL=https://learn.microsoft.com/api/mcp
SHAREPOINT_RESOURCE_NAME=your-sharepoint-connection  # Optional
SHAREPOINT_SITE_URL=https://your-company.sharepoint.com/teams/your-site  # Optional
```

## Running the Sample

### Main Application

```bash
mvn exec:java -Dexec.mainClass="Main"
```

This demonstrates:
- Agent creation with dynamic capabilities
- Three business scenarios (policy, technical, combined)
- Graceful degradation when services unavailable
- Interactive testing mode

### Evaluation

```bash
mvn exec:java -Dexec.mainClass="Evaluate"
```

This runs batch evaluation against test questions and generates `evaluation_results.json`.

## Project Structure

```
java/enterprise-agent-tutorial/1-idea-to-prototype/
├── Main.java                 # Main agent implementation
├── Evaluate.java            # Evaluation framework  
├── pom.xml                  # Maven project file
├── questions.jsonl          # Test questions
├── .env.template            # Environment template
├── README.md                # This file
├── MCP_SERVERS.md          # MCP server options
└── SAMPLE_SHAREPOINT_CONTENT.md  # Sample policies
```

## Dependencies

- **azure-ai-projects** (1.0.0-beta.1): Azure AI Foundry SDK
- **azure-identity** (1.13.1): Azure authentication
- **dotenv-java** (3.0.0): Environment variable management
- **gson** (2.10.1): JSON processing

## Troubleshooting

### Build Errors

If Maven dependencies fail to download:
```bash
mvn dependency:purge-local-repository
mvn clean install
```

### Runtime Errors

1. **Authentication**: Ensure `az login` is successful
2. **Environment**: Verify all required variables in `.env`
3. **Endpoint**: Check PROJECT_ENDPOINT format (https://...)
4. **Model**: Confirm MODEL_DEPLOYMENT_NAME matches your deployment

## Notes

- This sample uses preview SDK features (beta.1)
- SharePoint/MCP tool classes pending SDK release
- Current implementation uses inline tool definitions
- Will be updated with full SDK support post-Ignite

## Learn More

- [Azure AI Foundry Agent Documentation](../../developer-journey-stage-1-idea-to-prototype.md)
- [Java SDK Reference](https://learn.microsoft.com/java/api/overview/azure/ai-projects-readme)
- [Tutorial Series Overview](../../../enterprise-agent-tutorial/README.md)
