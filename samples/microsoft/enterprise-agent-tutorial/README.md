# Enterprise Agent Tutorial Series

This tutorial series demonstrates building enterprise-grade AI agents with Azure AI Foundry, progressing from prototype to production deployment.

## Tutorial Structure

### Stage 1: Idea to Prototype
**Location**: `1-idea-to-prototype/`

Build a Modern Workplace Assistant that combines:
- SharePoint integration for internal company knowledge
- Microsoft Learn MCP integration for external technical guidance
- Batch evaluation framework for testing
- Graceful degradation patterns

### Stage 2: Governance and Monitoring (Coming Soon)
**Location**: `2-governance/`

Add enterprise governance:
- Content filtering and safety guardrails
- Comprehensive evaluation metrics
- Continuous evaluation pipelines
- Compliance controls

### Stage 3: Production Deployment (Coming Soon)
**Location**: `3-production/`

Deploy to production:
- Azure AI Foundry deployment with scaling
- AI Gateway for cost/usage monitoring
- Advanced observability
- Production security and access controls

## Language Support

### Current Status

| Language | Stage 1 | Stage 2 | Stage 3 | Notes |
|----------|---------|---------|---------|-------|
| **Python** | ✅ Complete | 🔄 Planned | 🔄 Planned | Fully tested with preview SDK |
| **C#** | ⏳ Pending | 🔄 Planned | 🔄 Planned | Awaiting SDK support for SharePoint/MCP tools |
| **Java** | ⏳ Pending | 🔄 Planned | 🔄 Planned | Awaiting SDK support for SharePoint/MCP tools |
| **TypeScript** | ⏳ Pending | 🔄 Planned | 🔄 Planned | Awaiting SDK support for SharePoint/MCP tools |

### SDK Requirements

**Python** (Working):
- `azure-ai-projects>=1.1.0b4`
- `azure-ai-agents>=1.2.0b5` (preview - includes `SharepointTool`, `McpTool`)
- `azure-identity`
- `python-dotenv`

**C#, Java, TypeScript** (Pending):
- Awaiting release of preview SDK features for SharePoint and MCP tool integration
- These features will be generally available at Microsoft Ignite
- SDK parity work in progress

## Getting Started

For now, please use the **Python implementation**:

```bash
cd python/enterprise-agent-tutorial/1-idea-to-prototype
pip install -r requirements.txt
cp .env.template .env
# Edit .env with your Azure AI Foundry project details
python main.py
```

## Documentation

See the main tutorial documentation: `../developer-journey-stage-1-idea-to-prototype.md`

## Project Structure

```
enterprise-agent-tutorial/
├── README.md (this file)
├── 1-idea-to-prototype/
│   └── python/    # ✅ Complete and tested
│       ├── main.py
│       ├── evaluate.py
│       ├── questions.jsonl
│       ├── requirements.txt
│       └── ...
├── 2-governance/ (Coming soon)
└── 3-production/ (Coming soon)
```

## Notes

- This tutorial uses **preview SDK features** for SharePoint and MCP integration
- Preview features will be generally available at Microsoft Ignite
- C#, Java, and TypeScript implementations will be added once SDK parity is achieved
- All implementations will follow the same patterns demonstrated in Python
