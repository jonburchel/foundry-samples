import os
from dotenv import load_dotenv
from azure.identity import DefaultAzureCredential
from azure.ai.projects import AIProjectClient
from azure.ai.agents.models import AgentReference, PromptAgentDefinition

load_dotenv()

project_client = AIProjectClient(
    endpoint=os.environ["AZURE_AI_FOUNDRY_PROJECT_ENDPOINT"],
    credential=DefaultAzureCredential(),
    api_version="2025-05-15-preview",
)

with project_client:

    # [START prompt_agent_basic]
    openai_client = project_client.get_openai_client()

    agent = project_client.agents.create_agent_version(
        agent_name=os.environ["AZURE_AI_FOUNDRY_AGENT_NAME"],
        definition=PromptAgentDefinition(
            model=os.environ["AZURE_AI_FOUNDRY_MODEL_DEPLOYMENT_NAME"],
            instructions="You are a helpful assistant that answers general questions",
        ),
    )
    print(f"Agent created (id: {agent.id}, name: {agent.name}, version: {agent.version})")

    # See https://platform.openai.com/docs/api-reference/conversations/create?lang=python
    conversation = openai_client.conversations.create(
        items=[{"type": "message", "role": "user", "content": "What is the size of France in square miles?"}],
        # items=[EasyInputMessageParam(role="user", content="What is the size of France in square miles?")], # This does not work, since our service expects "type":"message" in the request.
        # items=[ResponsesUserMessageItemParam(content="What is the size of France in square miles?")], # This works, but results in a few MyPy errors due to mismatched types.
    )
    print(f"Created conversation with initial user message (id: {conversation.id})")

    # See https://platform.openai.com/docs/api-reference/responses/create?lang=python
    response = openai_client.responses.create(
        conversation=conversation.id, extra_body={"agent": AgentReference(name=agent.name).as_dict()}, input=[]
    )
    print(f"Response output: {response.output_text}")

    # See https://platform.openai.com/docs/api-reference/conversations/create-items?lang=python
    openai_client.conversations.items.create(
        conversation_id=conversation.id,
        items=[{"type": "message", "role": "user", "content": "And what is the capital city?"}],
        # items=[EasyInputMessageParam(role="user", content="And what is the capital city?")],
        # items=[ResponsesUserMessageItemParam(content="And what is the capital city?")],
    )
    print(f"Added a second user message to  the conversation")

    response = openai_client.responses.create(
        conversation=conversation.id, extra_body={"agent": AgentReference(name=agent.name).as_dict()}, input=[]
    )
    print(f"Response output: {response.output_text}")

    openai_client.conversations.delete(conversation_id=conversation.id)
    print("Conversation deleted")

    project_client.agents.delete_agent_version(agent_name=agent.name, agent_version=agent.version)
    print("Agent deleted")
    # [END prompt_agent_basic]