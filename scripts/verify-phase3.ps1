param([string]$BaseUrl = 'http://localhost:8096')

$ErrorActionPreference = 'Stop'

$checks = [ordered]@{}
$phasePage = Invoke-WebRequest -UseBasicParsing "$BaseUrl/"
$checks['GET / status 200'] = $phasePage.StatusCode -eq 200
$checks['Phase 3 UI'] = $phasePage.Content -match 'PHASE 3'
$checks['Vietnam to China'] = $phasePage.Content -match 'Vietnam' -and $phasePage.Content -match 'China'
$checks['Tuition 20,000 CNY'] = $phasePage.Content -match '20,000' -and $phasePage.Content -match 'CNY'
$checks['School registry verified'] = $phasePage.Content -match 'Recipient verified'
$checks['Alipay shown'] = $phasePage.Content -match 'Alipay Student Payment'
$checks['Bank A shown'] = $phasePage.Content -match 'Bank A International Transfer'
$checks['Bank B unavailable'] = $phasePage.Content -match 'Bank B Promotional Rate' -and $phasePage.Content -match 'Unavailable'
$checks['Bank B no execute'] = $phasePage.Content -match 'No select or execute action'
$checks['Cheaper ranks Bank A first'] = $phasePage.Content.IndexOf('Bank A International Transfer') -lt $phasePage.Content.IndexOf('Alipay Student Payment')

Invoke-WebRequest -UseBasicParsing -Method Post -Body @{ preference = 'FASTER' } "$BaseUrl/student/preference" | Out-Null
$fastPage = Invoke-WebRequest -UseBasicParsing "$BaseUrl/"
$checks['Faster ranks Alipay first'] = $fastPage.Content.IndexOf('Alipay Student Payment') -lt $fastPage.Content.IndexOf('Bank A International Transfer')

Invoke-WebRequest -UseBasicParsing -Method Post -Body @{ preference = 'SAFER' } "$BaseUrl/student/preference" | Out-Null
$safePage = Invoke-WebRequest -UseBasicParsing "$BaseUrl/"
$checks['Safer ranks Bank A first'] = $safePage.Content.IndexOf('Bank A International Transfer') -lt $safePage.Content.IndexOf('Alipay Student Payment')

$refreshPage = Invoke-WebRequest -UseBasicParsing -Method Post "$BaseUrl/student/quotes/refresh"
$checks['Quote refresh redirects home'] = $refreshPage.StatusCode -eq 200 -and $refreshPage.Content -match 'Quotes refreshed'

Invoke-WebRequest -UseBasicParsing -Method Post "$BaseUrl/reset" | Out-Null
$resetPage = Invoke-WebRequest -UseBasicParsing "$BaseUrl/"
$checks['Reset restores cheaper preference'] = $resetPage.Content -match 'value="CHEAPER" selected'
$checks['Reset preserves fixed demo bill'] = $resetPage.Content -match '20,000' -and $resetPage.Content -match 'Shenzhen Demo University'

$checks.GetEnumerator() | ForEach-Object {
    '{0}: {1}' -f $_.Key, $(if ($_.Value) { 'PASS' } else { 'FAIL' })
}
if ($checks.Values -contains $false) { exit 1 }