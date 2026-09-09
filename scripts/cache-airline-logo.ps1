param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[A-Za-z]{3}$')]
    [string] $Icao,

    [string] $OutputDirectory = 'data/cache/airline-logos',

    [string] $MetadataDirectory = 'data/cache/airline-logo-metadata'
)

$ErrorActionPreference = 'Stop'
$airlineIcao = $Icao.ToUpperInvariant()
$passengerUri = 'https://raw.githubusercontent.com/dotmarn/Airlines/master/passenger.json'
$openFlightsUri = 'https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat'
$passengerJson = Join-Path $MetadataDirectory 'dotmarn-passenger.json'
$openFlightsData = Join-Path $MetadataDirectory 'openflights-airlines.dat'
$tempPng = Join-Path ([System.IO.Path]::GetTempPath()) "screenduo-airline-logo-$airlineIcao.png"
$outputPath = Join-Path $OutputDirectory "$airlineIcao.rgb"

New-Item -ItemType Directory -Force $OutputDirectory | Out-Null
New-Item -ItemType Directory -Force $MetadataDirectory | Out-Null

if (-not (Test-Path -LiteralPath $passengerJson)) {
    Invoke-WebRequest -Uri $passengerUri -OutFile $passengerJson
}
if (-not (Test-Path -LiteralPath $openFlightsData)) {
    Invoke-WebRequest -Uri $openFlightsUri -OutFile $openFlightsData
}

function Split-OpenFlightsCsvLine([string] $line) {
    $values = New-Object System.Collections.Generic.List[string]
    $current = New-Object System.Text.StringBuilder
    $quoted = $false
    for ($index = 0; $index -lt $line.Length; $index++) {
        $character = $line[$index]
        if ($character -eq '"') {
            $quoted = -not $quoted
        } elseif ($character -eq ',' -and -not $quoted) {
            $values.Add($current.ToString())
            [void] $current.Clear()
        } else {
            [void] $current.Append($character)
        }
    }
    $values.Add($current.ToString())
    return $values
}

$airlineIata = $null
foreach ($line in Get-Content -LiteralPath $openFlightsData) {
    $columns = Split-OpenFlightsCsvLine $line
    if ($columns.Count -ge 8 -and $columns[4].ToUpperInvariant() -eq $airlineIcao) {
        $candidate = $columns[3].Trim().ToUpperInvariant()
        if ($candidate -ne '\N' -and $candidate.Length -gt 0) {
            $airlineIata = $candidate
            break
        }
    }
}

if ($null -eq $airlineIata) {
    throw "Unable to map ICAO $airlineIcao to an IATA code through OpenFlights."
}

$airline = (Get-Content -LiteralPath $passengerJson -Raw | ConvertFrom-Json) |
        Where-Object { $_.iata.ToUpperInvariant() -eq $airlineIata } |
        Select-Object -First 1

if ($null -eq $airline) {
    $logoUri = "https://images.kiwi.com/airlines/64/$airlineIata.png"
    $airlineName = "IATA $airlineIata"
} else {
    $logoUri = $airline.logo
    $airlineName = $airline.name
}

Invoke-WebRequest -Uri $logoUri -OutFile $tempPng

Add-Type -AssemblyName System.Drawing
$sourceBitmap = [System.Drawing.Bitmap]::new($tempPng)
try {
    $targetWidth = 32
    $targetHeight = 24
    $targetBitmap = [System.Drawing.Bitmap]::new($targetWidth, $targetHeight)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($targetBitmap)
        try {
            $graphics.Clear([System.Drawing.Color]::White)
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $ratio = [Math]::Min($targetWidth / $sourceBitmap.Width, $targetHeight / $sourceBitmap.Height)
            $width = [Math]::Max(1, [int][Math]::Round($sourceBitmap.Width * $ratio))
            $height = [Math]::Max(1, [int][Math]::Round($sourceBitmap.Height * $ratio))
            $x = [int][Math]::Floor(($targetWidth - $width) / 2)
            $y = [int][Math]::Floor(($targetHeight - $height) / 2)
            $graphics.DrawImage($sourceBitmap, $x, $y, $width, $height)
        } finally {
            $graphics.Dispose()
        }

        $bytes = [byte[]]::new($targetWidth * $targetHeight * 3)
        $offset = 0
        for ($row = 0; $row -lt $targetHeight; $row++) {
            for ($column = 0; $column -lt $targetWidth; $column++) {
                $pixel = $targetBitmap.GetPixel($column, $row)
                $bytes[$offset] = $pixel.R
                $bytes[$offset + 1] = $pixel.G
                $bytes[$offset + 2] = $pixel.B
                $offset += 3
            }
        }
        [System.IO.File]::WriteAllBytes($outputPath, $bytes)
    } finally {
        $targetBitmap.Dispose()
    }
} finally {
    $sourceBitmap.Dispose()
    Remove-Item -LiteralPath $tempPng -ErrorAction SilentlyContinue
}

Write-Host "Cached $airlineIcao / $airlineIata logo for $airlineName at $outputPath"
