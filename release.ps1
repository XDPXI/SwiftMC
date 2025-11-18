# Variables
$VERSION = "1.0.0-SNAPSHOT" # MATCH WITH VERSION IN BUILD.GRADLE!
$OLD = "7b8859ea7b7f72c58be15b4449ba63487c4e0b35"
$NEW = "e9604569341f70ff192c6f90534e191a2bb29b72"
$LIB_DIR = "build/libs"

# Build
Write-Host "Running clean build via ./gradlew clean build …"
& .\gradlew clean build
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed. Aborting."
    exit $LASTEXITCODE
}
& .\gradlew build
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed. Aborting."
    exit $LASTEXITCODE
}

# hashes
$files = @(
    "SwiftMC-$VERSION.jar",
    "SwiftMC-$VERSION-javadoc.jar",
    "SwiftMC-$VERSION-sources.jar"
)
$hashResults = @{}

foreach ($f in $files) {
    $path = Join-Path $LIB_DIR $f
    if (-not (Test-Path $path)) {
        Write-Error "File not found: $path"
        exit 1
    }

    # MD5
    $md5 = (Get-FileHash -Path $path -Algorithm MD5).Hash

    # SHA1
    $sha1 = (Get-FileHash -Path $path -Algorithm SHA1).Hash

    # SHA256
    $sha256 = (Get-FileHash -Path $path -Algorithm SHA256).Hash
    $hashResults[$f] = [PSCustomObject]@{
        MD5    = $md5
        SHA1   = $sha1
        SHA256 = $sha256
    }
}

# Get commit list
Write-Host "Fetching commits from GitHub: $OLD → $NEW"

$apiUrl = "https://api.github.com/repos/XDPXI/SwiftMC/compare/$OLD...$NEW"
$headers = @{ "User-Agent" = "PowerShell" }

try {
    $response = Invoke-RestMethod -Uri $apiUrl -Headers $headers -Method GET
} catch {
    Write-Error "Failed to fetch commits from GitHub. $_"
    exit 1
}

if (-not $response.commits) {
    Write-Error "No commits returned from GitHub API."
    exit 1
}

$commitLines = $response.commits | ForEach-Object {
    [PSCustomObject]@{
        ShortHash = $_.sha.Substring(0,7)
        Message   = $_.commit.message.Split("`n")[0]
    }
}

# Write RELEASE.md
$releaseMdPath = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Definition) "RELEASE.md"
Write-Host "Writing release file to $releaseMdPath"
$sb = New-Object System.Text.StringBuilder

$sb.AppendLine("## Commits") | Out-Null
$sb.AppendLine() | Out-Null
foreach ($c in $commitLines) {
    $line = ("- [``{1}``](https://github.com/XDPXI/SwiftMC/commit/{1}): {0}" -f $c.Message, $c.ShortHash)
    $sb.AppendLine($line) | Out-Null
}

$sb.AppendLine() | Out-Null
$sb.AppendLine("## Hashes") | Out-Null
$sb.AppendLine() | Out-Null

foreach ($f in $files) {
    $sb.AppendLine("|        | $f |") | Out-Null
    $sb.AppendLine("| ------ | --------------------- |") | Out-Null
    $sb.AppendLine("| MD5    | {0} |" -f $hashResults[$f].MD5) | Out-Null
    $sb.AppendLine("| SHA1   | {0} |" -f $hashResults[$f].SHA1) | Out-Null
    $sb.AppendLine("| SHA256 | {0} |" -f $hashResults[$f].SHA256) | Out-Null
    $sb.AppendLine() | Out-Null
}

[System.IO.File]::WriteAllText($releaseMdPath, $sb.ToString())

Write-Host "Release file generated. Done."
