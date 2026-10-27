<#
.SYNOPSIS
    DigiWear Automated Sideloading Assistant
    Easily connect and install the Wear OS / AOSP Watch app and Phone Companion app via ADB.
    Supports both interactive wizard mode and CLI flags (--watch, --phone, --all, --rebuild, --help).
#>

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$ScriptDir = if ($PSScriptRoot) { $PSScriptRoot } elseif ($MyInvocation.MyCommand.Path) { Split-Path -Parent $MyInvocation.MyCommand.Path } else { (Get-Location).Path }

function Write-Header {
    Clear-Host
    Write-Host '======================================================================' -ForegroundColor Cyan
    Write-Host '            DIGIWEAR SIDELOADING & INSTALLATION WIZARD            ' -ForegroundColor Yellow -BackgroundColor Black
    Write-Host '======================================================================' -ForegroundColor Cyan
    Write-Host '  500-Species Dual-Device Virtual Pet for Wear OS, AOSP & Android     ' -ForegroundColor Gray
    Write-Host '======================================================================' -ForegroundColor Cyan
    Write-Host ''
}

function Find-Adb {
    # 1. Check if adb is directly on PATH
    $cmd = Get-Command 'adb' -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    # 2. Check standard Android SDK locations
    $candidatePaths = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe",
        'C:\Android\sdk\platform-tools\adb.exe',
        'C:\Program Files\Android\platform-tools\adb.exe',
        "$HOME\Library\Android\sdk\platform-tools\adb",
        "$HOME\Android\Sdk\platform-tools\adb",
        '/usr/bin/adb',
        '/usr/local/bin/adb'
    )

    foreach ($path in $candidatePaths) {
        if (Test-Path $path) {
            $toolDir = Split-Path $path -Parent
            $env:PATH = "$toolDir;$($env:PATH)"
            return $path
        }
    }

    return $null
}

function Get-ApkPath {
    param(
        [string]$moduleName
    )
    $scriptDir = $ScriptDir
    $candidates = @()

    if ($moduleName -eq 'wear-os-app') {
        $candidates += @(
            (Join-Path $scriptDir 'wear-os-app\build\outputs\apk\release\wear-os-app-release.apk'),
            (Join-Path $scriptDir 'release_bin\DigiWear-WearOS-release.apk'),
            (Join-Path $scriptDir 'release_bin\wear-os-app-release.apk'),
            (Join-Path $scriptDir 'release_bin\digiwear-aosp-standard-watch-release.apk'),
            (Join-Path $scriptDir 'wear-os-app\build\outputs\apk\debug\wear-os-app-debug.apk'),
            (Join-Path $scriptDir 'release_bin\wear-os-app-debug.apk')
        )
    } elseif ($moduleName -eq 'phone-companion-app') {
        $candidates += @(
            (Join-Path $scriptDir 'phone-companion-app\build\outputs\apk\release\phone-companion-app-release.apk'),
            (Join-Path $scriptDir 'release_bin\DigiWear-Companion-release.apk'),
            (Join-Path $scriptDir 'release_bin\phone-companion-app-release.apk'),
            (Join-Path $scriptDir 'phone-companion-app\build\outputs\apk\debug\phone-companion-app-debug.apk'),
            (Join-Path $scriptDir 'release_bin\phone-companion-app-debug.apk')
        )
    }

    $existing = $candidates | Where-Object { Test-Path $_ } | ForEach-Object { Get-Item $_ }
    if ($existing) {
        # Prefer release APKs first, ordered by most recently modified timestamp
        $newestRelease = $existing | Where-Object { $_.Name -notmatch 'debug' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($newestRelease) { return $newestRelease.FullName }
        return ($existing | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
    }

    return $null
}

function Sync-ReleaseBinaries {
    $scriptDir = $ScriptDir
    $relBinDir = Join-Path $scriptDir 'release_bin'
    if (-not (Test-Path $relBinDir)) {
        New-Item -ItemType Directory -Path $relBinDir | Out-Null
    }

    $wearRel = Join-Path $scriptDir 'wear-os-app\build\outputs\apk\release\wear-os-app-release.apk'
    if (Test-Path $wearRel) {
        Copy-Item $wearRel (Join-Path $relBinDir 'DigiWear-WearOS-release.apk') -Force
        Copy-Item $wearRel (Join-Path $relBinDir 'wear-os-app-release.apk') -Force
        Copy-Item $wearRel (Join-Path $relBinDir 'digiwear-aosp-standard-watch-release.apk') -Force
    }
    $wearDbg = Join-Path $scriptDir 'wear-os-app\build\outputs\apk\debug\wear-os-app-debug.apk'
    if (Test-Path $wearDbg) {
        Copy-Item $wearDbg (Join-Path $relBinDir 'wear-os-app-debug.apk') -Force
    }

    $phoneRel = Join-Path $scriptDir 'phone-companion-app\build\outputs\apk\release\phone-companion-app-release.apk'
    if (Test-Path $phoneRel) {
        Copy-Item $phoneRel (Join-Path $relBinDir 'DigiWear-Companion-release.apk') -Force
        Copy-Item $phoneRel (Join-Path $relBinDir 'phone-companion-app-release.apk') -Force
    }
    $phoneDbg = Join-Path $scriptDir 'phone-companion-app\build\outputs\apk\debug\phone-companion-app-debug.apk'
    if (Test-Path $phoneDbg) {
        Copy-Item $phoneDbg (Join-Path $relBinDir 'phone-companion-app-debug.apk') -Force
    }
}

function Show-Guide {
    Write-Host ''
    Write-Host '----------------------------------------------------------------------' -ForegroundColor Yellow
    Write-Host '  HOW TO ENABLE DEVELOPER OPTIONS & WIRELESS / USB DEBUGGING          ' -ForegroundColor Yellow
    Write-Host '----------------------------------------------------------------------' -ForegroundColor Yellow
    Write-Host ''
    Write-Host '[ 1. Wear OS Smartwatch (Pixel Watch, Galaxy Watch 4/5/6/7/Ultra) ]' -ForegroundColor Green
    Write-Host '  1. On your watch, open Settings -> System -> About -> Versions.' -ForegroundColor White
    Write-Host '  2. Tap "Build Number" 7 times rapidly until you see "You are a developer!"' -ForegroundColor White
    Write-Host '  3. Go back to Settings -> Developer Options.' -ForegroundColor White
    Write-Host '  4. Turn ON "ADB Debugging" and "Wireless Debugging" (Debug over Wi-Fi).' -ForegroundColor White
    Write-Host '  5. Connect your watch and PC to the SAME Wi-Fi network.' -ForegroundColor White
    Write-Host '  6. First time on Wear OS 4/5? Use Option [6] Pair New Wireless Watch.' -ForegroundColor White
    Write-Host '  7. Note the IP address and Port (e.g. 192.168.1.50:5555) and use Option [1].' -ForegroundColor White
    Write-Host ''
    Write-Host '[ 2. Standalone AOSP / Full Android Watch (Kospet, Lokmat, Lemfo, Zeblaze) ]' -ForegroundColor Green
    Write-Host '  1. Open Settings -> System -> About Watch -> tap "Build Number" 7 times.' -ForegroundColor White
    Write-Host '  2. Enable "USB Debugging" (4-pin magnetic USB cable) or "Wireless Debugging".' -ForegroundColor White
    Write-Host '  3. After installing, open DigiWear Settings on the watch:' -ForegroundColor White
    Write-Host '     - Set Display Shape Override to ROUND if your circular watch clips corners.' -ForegroundColor White
    Write-Host '     - Use AOSP LAN Bridge (TCP 45820 / UDP 45821) to sync with the Phone App!' -ForegroundColor White
    Write-Host ''
    Write-Host '[ 3. Android Phone Companion ]' -ForegroundColor Green
    Write-Host '  1. On your phone, go to Settings -> About Phone -> tap "Build Number" 7 times.' -ForegroundColor White
    Write-Host '  2. Go to Settings -> System -> Developer Options.' -ForegroundColor White
    Write-Host '  3. Turn ON "USB Debugging" (connect via USB cable) or "Wireless Debugging".' -ForegroundColor White
    Write-Host '----------------------------------------------------------------------' -ForegroundColor Yellow
    Write-Host ''
    Read-Host 'Press [Enter] to return to the menu'
}

function Pair-WirelessDevice {
    param([string]$adb)
    Write-Host ''
    Write-Host '--- PAIR WEAR OS DEVICE (Android 11+ / Wear OS 4+ Pairing Code) ---' -ForegroundColor Cyan
    Write-Host 'On your watch: Settings -> Developer Options -> Wireless Debugging -> Pair new device.' -ForegroundColor Gray
    Write-Host ''
    $pairIpPort = Read-Host 'Enter Watch Pairing IP:Port (e.g. 192.168.1.50:42135)'
    if ([string]::IsNullOrWhiteSpace($pairIpPort)) {
        Write-Host 'Pairing aborted.' -ForegroundColor Yellow
        return
    }
    $pairCode = Read-Host 'Enter 6-digit Wi-Fi Pairing Code'
    if ([string]::IsNullOrWhiteSpace($pairCode)) {
        Write-Host 'Pairing aborted.' -ForegroundColor Yellow
        return
    }

    Write-Host "Executing: adb pair $pairIpPort $pairCode" -ForegroundColor Gray
    & $adb pair $pairIpPort $pairCode
    Write-Host ''
    Read-Host 'Press [Enter] to continue'
}

function Connect-WirelessDevice {
    param([string]$adb)
    Write-Host ''
    Write-Host '--- CONNECT TO WIRELESS WEAR OS / AOSP WATCH ---' -ForegroundColor Cyan
    Write-Host 'On your watch: Settings -> Developer Options -> Wireless Debugging (Main Screen)' -ForegroundColor Gray
    $ipPort = Read-Host 'Enter Watch IP:Port (e.g. 192.168.1.50:5555)'
    if ([string]::IsNullOrWhiteSpace($ipPort)) {
        Write-Host 'Connection aborted.' -ForegroundColor Yellow
        return $null
    }

    Write-Host "Connecting to $ipPort..." -ForegroundColor Gray
    & $adb connect $ipPort
    return $ipPort
}

function Install-Apk {
    param(
        [string]$adb,
        [string]$apkPath,
        [string]$deviceName,
        [string]$targetDesc
    )

    if (-not $apkPath -or -not (Test-Path $apkPath)) {
        Write-Host "Error: APK not found for $targetDesc." -ForegroundColor Red
        Write-Host 'Run option [8] or --rebuild to build the APK first!' -ForegroundColor Yellow
        return $false
    }

    $item = Get-Item $apkPath
    $fileName = $item.Name
    $sizeMb = [math]::Round(($item.Length / 1MB), 2)
    $timestamp = $item.LastWriteTime.ToString('yyyy-MM-dd HH:mm')
    Write-Host ''
    Write-Host "Installing $targetDesc ($fileName, ${sizeMb} MB, built $timestamp)..." -ForegroundColor Cyan

    $cmdArgs = @()
    if (![string]::IsNullOrWhiteSpace($deviceName)) {
        $cmdArgs += @('-s', $deviceName)
    }
    # Pass -g to automatically grant runtime permissions (step counter, heart rate, notifications, camera)
    $installArgsWithGrant = $cmdArgs + @('install', '-r', '-d', '-g', $apkPath)
    & $adb $installArgsWithGrant
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'Retrying install without -g flag...' -ForegroundColor Yellow
        $installArgsFallback = $cmdArgs + @('install', '-r', '-d', $apkPath)
        & $adb $installArgsFallback
    }

    if ($LASTEXITCODE -eq 0) {
        Write-Host "SUCCESS: Installed $targetDesc (all runtime permissions granted)!" -ForegroundColor Green
        return $true
    } else {
        Write-Host "FAILED: Installation failed (Exit code: $LASTEXITCODE)." -ForegroundColor Red
        Write-Host 'Tip: If you have an existing build signed with a different key, uninstall it first on the device.' -ForegroundColor Yellow
        return $false
    }
}

function Select-TargetDevice {
    param([string]$adb)
    $lines = @(& $adb devices -l | Where-Object { $_ -match '\bdevice\b' -and $_ -notmatch 'List of devices' })
    if (!$lines -or $lines.Count -eq 0) {
        Write-Host 'No connected ADB devices found!' -ForegroundColor Red
        Write-Host 'Please connect via USB or Wi-Fi first.' -ForegroundColor Yellow
        return $null
    }

    if ($lines.Count -eq 1) {
        $deviceId = ($lines[0] -split '\s+')[0]
        return $deviceId
    }

    Write-Host ''
    Write-Host 'Multiple devices detected:' -ForegroundColor Cyan
    for ($i = 0; $i -lt $lines.Count; $i++) {
        Write-Host "  [$($i+1)] $($lines[$i])" -ForegroundColor White
    }
    $choice = Read-Host "Select target device (1-$($lines.Count))"
    $idx = [int]$choice - 1
    if ($idx -ge 0 -and $idx -lt $lines.Count) {
        return ($lines[$idx] -split '\s+')[0]
    }
    return $null
}

function Invoke-RebuildApks {
    Write-Host ''
    Write-Host 'Rebuilding Release & Debug APKs and syncing release_bin/...' -ForegroundColor Cyan
    $scriptDir = $ScriptDir
    Push-Location $scriptDir
    cmd.exe /c "gradlew.bat assembleRelease assembleDebug"
    $exitCode = $LASTEXITCODE
    Pop-Location
    if ($exitCode -eq 0) {
        Sync-ReleaseBinaries
        Write-Host 'SUCCESS: Rebuilt and synced all APKs into release_bin/!' -ForegroundColor Green
    } else {
        Write-Host "FAILED: Gradle build exited with code $exitCode." -ForegroundColor Red
    }
}

$adbPath = Find-Adb

# --- Non-Interactive CLI Argument Handling ---
if ($args.Count -gt 0) {
    $lowerArgs = $args | ForEach-Object { $_.ToString().ToLower() }
    if ($lowerArgs -contains '--help' -or $lowerArgs -contains '-h' -or $lowerArgs -contains '/?') {
        Write-Host 'DigiWear Sideloading Assistant -- Usage:' -ForegroundColor Cyan
        Write-Host '  .\sideload.bat                  Launch interactive sideloading wizard'
        Write-Host '  .\sideload.bat --watch          Install Wear OS / AOSP Watch APK to connected device'
        Write-Host '  .\sideload.bat --phone          Install Phone Companion APK to connected device'
        Write-Host '  .\sideload.bat --all            Install both Watch and Phone APKs'
        Write-Host '  .\sideload.bat --rebuild        Rebuild Release & Debug APKs and sync release_bin/'
        Write-Host '  .\sideload.bat --rebuild --all  Rebuild APKs and install both Watch and Phone APKs'
        exit 0
    }
    if ($lowerArgs -contains '--rebuild' -or $lowerArgs -contains '-rebuild') {
        Invoke-RebuildApks
        if ($lowerArgs.Count -eq 1) {
            exit $LASTEXITCODE
        }
    }
    if ($lowerArgs -contains '--watch' -or $lowerArgs -contains '-watch') {
        if (-not $adbPath) { Write-Error 'ADB not found.'; exit 1 }
        $target = Select-TargetDevice -adb $adbPath
        if ($target) {
            $apk = Get-ApkPath 'wear-os-app'
            if (Install-Apk -adb $adbPath -apkPath $apk -deviceName $target -targetDesc 'Wear OS / AOSP Smartwatch App') { exit 0 }
        }
        exit 1
    }
    if ($lowerArgs -contains '--phone' -or $lowerArgs -contains '-phone') {
        if (-not $adbPath) { Write-Error 'ADB not found.'; exit 1 }
        $target = Select-TargetDevice -adb $adbPath
        if ($target) {
            $apk = Get-ApkPath 'phone-companion-app'
            if (Install-Apk -adb $adbPath -apkPath $apk -deviceName $target -targetDesc 'Phone Companion App') { exit 0 }
        }
        exit 1
    }
    if ($lowerArgs -contains '--all' -or $lowerArgs -contains '-all') {
        if (-not $adbPath) { Write-Error 'ADB not found.'; exit 1 }
        $target = Select-TargetDevice -adb $adbPath
        if ($target) {
            $apkW = Get-ApkPath 'wear-os-app'
            $apkP = Get-ApkPath 'phone-companion-app'
            Install-Apk -adb $adbPath -apkPath $apkW -deviceName $target -targetDesc 'Wear OS / AOSP Smartwatch App' | Out-Null
            Install-Apk -adb $adbPath -apkPath $apkP -deviceName $target -targetDesc 'Phone Companion App' | Out-Null
            exit 0
        }
        exit 1
    }
}

# --- Interactive Main Program Loop ---
while ($true) {
    Write-Header

    if ($adbPath) {
        Write-Host "ADB Status: FOUND ($adbPath)" -ForegroundColor Green
    } else {
        Write-Host 'ADB Status: NOT FOUND in PATH or standard Android SDK directories!' -ForegroundColor Red
        Write-Host 'Please install Android Studio / Platform-Tools or set your PATH.' -ForegroundColor Yellow
    }

    $watchApkPreview = Get-ApkPath 'wear-os-app'
    $phoneApkPreview = Get-ApkPath 'phone-companion-app'
    if ($watchApkPreview) {
        $wi = Get-Item $watchApkPreview
        Write-Host "Watch APK : $($wi.Name) ($([math]::Round($wi.Length / 1MB, 2)) MB, $($wi.LastWriteTime.ToString('yyyy-MM-dd HH:mm')))" -ForegroundColor DarkCyan
    }
    if ($phoneApkPreview) {
        $pi = Get-Item $phoneApkPreview
        Write-Host "Phone APK : $($pi.Name) ($([math]::Round($pi.Length / 1MB, 2)) MB, $($pi.LastWriteTime.ToString('yyyy-MM-dd HH:mm')))" -ForegroundColor DarkCyan
    }
    Write-Host ''

    Write-Host '  [1] Install Wear OS / AOSP Smartwatch App (Wi-Fi Wireless Debugging)' -ForegroundColor White
    Write-Host '  [2] Install Wear OS / AOSP Smartwatch App (USB / Connected Device)' -ForegroundColor White
    Write-Host '  [3] Install Phone Companion App (USB / Wireless)' -ForegroundColor White
    Write-Host '  [4] Full Dual-Device Sideload (Smartwatch + Phone)' -ForegroundColor White
    Write-Host '  [5] Check Connected ADB Devices' -ForegroundColor White
    Write-Host '  [6] Pair New Wireless Watch (Wi-Fi 6-Digit Pairing Code)' -ForegroundColor White
    Write-Host '  [7] View Setup Guide (Wear OS, Standalone AOSP Watch & Phone)' -ForegroundColor White
    Write-Host '  [8] Rebuild Release & Debug APKs from Source (Syncs release_bin/)' -ForegroundColor White
    Write-Host '  [0] Exit' -ForegroundColor Gray
    Write-Host ''

    $choice = Read-Host 'Select an option [0-8]'

    if ($choice -eq '0') {
        Write-Host 'Goodbye! May your Digimon digivolve to greatness!' -ForegroundColor Cyan
        break
    }

    if (!$adbPath -and $choice -ne '7' -and $choice -ne '8') {
        Write-Host 'ADB is required for this operation. Please check option [7].' -ForegroundColor Red
        Read-Host 'Press [Enter] to continue'
        continue
    }

    switch ($choice) {
        '1' {
            Connect-WirelessDevice -adb $adbPath | Out-Null
            $target = Select-TargetDevice -adb $adbPath
            if ($target) {
                $apk = Get-ApkPath 'wear-os-app'
                Install-Apk -adb $adbPath -apkPath $apk -deviceName $target -targetDesc 'Wear OS / AOSP Smartwatch App' | Out-Null
            }
            Read-Host 'Press [Enter] to continue'
        }
        '2' {
            $target = Select-TargetDevice -adb $adbPath
            if ($target) {
                $apk = Get-ApkPath 'wear-os-app'
                Install-Apk -adb $adbPath -apkPath $apk -deviceName $target -targetDesc 'Wear OS / AOSP Smartwatch App' | Out-Null
            }
            Read-Host 'Press [Enter] to continue'
        }
        '3' {
            $target = Select-TargetDevice -adb $adbPath
            if ($target) {
                $apk = Get-ApkPath 'phone-companion-app'
                Install-Apk -adb $adbPath -apkPath $apk -deviceName $target -targetDesc 'Phone Companion App' | Out-Null
            }
            Read-Host 'Press [Enter] to continue'
        }
        '4' {
            Write-Host '--- DUAL-DEVICE INSTALLATION ---' -ForegroundColor Cyan
            Write-Host "First, let's install the Wear OS / AOSP Smartwatch App:" -ForegroundColor Yellow
            $watchTarget = Select-TargetDevice -adb $adbPath
            if ($watchTarget) {
                $apkWatch = Get-ApkPath 'wear-os-app'
                Install-Apk -adb $adbPath -apkPath $apkWatch -deviceName $watchTarget -targetDesc 'Wear OS / AOSP Smartwatch App' | Out-Null
            }

            Write-Host ''
            Write-Host "Next, let's install the Phone Companion App:" -ForegroundColor Yellow
            $phoneTarget = Select-TargetDevice -adb $adbPath
            if ($phoneTarget) {
                $apkPhone = Get-ApkPath 'phone-companion-app'
                Install-Apk -adb $adbPath -apkPath $apkPhone -deviceName $phoneTarget -targetDesc 'Phone Companion App' | Out-Null
            }
            Read-Host 'Press [Enter] to continue'
        }
        '5' {
            Write-Host ''
            Write-Host 'Connected ADB Devices:' -ForegroundColor Cyan
            & $adbPath devices -l
            Write-Host ''
            Read-Host 'Press [Enter] to continue'
        }
        '6' {
            Pair-WirelessDevice -adb $adbPath
        }
        '7' {
            Show-Guide
        }
        '8' {
            Invoke-RebuildApks
            Read-Host 'Press [Enter] to continue'
        }
        default {
            Write-Host 'Invalid option. Please choose between 0 and 8.' -ForegroundColor Yellow
            Start-Sleep -Seconds 1
        }
    }
}
