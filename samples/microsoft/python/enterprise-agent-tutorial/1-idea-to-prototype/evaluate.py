#!/usr/bin/env python3
"""
Evaluation Script for Modern Workplace Assistant
Tests the agent with predefined business scenarios to assess quality.
"""

#region evaluation_imports
import json
from main import create_workplace_assistant, chat_with_assistant
#endregion evaluation_imports

#region evaluation_functions
def load_test_questions(filepath="questions.jsonl"):
    """Load test questions from JSONL file"""
    questions = []
    with open(filepath, 'r') as f:
        for line in f:
            questions.append(json.loads(line.strip()))
    return questions

def run_evaluation(agent_name, mcp_tool):
    """Run evaluation with test questions"""
    questions = load_test_questions()
    results = []
    
    print(f"🧪 Running evaluation with {len(questions)} test questions...")
    
    for i, q in enumerate(questions, 1):
        print(f"📝 Question {i}/{len(questions)}: {q['question']}")
        
        response, status = chat_with_assistant(agent_name, mcp_tool, q["question"])
        
        # Simple evaluation: check if response contains expected keywords
        contains_expected = any(keyword.lower() in response.lower() 
                              for keyword in q.get("expected_keywords", []))
        
        result = {
            "question": q["question"],
            "response": response,
            "status": status,
            "contains_expected": contains_expected,
            "expected_keywords": q.get("expected_keywords", [])
        }
        results.append(result)
        
        status_icon = "✅" if contains_expected else "⚠️"
        print(f"{status_icon} Response length: {len(response)} chars (Status: {status})")
    
    # Calculate pass rate
    passed = sum(1 for r in results if r["contains_expected"])
    print(f"\n📊 Evaluation Results: {passed}/{len(results)} questions passed")
    
    return results
#endregion evaluation_functions

#region evaluation_main
def main():
    """Run evaluation on the workplace assistant"""
    print("🧪 Modern Workplace Assistant - Evaluation")
    print("="*50)
    
    try:
        # Create agent
        agent, mcp_tool, sharepoint_tool = create_workplace_assistant()
        
        # Run evaluation
        results = run_evaluation(agent.name, mcp_tool)
        
        # Save results
        with open("evaluation_results.json", "w") as f:
            json.dump(results, f, indent=2)
        
        print(f"💾 Results saved to evaluation_results.json")
        
    except Exception as e:
        print(f"❌ Evaluation failed: {e}")
#endregion evaluation_main

#region evaluation_execution
if __name__ == "__main__":
    main()
