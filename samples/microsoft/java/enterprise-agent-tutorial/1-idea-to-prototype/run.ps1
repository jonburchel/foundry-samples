# PowerShell script to run Java sample
# Right-click this file and select "Run with PowerShell"

Write-Host "🚀 Running Java Enterprise Agent Tutorial..." -ForegroundColor Cyan
Write-Host ""

# Navigate to script directory
Set-Location -Path $PSScriptRoot

# Check if Maven is available
$mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnCommand) {
    Write-Host "❌ Maven not found in PATH" -ForegroundColor Red
    Write-Host "Please restart your PowerShell session after adding Maven to PATH" -ForegroundColor Yellow
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "✅ Maven found: $($mvnCommand.Source)" -ForegroundColor Green
Write-Host ""

# Run the sample
Write-Host "▶️  Executing Java sample..." -ForegroundColor Cyan
mvn -q exec:java -Dexec.mainClass=Main

Write-Host ""
Write-Host "✅ Java sample execution complete" -ForegroundColor Green
Write-Host ""
Read-Host "Press Enter to exit"
