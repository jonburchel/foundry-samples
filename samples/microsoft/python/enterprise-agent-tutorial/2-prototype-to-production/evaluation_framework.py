#!/usr/bin/env python3
"""
Evaluation Framework Module

Handles evaluation dataset creation and baseline evaluation.
Establishes quality baselines using ground truth data.
"""

import json
from datetime import datetime
from typing import Dict, Any
from azure.ai.agents.models import AgentReference

class EvaluationFramework:
    """Manages evaluation dataset creation and baseline evaluation."""
    
    def __init__(self, project_client):
        """
        Initialize with project client.
        
        Args:
            project_client: AIProjectClient instance
        """
        self.project_client = project_client
        self.openai_client = project_client.get_openai_client()
    
    def create_evaluation_dataset(self) -> str:
        """
        Create comprehensive evaluation dataset with ground truth labels.
        
        Returns:
            Filename of the created dataset
        """
        
        print("\n📊 CREATING EVALUATION DATASET")
        print("="*70)
        
        # <evaluation_dataset_structure>
        # NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        eval_dataset = [
            {
                "id": "eval_001",
                "question": "What is our company's remote work security policy?",
                "expected_source": "sharepoint",
                "expected_topics": ["remote work", "security", "policy", "VPN", "MFA"],
                "quality_threshold": 0.7
            },
            {
                "id": "eval_002",
                "question": "How do I configure Azure AD Conditional Access?",
                "expected_source": "microsoft_learn",
                "expected_topics": ["Azure AD", "conditional access", "configuration"],
                "quality_threshold": 0.8
            },
            {
                "id": "eval_003",
                "question": "What Azure security features should I use to comply with our data governance policy?",
                "expected_source": "both",
                "expected_topics": ["Azure", "security", "compliance", "governance", "data"],
                "quality_threshold": 0.75
            },
            {
                "id": "eval_004",
                "question": "Explain our collaboration standards for Microsoft Teams",
                "expected_source": "sharepoint",
                "expected_topics": ["Teams", "collaboration", "standards", "sharing"],
                "quality_threshold": 0.7
            },
            {
                "id": "eval_005",
                "question": "How do I implement multi-factor authentication in Azure?",
                "expected_source": "microsoft_learn",
                "expected_topics": ["MFA", "authentication", "Azure", "implementation"],
                "quality_threshold": 0.8
            }
        ]
        # </evaluation_dataset_structure>
        
        filename = 'evaluation_dataset.jsonl'
        with open(filename, 'w') as f:
            for item in eval_dataset:
                f.write(json.dumps(item) + '\n')
        
        print(f"✅ Created evaluation dataset: {filename}")
        print(f"   Total questions: {len(eval_dataset)}")
        print(f"   Coverage: SharePoint, Microsoft Learn, Combined scenarios")
        
        return filename
    
    def run_baseline_evaluation(self, agent_name: str, dataset_file: str) -> Dict[str, Any]:
        """
        Run baseline evaluation to establish quality metrics.
        
        Args:
            agent_name: Name of the agent to evaluate
            dataset_file: Path to the evaluation dataset file
            
        Returns:
            Dict containing evaluation results
        """
        
        print("\n📈 RUNNING BASELINE EVALUATION")
        print("="*70)
        
        # Load evaluation dataset
        eval_data = []
        with open(dataset_file, 'r') as f:
            for line in f:
                eval_data.append(json.loads(line))
        
        results = {
            "timestamp": datetime.now().isoformat(),
            "agent_name": agent_name,
            "dataset": dataset_file,
            "total_questions": len(eval_data),
            "evaluations": []
        }
        
        for item in eval_data:
            print(f"\n🔍 Evaluating: {item['id']}")
            print(f"   Question: {item['question'][:60]}...")
            
            try:
                # Get agent response
                conversation = self.openai_client.conversations.create(
                    items=[{
                        "type": "message",
                        "role": "user",
                        "content": item['question']
                    }]
                )
                
                response = self.openai_client.responses.create(
                    conversation=conversation.id,
                    extra_body={"agent": AgentReference(name=agent_name).as_dict()}
                )
                
                response_text = response.output_text if hasattr(response, 'output_text') else str(response.output)
                
                # <quality_metric_calculation>
                # NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
                # Calculate quality score based on topic coverage
                topics_found = sum(1 for topic in item['expected_topics'] 
                                 if topic.lower() in response_text.lower())
                quality_score = topics_found / len(item['expected_topics'])
                
                passed = quality_score >= item['quality_threshold']
                # </quality_metric_calculation>
                
                eval_result = {
                    "id": item['id'],
                    "question": item['question'],
                    "response_length": len(response_text),
                    "topics_expected": len(item['expected_topics']),
                    "topics_found": topics_found,
                    "quality_score": quality_score,
                    "threshold": item['quality_threshold'],
                    "passed": passed
                }
                
                results["evaluations"].append(eval_result)
                
                status = "✅ PASS" if passed else "❌ FAIL"
                print(f"   {status} - Quality: {quality_score:.2f} (threshold: {item['quality_threshold']})")
                
            except Exception as e:
                print(f"   ❌ ERROR: {e}")
                results["evaluations"].append({
                    "id": item['id'],
                    "error": str(e),
                    "passed": False
                })
        
        # Calculate overall metrics
        passed_count = sum(1 for e in results["evaluations"] if e.get("passed", False))
        results["pass_rate"] = (passed_count / len(eval_data)) * 100
        results["avg_quality_score"] = sum(e.get("quality_score", 0) 
                                          for e in results["evaluations"]) / len(eval_data)
        
        print("\n" + "="*70)
        print(f"📊 BASELINE EVALUATION COMPLETE")
        print(f"   Pass Rate: {results['pass_rate']:.1f}%")
        print(f"   Avg Quality Score: {results['avg_quality_score']:.2f}")
        print(f"   Passed: {passed_count}/{len(eval_data)}")
        
        # Save results
        with open('baseline_evaluation.json', 'w') as f:
            json.dump(results, f, indent=2)
        print(f"   📄 Results saved to: baseline_evaluation.json")
        
        return results
