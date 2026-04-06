$json1 = '{"username": "hod", "email": "hod@com", "password": "weak"}'
$json2 = '{"username": "hod", "email": "hod@com", "password": "Strong1Password!"}'
$json3 = '{"username": "hod", "email": "hod2@com", "password": "Strong2Password!"}'

Set-Content -Path test1.json -Value $json1
Set-Content -Path test2.json -Value $json2
Set-Content -Path test3.json -Value $json3

Write-Host "--- TEST 1: WEAK PASSWORD ---"
curl.exe -s -w " | HTTP %{http_code}" -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "@test1.json"

Write-Host " `n--- TEST 2: STRONG VALID ---"
curl.exe -s -w " | HTTP %{http_code}" -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "@test2.json"

Write-Host " `n--- TEST 3: DUPLICATE USERNAME ---"
curl.exe -s -w " | HTTP %{http_code}" -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "@test3.json"
