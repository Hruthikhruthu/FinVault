$ErrorActionPreference = "Stop"

Invoke-RestMethod http://localhost:8080/actuator/health | ConvertTo-Json -Depth 5
Invoke-WebRequest http://localhost:3000 -UseBasicParsing | Select-Object StatusCode, StatusDescription
