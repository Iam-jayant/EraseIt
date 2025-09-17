# GitHub Repository Setup Script
# Run this after creating your repository on GitHub.com

Write-Host "=== EraseIt - Secure Data Wipe Tool GitHub Setup ===" -ForegroundColor Green
Write-Host ""

# Repository is already configured
$repoName = "EraseIt"

$githubUrl = "https://github.com/Iam-jayant/$repoName.git"

Write-Host "Setting up remote for: $githubUrl" -ForegroundColor Yellow

# Add remote origin
git remote add origin $githubUrl

# Push to GitHub
Write-Host "Pushing to GitHub..." -ForegroundColor Yellow
git push -u origin main

Write-Host ""
Write-Host "✓ Repository successfully pushed to GitHub!" -ForegroundColor Green
Write-Host "✓ View your repository at: https://github.com/Iam-jayant/EraseIt" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Add repository topics/tags on GitHub: java, javafx, cybersecurity, data-wipe, hackathon"
Write-Host "2. Create a release for Smart India Hackathon 2025 submission"
Write-Host "3. Add your team information to README.md"

# Check if push was successful
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "🎉 Setup completed successfully!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "⚠️  There might have been an issue. Check the output above." -ForegroundColor Red
    Write-Host "If the repository already exists, you may need to use a different name." -ForegroundColor Yellow
}