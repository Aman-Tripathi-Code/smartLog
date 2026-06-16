param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Transactions = 5,
    [switch]$ErrorSpike
)

$headers = @{ "Content-Type" = "application/json" }

function New-Log {
    param(
        [string]$EventId,
        [string]$Timestamp,
        [string]$ServiceName,
        [string]$Level,
        [string]$Message,
        [string]$CorrelationId,
        [string]$TraceId,
        [string]$SpanId,
        [string]$ParentSpanId,
        [string]$UserId,
        [string]$TransactionId,
        [string]$ExceptionType
    )

    $log = [ordered]@{
        eventId = $EventId
        timestamp = $Timestamp
        serviceName = $ServiceName
        environment = "demo"
        level = $Level
        message = $Message
        correlationId = $CorrelationId
        traceId = $TraceId
        spanId = $SpanId
        userId = $UserId
        transactionId = $TransactionId
        module = $ServiceName.ToUpperInvariant().Replace("-", "_")
        attributes = @{
            source = "sample-generator"
            flow = "trade-finance"
        }
    }

    if ($ParentSpanId) {
        $log.parentSpanId = $ParentSpanId
    }
    if ($ExceptionType) {
        $log.exceptionType = $ExceptionType
    }
    return $log
}

for ($index = 1; $index -le $Transactions; $index++) {
    $timestamp = (Get-Date).ToUniversalTime()
    $suffix = "{0}-{1}" -f (Get-Date -Format "yyyyMMddHHmmss"), $index
    $correlationId = "corr-demo-$suffix"
    $traceId = "trace-demo-$suffix"
    $transactionId = "TF-DEMO-$suffix"
    $userId = "U-DEMO-$index"
    $failed = $ErrorSpike -or ($index % 2 -eq 0)

    $logs = @(
        (New-Log "evt-demo-gateway-$suffix" $timestamp.AddMilliseconds(100).ToString("o") "api-gateway-service" "INFO" "Request received" $correlationId $traceId "span-gateway-$suffix" $null $userId $transactionId $null),
        (New-Log "evt-demo-auth-$suffix" $timestamp.AddMilliseconds(200).ToString("o") "auth-service" "INFO" "User authenticated" $correlationId $traceId "span-auth-$suffix" "span-gateway-$suffix" $userId $transactionId $null),
        (New-Log "evt-demo-trade-$suffix" $timestamp.AddMilliseconds(300).ToString("o") "trade-service" "INFO" "Trade transaction created" $correlationId $traceId "span-trade-$suffix" "span-auth-$suffix" $userId $transactionId $null)
    )

    if ($failed) {
        $logs += (New-Log "evt-demo-limit-$suffix" $timestamp.AddMilliseconds(400).ToString("o") "limit-check-service" "ERROR" "Customer limit validation failed" $correlationId $traceId "span-limit-$suffix" "span-trade-$suffix" $userId $transactionId "LimitExceededException")
        $logs += (New-Log "evt-demo-workflow-$suffix" $timestamp.AddMilliseconds(500).ToString("o") "workflow-service" "WARN" "Workflow stopped due to validation failure" $correlationId $traceId "span-workflow-$suffix" "span-limit-$suffix" $userId $transactionId $null)
    } else {
        $logs += (New-Log "evt-demo-limit-$suffix" $timestamp.AddMilliseconds(400).ToString("o") "limit-check-service" "INFO" "Customer limit approved" $correlationId $traceId "span-limit-$suffix" "span-trade-$suffix" $userId $transactionId $null)
        $logs += (New-Log "evt-demo-workflow-$suffix" $timestamp.AddMilliseconds(500).ToString("o") "workflow-service" "INFO" "Workflow submitted" $correlationId $traceId "span-workflow-$suffix" "span-limit-$suffix" $userId $transactionId $null)
    }

    $payload = @{ logs = $logs } | ConvertTo-Json -Depth 8
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/logs/batch" -Headers $headers -Body $payload | Out-Null
    Write-Host "Sent $($logs.Count) logs for $correlationId"
}

Write-Host "Done. Try: $BaseUrl/api/v1/traces/corr-demo-<timestamp>-<index>"
