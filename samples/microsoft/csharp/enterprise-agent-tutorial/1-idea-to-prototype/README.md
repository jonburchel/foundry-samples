# Azure AI Foundry - Modern Workplace Assistant (C#)

**Tutorial 1** of the Azure AI Foundry enterprise tutorial series. This sample demonstrates how to build AI agents that combine internal knowledge (SharePoint) with external technical guidance (Microsoft Learn) for realistic business scenarios.

> **🚀 Preview SDK**: This sample uses preview versions of the Azure AI SDK. These features will be GA at Microsoft Ignite.

## 🎯 Business Scenario: Modern Workplace Assistant

This sample creates an AI assistant that helps employees with:

- **Company policies** (from SharePoint documents)
- **Technical implementation** (from Microsoft Learn)
- **Complete solutions** (combining both sources)

**Example Questions:**

- "What is our remote work security policy?" → *Uses SharePoint*
- "How do I set up Azure AD conditional access?" → *Uses Microsoft Learn*  
- "Our policy requires MFA - how do I implement it in Azure?" → *Uses both sources*

## ⚡ Quick Start

### 1. Run the Main Sample

```bash
dotnet run
```

This demonstrates the core functionality with sample business scenarios.

### 2. Run Evaluation

```bash
dotnet run --project Evaluate.csproj
```

Tests the agent with predefined questions and measures quality.

## 📁 Ultra-Minimal Sample Structure

This sample contains only **10 essential files** - nothing extraneous:

### Core Sample (3 files)

- **`Program.cs`** - Complete Modern Workplace Assistant implementation
- **`Evaluate.cs`** - Business evaluation framework
- **`questions.jsonl`** - Business test scenarios (4 questions)

### Setup & Documentation (7 files)

- **`ModernWorkplaceAssistant.csproj`** - .NET project file
- **`.env.template`** - Environment variables template
- **`MCP_SERVERS.md`** - MCP server configuration guide
- **`SAMPLE_SHAREPOINT_CONTENT.md`** - Sample business documents
- **`README.md`** - Complete setup instructions
- **`.env`** - Your actual configuration (create from template)

## 📁 SharePoint Business Documents Setup

To demonstrate the complete business scenario, you need to upload sample documents to your SharePoint site. The sample includes realistic Contoso Corp business documents that create scenarios where employees need both company policy information and technical implementation guidance.

### Step 1: Prepare Your SharePoint Site

1. **Navigate to your SharePoint site** (the one configured in your Azure AI Foundry SharePoint connection)
2. **Create or use a document library** called "Company Policies" or use the default "Documents" library
3. **Ensure you have edit permissions** to upload documents

### Step 2: Create Sample Business Documents

The `SAMPLE_SHAREPOINT_CONTENT.md` file contains four realistic business documents. Create these as Word documents (.docx) in your SharePoint site:

#### 📄 Document 1: `remote-work-policy.docx`

**Content**: Remote work security requirements including VPN usage, MFA requirements, device compliance, and data access policies. References Azure AD and Microsoft 365 security features.

#### 📄 Document 2: `security-guidelines.docx`

**Content**: Azure security standards including conditional access policies, identity governance, and compliance requirements. Establishes company standards for Azure resource security.

#### 📄 Document 3: `collaboration-standards.docx`

**Content**: Microsoft Teams and SharePoint usage policies, including data sharing guidelines, external collaboration rules, and communication standards.

#### 📄 Document 4: `data-governance-policy.docx`

**Content**: Data classification, retention policies, and governance requirements for Azure and Microsoft 365 data. Includes sensitivity labels and compliance procedures.

### Step 3: Upload Documents to SharePoint

1. **For each document in `SAMPLE_SHAREPOINT_CONTENT.md`**:
   - Create a new Word document in SharePoint
   - Copy the content from the corresponding section  
   - Save with the specified filename (e.g., `remote-work-policy.docx`)

2. **Verify document access**:
   - Ensure documents are searchable
   - Check that your Azure AI Foundry connection can access the site
   - Test that documents appear in SharePoint search results

### Why These Documents Matter

These sample documents create realistic business scenarios:

- **"What is our remote work security policy?"** → Searches `remote-work-policy.docx`
- **"How do I set up Azure AD conditional access?"** → Uses Microsoft Learn MCP
- **"Our policy requires MFA - how do I implement it in Azure?"** → Combines both sources

This demonstrates how modern workplace assistants help employees by connecting company policies with technical implementation guidance.

## 🚀 Quick Start (5 minutes)

### Step 1: Prerequisites Check

Make sure you have:

- [x] **Azure AI Foundry project** with a deployed model (e.g., `gpt-4o-mini`)
- [x] **.NET 8.0+** installed (`dotnet --version`)
- [x] **SharePoint connection** configured in your Azure AI Foundry project
- [x] **MCP server endpoint** (or use a placeholder for testing)
- [x] **Azure CLI** authenticated (`az login`)

### Step 2: Environment Setup

1. **Copy the environment template:**

   ```bash
   cp .env.template .env
   ```

2. **Edit `.env` with your actual values:**

   ```bash
   PROJECT_ENDPOINT=https://your-project.aiservices.azure.com
   MODEL_DEPLOYMENT_NAME=gpt-4o-mini
   SHAREPOINT_RESOURCE_NAME=your-sharepoint-connection
   MCP_SERVER_URL=https://your-mcp-server.com
   ```

3. **Restore dependencies:**

   ```bash
   dotnet restore
   ```

### Step 3: Run the Main Sample

```bash
dotnet run
```

**Expected output:**

```text
Created agent: <agent-id>
SharePoint Response: <response about your remote work policy>
```

> **Note**: You'll get a "Resource not found" error until you configure actual SharePoint and MCP connections in Azure AI Foundry.

### Step 4: Run Evaluation

Evaluate your agent with test questions:

```bash
dotnet run --project Evaluate.csproj
```

**Expected output:**

```json
Evaluation: 3/4 passed
[
  {
    "question": "What's our remote work policy?",
    "response": "According to SharePoint...",
    "contains_expected": true
  }
  ...
]
```

## 🔧 Troubleshooting

### Common Issues

#### Authentication failed

**Solution**: Ensure Azure CLI is authenticated:

```bash
az login
az account show
```

#### Package/NuGet errors

**Solution**: Clean and restore dependencies:

```bash
dotnet clean
dotnet restore
dotnet build
```

#### Cannot find deployment

**Solution**: Verify your model deployment name in Azure AI Foundry portal and update `.env` file.

#### SharePoint connection not found

**Solution**: Check that your SharePoint connection name in `.env` matches exactly with the connection in Azure AI Foundry.

## 📦 Dependencies

This sample uses the following Azure SDKs:

- **Azure.AI.Projects**: Azure AI Foundry SDK for .NET
- **Azure.AI.Agents.Persistent**: Azure AI Agents SDK
- **Azure.Identity**: Azure authentication
- **DotNetEnv**: Environment variable management

## 📚 Learn More

- [Azure AI Foundry Documentation](https://learn.microsoft.com/azure/ai-foundry/)
- [.NET SDK Reference](https://learn.microsoft.com/dotnet/api/azure.ai.projects)
- [Tutorial Series Overview](../../../enterprise-agent-tutorial/README.md)

## 💡 What's Next?

Great work! You've built a modern workplace assistant that combines internal and external knowledge. Here are some ideas to extend this sample:

- 🔍 **Add more data sources**: Connect to databases, file shares, or other APIs
- 🎨 **Build a UI**: Create a web or mobile interface for your assistant with Blazor or ASP.NET Core
- 🔐 **Implement role-based access**: Filter SharePoint results based on user permissions
- 📊 **Add telemetry**: Track agent performance and user satisfaction with Application Insights
- 🌐 **Multi-language support**: Extend the assistant to handle multiple languages

Keep building amazing AI experiences! 🚀
