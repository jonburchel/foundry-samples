# <generate_response>

curl -X POST https://YOUR-FOUNDRY-RESOURCE-NAME.services.ai.azure.com/api/projects/YOUR-PROJECT-NAME/openai/responses?api-version=2025-11-15-preview \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AZURE_AI_AUTH_TOKEN" \
-d '{
        "model": "gpt-4.1-mini",
        "input": "What is the size of France in square miles?"
}'

# </generate_response>

# <create_agent>
curl -X POST https://YOUR-FOUNDRY-RESOURCE-NAME.services.ai.azure.com/api/projects/YOUR-PROJECT-NAME/agents?api-version=2025-11-15-preview \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AZURE_AI_AUTH_TOKEN" \
  -d '{
        "name": "MyAgent",
        "definition": {
            "kind": "prompt",
            "model": "gpt-4.1-mini", 
            "instructions": "You are a helpful assistant that answers general questions"
        }
    }'

# </create_agent>

# <chat_with_agent>
#Create a conversation
curl -X POST https://YOUR-FOUNDRY-RESOURCE-NAME.services.ai.azure.com/api/projects/YOUR-PROJECT-NAME/openai/conversations?api-version=2025-11-15-preview \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AZURE_AI_AUTH_TOKEN" \
  -d '{}'
# Lets say Conversation ID created is conv_123456789. Use this in the next step

# Generate a response with the agent and conversation
curl -X POST https://YOUR-FOUNDRY-RESOURCE-NAME.services.ai.azure.com/api/projects/YOUR-PROJECT-NAME/openai/responses?api-version=2025-11-15-preview \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $AZURE_AI_AUTH_TOKEN" \
  -d '{
        "agent": {"type": "agent_reference", "name": "MyAgent"},
        "conversation" : "<YOUR_CONVERSATION_ID>",
        "input" : "What is the size of France in square miles?"
    }'
# </chat_with_agent>