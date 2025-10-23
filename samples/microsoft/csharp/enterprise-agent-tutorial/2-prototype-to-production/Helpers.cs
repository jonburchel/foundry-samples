using System;
using System.Text.Json;

namespace Microsoft.Azure.Samples.PrototypeToProduction
{
    /// <summary>
    /// Helper utilities for the Prototype to Production tutorial.
    /// Provides common functionality used across modules.
    /// </summary>
    public static class Helpers
    {
        /// <summary>
        /// Prints a formatted section header.
        /// </summary>
        /// <param name="title">Title of the section</param>
        /// <param name="width">Width of the header (default 70)</param>
        public static void PrintSectionHeader(string title, int width = 70)
        {
            Console.WriteLine("\n" + new string('=', width));
            Console.WriteLine(title);
            Console.WriteLine(new string('=', width));
        }

        /// <summary>
        /// Prints a formatted subsection header.
        /// </summary>
        /// <param name="title">Title of the subsection</param>
        public static void PrintSubsectionHeader(string title)
        {
            Console.WriteLine($"\n{title}");
            Console.WriteLine(new string('-', 70));
        }

        /// <summary>
        /// Formats a JSON object for pretty printing.
        /// </summary>
        /// <param name="obj">Object to serialize</param>
        /// <returns>Formatted JSON string</returns>
        public static string FormatJson(object obj)
        {
            return JsonSerializer.Serialize(obj, new JsonSerializerOptions 
            { 
                WriteIndented = true 
            });
        }

        /// <summary>
        /// Truncates a string to a specified length and adds ellipsis if needed.
        /// </summary>
        /// <param name="text">Text to truncate</param>
        /// <param name="maxLength">Maximum length</param>
        /// <returns>Truncated string</returns>
        public static string Truncate(string text, int maxLength)
        {
            if (string.IsNullOrEmpty(text) || text.Length <= maxLength)
                return text;

            return text.Substring(0, maxLength) + "...";
        }

        /// <summary>
        /// Prints a success message with formatting.
        /// </summary>
        /// <param name="message">Success message</param>
        public static void PrintSuccess(string message)
        {
            Console.WriteLine($"✅ {message}");
        }

        /// <summary>
        /// Prints an error message with formatting.
        /// </summary>
        /// <param name="message">Error message</param>
        public static void PrintError(string message)
        {
            Console.WriteLine($"❌ {message}");
        }

        /// <summary>
        /// Prints a warning message with formatting.
        /// </summary>
        /// <param name="message">Warning message</param>
        public static void PrintWarning(string message)
        {
            Console.WriteLine($"⚠️  {message}");
        }

        /// <summary>
        /// Prints an info message with formatting.
        /// </summary>
        /// <param name="message">Info message</param>
        public static void PrintInfo(string message)
        {
            Console.WriteLine($"ℹ️  {message}");
        }
    }
}
