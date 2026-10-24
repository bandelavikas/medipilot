# MediPilot API test script
# Run it with the backend running. It creates temporary test accounts (tmp.*) and disables them at the end.

$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:8080"
$culture = [Globalization.CultureInfo]::InvariantCulture
$script:passed = 0
$script:failed = 0
$script:results = @()

function Call-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [string]$Token,
        $Body
    )

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $arguments = @{
        Method          = $Method
        Uri             = "$baseUrl$Path"
        Headers         = $headers
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $arguments["ContentType"] = "application/json"
        $arguments["Body"] = ($Body | ConvertTo-Json -Depth 5)
    }

    $status = 0
    $text = ""
    try {
        $response = Invoke-WebRequest @arguments
        $status = [int]$response.StatusCode
        $text = $response.Content
    }
    catch {
        $errorResponse = $_.Exception.Response
        if ($null -eq $errorResponse) {
            throw
        }
        $status = [int]$errorResponse.StatusCode
        try {
            $reader = New-Object System.IO.StreamReader($errorResponse.GetResponseStream())
            $text = $reader.ReadToEnd()
        }
        catch {
            $text = ""
        }
    }

    $json = $null
    if ($text) {
        try {
            $json = $text | ConvertFrom-Json
        }
        catch {
            $json = $null
        }
    }

    return [pscustomobject]@{ Status = $status; Json = $json; Text = $text }
}

function Check {
    param([string]$Name, [bool]$Condition, [string]$Detail = "")

    if ($Condition) {
        $script:passed++
        Write-Host "  PASS  $Name" -ForegroundColor Green
        $outcome = "PASS"
    }
    else {
        $script:failed++
        Write-Host "  FAIL  $Name  $Detail" -ForegroundColor Red
        $outcome = "FAIL"
    }
    $script:results += [pscustomobject]@{ Test = $Name; Result = $outcome; Detail = $Detail }
}

function ExpectStatus {
    param([string]$Name, $Response, [int]$Expected)
    Check $Name ($Response.Status -eq $Expected) "(expected $Expected, got $($Response.Status))"
}

function Section {
    param([string]$Title)
    Write-Host ""
    Write-Host $Title -ForegroundColor Cyan
}
function Count-Of {
    param($Items)
    if ($null -eq $Items) {
        return 0
    }
    return @($Items).Count
}
function Login {
    param([string]$Email, [string]$Password)
    return Call-Api -Method POST -Path "/api/auth/login" -Body @{ email = $Email; password = $Password }
}

Write-Host "MediPilot API tests" -ForegroundColor Cyan
Write-Host "Backend address: $baseUrl"

$secure = Read-Host "Admin password (press Enter to skip the admin tests)" -AsSecureString
$adminPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))

$stamp = Get-Date -Format "yyyyMMddHHmmss"
$tmpPassword = "TestPass@12345"
$tmpDoctorEmail = "tmp.doctor.$stamp@example.com"
$tmpPatientEmail = "tmp.patient.$stamp@example.com"

$today = (Get-Date).ToString("yyyy-MM-dd", $culture)
$tomorrow = (Get-Date).AddDays(1).ToString("yyyy-MM-dd", $culture)
$inThreeDays = (Get-Date).AddDays(3).ToString("yyyy-MM-dd", $culture)

# ---------------------------------------------------------------
Section "1. Basics, login and tokens"

$r = Call-Api -Path "/api/hello"
ExpectStatus "Public hello address is open" $r 200

$r = Call-Api -Path "/api/me"
ExpectStatus "Protected address without a token is refused" $r 401

$r = Call-Api -Path "/api/me" -Token "abc.def.ghi"
ExpectStatus "A fake token is refused" $r 401

$r = Login "doctor1@example.com" "WrongPassword1"
ExpectStatus "Wrong password is refused" $r 401

$doctorLogin = Login "doctor1@example.com" "Doctor@12345"
ExpectStatus "Doctor can log in" $doctorLogin 200
$patientLogin = Login "patient1@example.com" "Patient@12345"
ExpectStatus "Patient can log in" $patientLogin 200

if ($doctorLogin.Status -ne 200 -or $patientLogin.Status -ne 200) {
    Write-Host "Cannot continue: doctor1 or patient1 could not log in. Is the backend running?" -ForegroundColor Red
    exit 1
}
$doctorToken = $doctorLogin.Json.token
$patientToken = $patientLogin.Json.token

$r = Call-Api -Path "/api/me" -Token $doctorToken
Check "The token identifies the doctor" ($r.Status -eq 200 -and $r.Json.role -eq "DOCTOR") "(got $($r.Status))"

# ---------------------------------------------------------------
Section "2. Registration rules"

$r = Call-Api -Method POST -Path "/api/auth/register" -Body @{ fullName = "Dr Duplicate"; email = "doctor1@example.com"; password = "Doctor@12345"; role = "DOCTOR" }
ExpectStatus "Registering an existing email is refused" $r 409

$r = Call-Api -Method POST -Path "/api/auth/register" -Body @{ fullName = "Fake Admin"; email = "tmp.admin.$stamp@example.com"; password = "TestPass@12345"; role = "ADMIN" }
ExpectStatus "Registering as an admin is refused" $r 403

$r = Call-Api -Method POST -Path "/api/auth/register" -Body @{ fullName = "Short Password"; email = "tmp.short.$stamp@example.com"; password = "short"; role = "PATIENT" }
ExpectStatus "A too-short password is refused" $r 400

$r = Call-Api -Method POST -Path "/api/auth/register" -Body @{ fullName = "Bad Email"; email = "not-an-email"; password = "TestPass@12345"; role = "PATIENT" }
ExpectStatus "An invalid email is refused" $r 400

# ---------------------------------------------------------------
Section "3. Role-based access"

$r = Call-Api -Path "/api/admin/stats" -Token $doctorToken
ExpectStatus "A doctor cannot open the admin area" $r 403

$r = Call-Api -Path "/api/doctor/patients" -Token $patientToken
ExpectStatus "A patient cannot open the doctor area" $r 403

$r = Call-Api -Path "/api/patient/schedule" -Token $doctorToken
ExpectStatus "A doctor cannot open the patient schedule" $r 403

$r = Call-Api -Path "/api/patient/adherence" -Token $doctorToken
ExpectStatus "A doctor cannot open a patient's own adherence address" $r 403

# ---------------------------------------------------------------
Section "4. Temporary accounts and doctor-patient assignment"

$r = Call-Api -Method POST -Path "/api/auth/register" -Body @{ fullName = "Temp Patient"; email = $tmpPatientEmail; password = $tmpPassword; role = "PATIENT" }
ExpectStatus "A new patient can register" $r 201
$tmpPatientToken = $r.Json.token

$r = Call-Api -Method POST -Path "/api/auth/register" -Body @{ fullName = "Dr Temp"; email = $tmpDoctorEmail; password = $tmpPassword; role = "DOCTOR"; specialization = "Testing" }
ExpectStatus "A new doctor can register" $r 201
$tmpDoctorToken = $r.Json.token

$r = Call-Api -Method POST -Path "/api/doctor/patients" -Token $doctorToken -Body @{ email = $tmpPatientEmail }
ExpectStatus "A doctor can add a patient to their list" $r 201
$tmpPatientId = $r.Json.patientId

$r = Call-Api -Method POST -Path "/api/doctor/patients" -Token $doctorToken -Body @{ email = $tmpPatientEmail }
ExpectStatus "Adding the same patient twice is refused" $r 409

$r = Call-Api -Method POST -Path "/api/doctor/patients" -Token $tmpDoctorToken -Body @{ email = $tmpPatientEmail }
ExpectStatus "Another doctor cannot take an already assigned patient" $r 409

$r = Call-Api -Method POST -Path "/api/doctor/patients" -Token $doctorToken -Body @{ email = "nobody.$stamp@example.com" }
ExpectStatus "Adding an unknown email is refused" $r 404

$r = Call-Api -Path "/api/doctor/patients" -Token $tmpDoctorToken
Check "A doctor with no patients sees an empty list" ($r.Status -eq 200 -and (Count-Of $r.Json) -eq 0) "(got $($r.Status), count $(Count-Of $r.Json))"

# ---------------------------------------------------------------
Section "5. Prescriptions and automatic schedules"

$rx = @{
    patientId           = $tmpPatientId
    medicineName        = "Test Medicine A"
    dosage              = "1 tablet"
    frequency           = "ONCE_DAILY"
    administrationTimes = @("09:00")
    startDate           = $tomorrow
    endDate             = $inThreeDays
    instructions        = "Test data"
}

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $doctorToken -Body $rx
ExpectStatus "A doctor can prescribe to their own patient" $r 201
$rxId = $r.Json.id

$bad = $rx.Clone()
$bad.frequency = "TWICE_DAILY"
$r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $doctorToken -Body $bad
ExpectStatus "Dose times that do not match the frequency are refused" $r 400

$bad = $rx.Clone()
$bad.endDate = $today
$r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $doctorToken -Body $bad
ExpectStatus "An end date before the start date is refused" $r 400

$bad = $rx.Clone()
$bad.frequency = "TWICE_DAILY"
$bad.administrationTimes = @("09:00", "09:00")
$r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $doctorToken -Body $bad
ExpectStatus "Two identical dose times are refused" $r 400

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $tmpDoctorToken -Body $rx
ExpectStatus "Another doctor cannot prescribe to this patient" $r 404

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $tmpPatientToken -Body $rx
ExpectStatus "A patient cannot create a prescription" $r 403

$r = Call-Api -Path "/api/patient/schedule?from=$tomorrow&to=$inThreeDays" -Token $tmpPatientToken
Check "The schedule was created automatically (3 doses)" ($r.Status -eq 200 -and @($r.Json).Count -eq 3) "(got $($r.Status), count $(@($r.Json).Count))"
$doses = @($r.Json)
$firstDose = $doses[0]

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions/$rxId/schedule" -Token $doctorToken
Check "Generating the schedule again creates no duplicates" ($r.Status -eq 200 -and $r.Json.dosesCreated -eq 0) "(got $($r.Status))"

$r = Call-Api -Path "/api/patient/schedule?from=$tomorrow&to=$inThreeDays" -Token $tmpPatientToken
Check "Still exactly 3 doses after regenerating" (@($r.Json).Count -eq 3) "(count $(@($r.Json).Count))"

$r = Call-Api -Path "/api/patient/prescriptions" -Token $tmpPatientToken
Check "The patient can see their prescription" ($r.Status -eq 200 -and @($r.Json).Count -eq 1) "(got $($r.Status))"

$r = Call-Api -Path "/api/doctor/patients/$tmpPatientId/prescriptions" -Token $doctorToken
Check "The doctor can see the patient's prescriptions" ($r.Status -eq 200 -and @($r.Json).Count -eq 1) "(got $($r.Status))"

$r = Call-Api -Path "/api/doctor/patients/$tmpPatientId/prescriptions" -Token $tmpDoctorToken
ExpectStatus "Another doctor cannot see this patient's prescriptions" $r 404

# ---------------------------------------------------------------
Section "6. Taken and Missed rules (instant checks)"

Check "A dose due tomorrow has both buttons switched off" ($firstDose.status -eq "PENDING" -and $firstDose.canMarkTaken -eq $false -and $firstDose.canMarkMissed -eq $false)

$r = Call-Api -Method POST -Path "/api/patient/schedule/$($firstDose.id)/respond" -Token $tmpPatientToken -Body @{ status = "TAKEN" }
ExpectStatus "A dose due tomorrow cannot be marked Taken yet" $r 400

$r = Call-Api -Method POST -Path "/api/patient/schedule/$($firstDose.id)/respond" -Token $tmpPatientToken -Body @{ status = "MISSED" }
ExpectStatus "A future dose cannot be marked Missed" $r 400

$r = Call-Api -Method POST -Path "/api/patient/schedule/$($firstDose.id)/respond" -Token $tmpPatientToken -Body @{ status = "PENDING" }
ExpectStatus "Pending is not an allowed answer" $r 400

$r = Call-Api -Method POST -Path "/api/patient/schedule/$($firstDose.id)/respond" -Token $patientToken -Body @{ status = "TAKEN" }
ExpectStatus "Another patient cannot answer this dose" $r 404

$r = Call-Api -Method POST -Path "/api/patient/schedule/$($firstDose.id)/respond" -Token $doctorToken -Body @{ status = "TAKEN" }
ExpectStatus "A doctor cannot answer a patient's dose" $r 403

# ---------------------------------------------------------------
Section "7. Taken and Missed rules (timed checks, about 2 minutes)"

$due = (Get-Date).AddMinutes(2)
$dueTime = $due.ToString("HH:mm", $culture)
$dueDate = $due.ToString("yyyy-MM-dd", $culture)

foreach ($name in @("Test Medicine B", "Test Medicine C")) {
    $soon = @{
        patientId           = $tmpPatientId
        medicineName        = $name
        dosage              = "1 tablet"
        frequency           = "ONCE_DAILY"
        administrationTimes = @($dueTime)
        startDate           = $dueDate
        endDate             = $dueDate
    }
    $r = Call-Api -Method POST -Path "/api/doctor/prescriptions" -Token $doctorToken -Body $soon
    ExpectStatus "Prescription '$name' with a dose due soon is created" $r 201
    if ($name -eq "Test Medicine B") { $rxBId = $r.Json.id }
}

$r = Call-Api -Path "/api/patient/schedule?from=$dueDate&to=$dueDate" -Token $tmpPatientToken
$todayDoses = @($r.Json)
$doseB = $todayDoses | Where-Object { $_.medicineName -eq "Test Medicine B" } | Select-Object -First 1
$doseC = $todayDoses | Where-Object { $_.medicineName -eq "Test Medicine C" } | Select-Object -First 1
Check "Both doses due soon are in today's schedule" ($null -ne $doseB -and $null -ne $doseC)

if ($null -ne $doseB -and $null -ne $doseC) {
    Check "A dose due soon can be marked Taken but not Missed yet" ($doseC.canMarkTaken -eq $true -and $doseC.canMarkMissed -eq $false)

    $r = Call-Api -Method POST -Path "/api/patient/schedule/$($doseB.id)/respond" -Token $tmpPatientToken -Body @{ status = "MISSED" }
    ExpectStatus "Marking a dose Missed before its time is refused" $r 400

    $r = Call-Api -Method POST -Path "/api/patient/schedule/$($doseC.id)/respond" -Token $tmpPatientToken -Body @{ status = "TAKEN" }
    Check "A dose due soon can be marked Taken" ($r.Status -eq 200 -and $r.Json.status -eq "TAKEN" -and $null -ne $r.Json.respondedAt) "(got $($r.Status))"

    $r = Call-Api -Method POST -Path "/api/patient/schedule/$($doseC.id)/respond" -Token $tmpPatientToken -Body @{ status = "MISSED" }
    ExpectStatus "A recorded dose cannot be changed" $r 409

    $scheduled = [datetime]::ParseExact("$dueDate $dueTime", "yyyy-MM-dd HH:mm", $culture)
    $wait = [int][Math]::Ceiling(($scheduled - (Get-Date)).TotalSeconds) + 5
    if ($wait -gt 0) {
        Write-Host "  ... waiting $wait seconds for the dose time to pass" -ForegroundColor Yellow
        Start-Sleep -Seconds $wait
    }

    $r = Call-Api -Method POST -Path "/api/patient/schedule/$($doseB.id)/respond" -Token $tmpPatientToken -Body @{ status = "MISSED" }
    Check "After its time, a dose can be marked Missed" ($r.Status -eq 200 -and $r.Json.status -eq "MISSED") "(got $($r.Status))"
}

# ---------------------------------------------------------------
Section "8. Adherence reports"

$r = Call-Api -Path "/api/patient/adherence" -Token $tmpPatientToken
Check "Patient report: 1 taken, 1 missed, 50 percent, nothing awaiting" ($r.Status -eq 200 -and $r.Json.taken -eq 1 -and $r.Json.missed -eq 1 -and $r.Json.adherencePercent -eq 50 -and $r.Json.awaitingResponse -eq 0) "(got $($r.Status))"

$r = Call-Api -Path "/api/doctor/patients/$tmpPatientId/adherence" -Token $doctorToken
Check "Doctor report shows the same numbers" ($r.Status -eq 200 -and $r.Json.taken -eq 1 -and $r.Json.missed -eq 1 -and $r.Json.adherencePercent -eq 50) "(got $($r.Status))"

$r = Call-Api -Path "/api/doctor/patients/$tmpPatientId/adherence" -Token $tmpDoctorToken
ExpectStatus "Another doctor cannot see this patient's adherence" $r 404

$r = Call-Api -Path "/api/patient/adherence?from=$tomorrow&to=$today" -Token $tmpPatientToken
ExpectStatus "A backwards date range is refused" $r 400

$r = Call-Api -Path "/api/doctor/patients/$tmpPatientId/adherence?from=$tomorrow&to=$inThreeDays" -Token $doctorToken
Check "Future doses are upcoming, never missed" ($r.Status -eq 200 -and $r.Json.missed -eq 0 -and $r.Json.upcoming -ge 3) "(got $($r.Status))"

# ---------------------------------------------------------------
Section "9. Cancelling prescriptions"

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions/$rxId/cancel" -Token $tmpDoctorToken
ExpectStatus "Another doctor cannot cancel this prescription" $r 404

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions/$rxId/cancel" -Token $doctorToken
Check "A doctor can cancel their prescription and its 3 future doses are removed" ($r.Status -eq 200 -and $r.Json.futureDosesRemoved -eq 3) "(got $($r.Status))"

$r = Call-Api -Method POST -Path "/api/doctor/prescriptions/$rxId/cancel" -Token $doctorToken
ExpectStatus "Cancelling twice is refused" $r 409

$r = Call-Api -Path "/api/patient/schedule?from=$tomorrow&to=$inThreeDays" -Token $tmpPatientToken
Check "The cancelled prescription no longer has upcoming doses" ($r.Status -eq 200 -and (Count-Of $r.Json) -eq 0) "(got $($r.Status), count $(Count-Of $r.Json))"

$r = Call-Api -Path "/api/patient/adherence" -Token $tmpPatientToken
Check "Answered doses stay in the history after cancelling" ($r.Status -eq 200 -and $r.Json.taken -eq 1 -and $r.Json.missed -eq 1)

# ---------------------------------------------------------------
Section "10. Call service"

$r = Call-Api -Method POST -Path "/api/calls/ticket"
ExpectStatus "A call ticket without a login is refused" $r 401

$r = Call-Api -Method POST -Path "/api/calls/ticket" -Token $patientToken
Check "A patient gets a call ticket" ($r.Status -eq 200 -and $r.Json.ticket.Length -eq 43) "(got $($r.Status))"
$wsUrl = "$baseUrl/ws/calls?ticket=$($r.Json.ticket)"

$first = & curl.exe -s -i -N --max-time 3 -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" $wsUrl | Select-Object -First 1
Check "The call service accepts a valid ticket" ("$first" -like "HTTP/1.1 101*") "(got '$first')"

$second = & curl.exe -s -i -N --max-time 3 -H "Connection: Upgrade" -H "Upgrade: websocket" -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" $wsUrl | Select-Object -First 1
Check "A ticket can only be used once" ("$second" -like "HTTP/1.1 403*") "(got '$second')"

$r = Call-Api -Path "/api/patient/my-doctor" -Token $patientToken
Check "A patient can find their doctor" ($r.Status -eq 200 -and $r.Json.fullName -eq "Dr Asha Verma") "(got $($r.Status))"

# ---------------------------------------------------------------
Section "11. Admin controls"

if ([string]::IsNullOrEmpty($adminPassword)) {
    Write-Host "  (skipped: no admin password was entered)" -ForegroundColor Yellow
}
else {
    $adminLogin = Login "admin@example.com" $adminPassword
    ExpectStatus "Admin can log in" $adminLogin 200

    if ($adminLogin.Status -eq 200) {
        $adminToken = $adminLogin.Json.token

        $r = Call-Api -Path "/api/admin/stats" -Token $adminToken
        Check "Admin can read the statistics" ($r.Status -eq 200 -and $r.Json.totalUsers -gt 0) "(got $($r.Status))"

        $r = Call-Api -Path "/api/doctor/patients" -Token $adminToken
        ExpectStatus "Admin cannot see doctor or patient data" $r 403

        $r = Call-Api -Path "/api/patient/schedule" -Token $adminToken
        ExpectStatus "Admin cannot see a patient's schedule" $r 403

        $r = Call-Api -Method POST -Path "/api/calls/ticket" -Token $adminToken
        ExpectStatus "Admin cannot use calls" $r 403

        $r = Call-Api -Path "/api/admin/users" -Token $adminToken
        ExpectStatus "Admin can list accounts" $r 200
        $users = @($r.Json)
        $tmpPatientUser = $users | Where-Object { $_.email -eq $tmpPatientEmail } | Select-Object -First 1
        $tmpDoctorUser = $users | Where-Object { $_.email -eq $tmpDoctorEmail } | Select-Object -First 1
        $adminUser = $users | Where-Object { $_.email -eq "admin@example.com" } | Select-Object -First 1

        $r = Call-Api -Method PATCH -Path "/api/admin/users/$($adminUser.id)/status" -Token $adminToken -Body @{ active = $false }
        ExpectStatus "Admin cannot disable their own account" $r 400

        $r = Call-Api -Method PATCH -Path "/api/admin/users/$($tmpPatientUser.id)/status" -Token $adminToken -Body @{ active = $false }
        Check "Admin can disable an account" ($r.Status -eq 200 -and $r.Json.active -eq $false) "(got $($r.Status))"

        $r = Login $tmpPatientEmail $tmpPassword
        ExpectStatus "A disabled account cannot log in" $r 403

        $r = Call-Api -Path "/api/me" -Token $tmpPatientToken
        ExpectStatus "A disabled account's old token stops working" $r 401

        $r = Call-Api -Method PATCH -Path "/api/admin/users/$($tmpPatientUser.id)/status" -Token $adminToken -Body @{ active = $true }
        Check "Admin can enable the account again" ($r.Status -eq 200 -and $r.Json.active -eq $true) "(got $($r.Status))"

        $r = Login $tmpPatientEmail $tmpPassword
        ExpectStatus "The re-enabled account can log in again" $r 200

        # Clean-up: switch the temporary accounts off.
        $null = Call-Api -Method PATCH -Path "/api/admin/users/$($tmpPatientUser.id)/status" -Token $adminToken -Body @{ active = $false }
        $null = Call-Api -Method PATCH -Path "/api/admin/users/$($tmpDoctorUser.id)/status" -Token $adminToken -Body @{ active = $false }
        Write-Host "  (the temporary tmp.* accounts were disabled)" -ForegroundColor Yellow
    }
}

# ---------------------------------------------------------------
Write-Host ""
Write-Host "==============================================" -ForegroundColor Cyan
Write-Host "Passed: $script:passed   Failed: $script:failed" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$file = Join-Path $PSScriptRoot "test-results.txt"
$header = "MediPilot API test results - " + (Get-Date -Format "yyyy-MM-dd HH:mm") + "`r`nPassed: $script:passed   Failed: $script:failed`r`n"
$table = $script:results | Format-Table -AutoSize -Wrap | Out-String -Width 220
Set-Content -Path $file -Value ($header + $table)
Write-Host "Results saved to $file"