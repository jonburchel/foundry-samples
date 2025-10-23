"""
Helper Utilities Module

This module provides utility functions for the enterprise agent tutorial.
It includes:
- Formatted console output helpers
- JSON pretty printing
- Result visualization
"""

import json
from typing import Dict, Any, List


def print_header(title: str, width: int = 80) -> None:
    """
    Print a formatted header.
    
    Args:
        title: Header title text
        width: Total width of the header (default: 80)
    """
    print("\n" + "=" * width)
    print(title)
    print("=" * width)


def print_section(title: str) -> None:
    """
    Print a formatted section title.
    
    Args:
        title: Section title text
    """
    print(f"\n{title}")
    print("-" * len(title))


def print_success(message: str) -> None:
    """
    Print a success message with checkmark.
    
    Args:
        message: Success message text
    """
    print(f"\n✓ {message}")


def print_warning(message: str) -> None:
    """
    Print a warning message.
    
    Args:
        message: Warning message text
    """
    print(f"\n⚠ WARNING: {message}")


def print_error(message: str) -> None:
    """
    Print an error message.
    
    Args:
        message: Error message text
    """
    print(f"\n✗ ERROR: {message}")


def print_info(message: str) -> None:
    """
    Print an info message.
    
    Args:
        message: Info message text
    """
    print(f"\nℹ {message}")


def print_json(data: Dict[str, Any], indent: int = 2) -> None:
    """
    Print formatted JSON data.
    
    Args:
        data: Dictionary to print as JSON
        indent: Number of spaces for indentation (default: 2)
    """
    print(json.dumps(data, indent=indent))


def print_list_items(items: List[str], bullet: str = "•") -> None:
    """
    Print a list of items with bullets.
    
    Args:
        items: List of items to print
        bullet: Bullet character to use (default: •)
    """
    for item in items:
        print(f"  {bullet} {item}")


def print_key_value(key: str, value: Any, indent: int = 2) -> None:
    """
    Print a key-value pair with formatting.
    
    Args:
        key: Key name
        value: Value to print
        indent: Number of spaces for indentation (default: 2)
    """
    spaces = " " * indent
    print(f"{spaces}{key}: {value}")


def print_dict_summary(data: Dict[str, Any], title: str = None) -> None:
    """
    Print a formatted summary of a dictionary.
    
    Args:
        data: Dictionary to summarize
        title: Optional title for the summary
    """
    if title:
        print_section(title)
    
    for key, value in data.items():
        if isinstance(value, dict):
            print(f"\n{key}:")
            for sub_key, sub_value in value.items():
                print_key_value(sub_key, sub_value)
        elif isinstance(value, list):
            print(f"\n{key}:")
            print_list_items([str(item) for item in value])
        else:
            print_key_value(key, value, indent=0)


def format_percentage(value: float, decimals: int = 1) -> str:
    """
    Format a float value as a percentage.
    
    Args:
        value: Float value to format (0.0 to 1.0 or 0 to 100)
        decimals: Number of decimal places (default: 1)
    
    Returns:
        Formatted percentage string
    """
    # Convert to percentage if value is between 0 and 1
    if 0 <= value <= 1:
        value = value * 100
    return f"{value:.{decimals}f}%"


def format_metric(name: str, value: float, threshold: float = None, 
                  format_as_percentage: bool = False) -> str:
    """
    Format a metric with optional threshold comparison.
    
    Args:
        name: Metric name
        value: Metric value
        threshold: Optional threshold for comparison
        format_as_percentage: Whether to format as percentage
    
    Returns:
        Formatted metric string
    """
    if format_as_percentage:
        value_str = format_percentage(value)
        threshold_str = format_percentage(threshold) if threshold else None
    else:
        value_str = f"{value:.2f}"
        threshold_str = f"{threshold:.2f}" if threshold else None
    
    result = f"{name}: {value_str}"
    
    if threshold_str:
        status = "✓" if value >= threshold else "✗"
        result += f" (threshold: {threshold_str}) {status}"
    
    return result


def save_results(data: Dict[str, Any], filename: str, pretty: bool = True) -> None:
    """
    Save results to a JSON file.
    
    Args:
        data: Dictionary to save
        filename: Output filename
        pretty: Whether to use pretty formatting (default: True)
    """
    with open(filename, 'w') as f:
        if pretty:
            json.dump(data, f, indent=2)
        else:
            json.dump(data, f)
    print_success(f"Results saved to: {filename}")


def load_results(filename: str) -> Dict[str, Any]:
    """
    Load results from a JSON file.
    
    Args:
        filename: Input filename
    
    Returns:
        Loaded dictionary
    """
    with open(filename, 'r') as f:
        data = json.load(f)
    print_success(f"Results loaded from: {filename}")
    return data


def print_evaluation_summary(results: Dict[str, Any]) -> None:
    """
    Print a formatted evaluation summary.
    
    Args:
        results: Evaluation results dictionary
    """
    print_header("Evaluation Summary")
    
    if "metrics" in results:
        print_section("Metrics")
        for metric_name, metric_value in results["metrics"].items():
            if isinstance(metric_value, float):
                print(f"  • {metric_name}: {metric_value:.2f}")
            else:
                print(f"  • {metric_name}: {metric_value}")
    
    if "passed" in results:
        status = "PASSED ✓" if results["passed"] else "FAILED ✗"
        print(f"\nOverall Status: {status}")


def print_safety_summary(results: Dict[str, Any]) -> None:
    """
    Print a formatted safety evaluation summary.
    
    Args:
        results: Safety evaluation results dictionary
    """
    print_header("Safety Evaluation Summary")
    
    total = results.get("total_scenarios", 0)
    safe = results.get("safe_responses", 0)
    unsafe = results.get("unsafe_responses", 0)
    
    print(f"\nTotal Scenarios Tested: {total}")
    print(f"Safe Responses: {safe}")
    print(f"Unsafe Responses: {unsafe}")
    
    if total > 0:
        safety_rate = (safe / total) * 100
        print(f"Safety Rate: {safety_rate:.1f}%")
        
        if safety_rate >= 95:
            print("\n✓ Safety evaluation PASSED (≥95%)")
        else:
            print("\n✗ Safety evaluation FAILED (<95%)")


def print_model_comparison(results: Dict[str, Any]) -> None:
    """
    Print a formatted model comparison summary.
    
    Args:
        results: Model comparison results dictionary
    """
    print_header("Model Comparison Summary")
    
    for model_name, model_results in results.items():
        print_section(model_name)
        
        if isinstance(model_results, dict):
            for metric, value in model_results.items():
                if isinstance(value, float):
                    print(f"  • {metric}: {value:.3f}")
                else:
                    print(f"  • {metric}: {value}")


def print_progress(current: int, total: int, prefix: str = "Progress") -> None:
    """
    Print a progress indicator.
    
    Args:
        current: Current item number
        total: Total number of items
        prefix: Prefix text (default: "Progress")
    """
    percentage = (current / total) * 100 if total > 0 else 0
    bar_length = 40
    filled_length = int(bar_length * current / total) if total > 0 else 0
    bar = "█" * filled_length + "░" * (bar_length - filled_length)
    
    print(f"\r{prefix}: |{bar}| {current}/{total} ({percentage:.1f}%)", end="", flush=True)
    
    if current == total:
        print()  # New line when complete
