@echo off
setlocal
echo FinBridge - Gemini API. Khong can Ollama. Giu cua so nay mo; Ctrl+C de dung.
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-gemini.ps1" %*
exit /b %ERRORLEVEL%
