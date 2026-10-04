@echo off
setlocal
echo FinBridge - chay local tu CMD
echo Can mo ung dung Ollama truoc. Model: qwen3:4b.
echo Script se kiem tra Java, Maven, Ollama va model, sau do chay FinBridge.
echo Lan dau lam nong AI co the mat toi 60 giay. Giu cua so nay mo.
echo.
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-local.ps1" %*
set "finbridgeExit=%ERRORLEVEL%"
if not "%finbridgeExit%"=="0" (
  echo.
  echo FinBridge chua khoi dong thanh cong. Xem dong loi phia tren.
  echo Ollama chua chay: mo ung dung Ollama tu Start Menu, roi chay lai.
  echo Model chua co: ollama pull qwen3:4b
  echo Cong 8080 dang dung: dung app cu bang Ctrl+C, hoac chay scripts\start-local.cmd -Port 8081
)
exit /b %finbridgeExit%
