# Скачивает тематические фотографии для Voyago в client/src/main/resources/images.
# Источник — Wikimedia Commons (свободные лицензии). Сначала ищется фото конкретного отеля/города
# по названию, если его нет — подходящее по теме (бассейн, номер, ресторан...).
# Список авторов и ссылок на источники сохраняется в images/CREDITS.txt.
#
# Запуск из корня проекта:   powershell -ExecutionPolicy Bypass -File download-photos.ps1
# Перекачать всё заново:      powershell -ExecutionPolicy Bypass -File download-photos.ps1 -Force
param([switch]$Force)

$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$root = Join-Path $PSScriptRoot "client\src\main\resources"
$agent = "VoyagoCoursework/1.0 (student project; PowerShell)"
$api = "https://commons.wikimedia.org/w/api.php"

# путь, поиск по названию, запасной поиск по теме
$photos = @(
    @("images/hero/login.jpg", "tropical beach palm sea", "tropical beach"),
    @("images/hero/home.jpg", "beach resort sea aerial", "tropical beach"),
    @("images/countries/egypt.jpg", "Egypt landscape tourism", "Egypt"),
    @("images/countries/georgia.jpg", "Georgia country landscape tourism", "Georgia country"),
    @("images/countries/italy.jpg", "Italy landscape tourism", "Italy"),
    @("images/countries/maldives.jpg", "Maldives landscape tourism", "Maldives"),
    @("images/countries/spain.jpg", "Spain landscape tourism", "Spain"),
    @("images/countries/thailand.jpg", "Thailand landscape tourism", "Thailand"),
    @("images/countries/turkey.jpg", "Turkey landscape tourism", "Turkey"),
    @("images/countries/uae.jpg", "Dubai landscape tourism", "Dubai"),
    @("images/cities/antalya.jpg", "Antalya city view", "Antalya"),
    @("images/cities/belek.jpg", "Belek city view", "Belek"),
    @("images/cities/kemer.jpg", "Kemer city view", "Kemer"),
    @("images/cities/side.jpg", "Side city view", "Side"),
    @("images/cities/hurghada.jpg", "Hurghada city view", "Hurghada"),
    @("images/cities/sharm-el-sheikh.jpg", "Sharm El Sheikh city view", "Sharm El Sheikh"),
    @("images/cities/dubai.jpg", "Dubai city view", "Dubai"),
    @("images/cities/abu-dhabi.jpg", "Abu Dhabi city view", "Abu Dhabi"),
    @("images/cities/phuket.jpg", "Phuket city view", "Phuket"),
    @("images/cities/pattaya.jpg", "Pattaya city view", "Pattaya"),
    @("images/cities/north-male-atoll.jpg", "North Male Atoll city view", "North Male Atoll"),
    @("images/cities/batumi.jpg", "Batumi city view", "Batumi"),
    @("images/cities/gudauri.jpg", "Gudauri city view", "Gudauri"),
    @("images/cities/tbilisi.jpg", "Tbilisi city view", "Tbilisi"),
    @("images/cities/rome.jpg", "Rome city view", "Rome"),
    @("images/cities/rimini.jpg", "Rimini city view", "Rimini"),
    @("images/cities/barcelona.jpg", "Barcelona city view", "Barcelona"),
    @("images/cities/salou.jpg", "Salou city view", "Salou"),
    @("images/hotels/rixos-premium-belek-main.jpg", "Rixos Premium Belek hotel", "Belek resort hotel building"),
    @("images/hotels/rixos-premium-belek-pool.jpg", "Rixos Premium Belek pool", "hotel swimming pool resort"),
    @("images/hotels/rixos-premium-belek-room.jpg", "Rixos Premium Belek room", "hotel room bed interior"),
    @("images/hotels/rixos-premium-belek-restaurant.jpg", "Rixos Premium Belek restaurant", "hotel restaurant interior"),
    @("images/hotels/titanic-deluxe-lara-main.jpg", "Titanic Deluxe Lara hotel", "Antalya resort hotel building"),
    @("images/hotels/titanic-deluxe-lara-pool.jpg", "Titanic Deluxe Lara pool", "hotel swimming pool resort"),
    @("images/hotels/titanic-deluxe-lara-room.jpg", "Titanic Deluxe Lara room", "hotel room bed interior"),
    @("images/hotels/titanic-deluxe-lara-restaurant.jpg", "Titanic Deluxe Lara restaurant", "hotel restaurant interior"),
    @("images/hotels/maxx-royal-kemer-resort-main.jpg", "Maxx Royal Kemer Resort hotel", "Kemer resort hotel building"),
    @("images/hotels/maxx-royal-kemer-resort-pool.jpg", "Maxx Royal Kemer Resort pool", "hotel swimming pool resort"),
    @("images/hotels/maxx-royal-kemer-resort-room.jpg", "Maxx Royal Kemer Resort room", "hotel room bed interior"),
    @("images/hotels/maxx-royal-kemer-resort-restaurant.jpg", "Maxx Royal Kemer Resort restaurant", "hotel restaurant interior"),
    @("images/hotels/barut-hemera-main.jpg", "Barut Hemera hotel", "Side resort hotel building"),
    @("images/hotels/barut-hemera-pool.jpg", "Barut Hemera pool", "hotel swimming pool resort"),
    @("images/hotels/barut-hemera-room.jpg", "Barut Hemera room", "hotel room bed interior"),
    @("images/hotels/barut-hemera-restaurant.jpg", "Barut Hemera restaurant", "hotel restaurant interior"),
    @("images/hotels/delphin-imperial-lara-main.jpg", "Delphin Imperial Lara hotel", "Antalya resort hotel building"),
    @("images/hotels/delphin-imperial-lara-pool.jpg", "Delphin Imperial Lara pool", "hotel swimming pool resort"),
    @("images/hotels/delphin-imperial-lara-room.jpg", "Delphin Imperial Lara room", "hotel room bed interior"),
    @("images/hotels/delphin-imperial-lara-restaurant.jpg", "Delphin Imperial Lara restaurant", "hotel restaurant interior"),
    @("images/hotels/hilton-hurghada-plaza-main.jpg", "Hilton Hurghada Plaza hotel", "Hurghada resort hotel building"),
    @("images/hotels/hilton-hurghada-plaza-pool.jpg", "Hilton Hurghada Plaza pool", "hotel swimming pool resort"),
    @("images/hotels/hilton-hurghada-plaza-room.jpg", "Hilton Hurghada Plaza room", "hotel room bed interior"),
    @("images/hotels/hilton-hurghada-plaza-restaurant.jpg", "Hilton Hurghada Plaza restaurant", "hotel restaurant interior"),
    @("images/hotels/steigenberger-aqua-magic-main.jpg", "Steigenberger Aqua Magic hotel", "Hurghada resort hotel building"),
    @("images/hotels/steigenberger-aqua-magic-pool.jpg", "Steigenberger Aqua Magic pool", "hotel swimming pool resort"),
    @("images/hotels/steigenberger-aqua-magic-room.jpg", "Steigenberger Aqua Magic room", "hotel room bed interior"),
    @("images/hotels/steigenberger-aqua-magic-restaurant.jpg", "Steigenberger Aqua Magic restaurant", "hotel restaurant interior"),
    @("images/hotels/rixos-sharm-el-sheikh-main.jpg", "Rixos Sharm El Sheikh hotel", "Sharm El Sheikh resort hotel building"),
    @("images/hotels/rixos-sharm-el-sheikh-pool.jpg", "Rixos Sharm El Sheikh pool", "hotel swimming pool resort"),
    @("images/hotels/rixos-sharm-el-sheikh-room.jpg", "Rixos Sharm El Sheikh room", "hotel room bed interior"),
    @("images/hotels/rixos-sharm-el-sheikh-restaurant.jpg", "Rixos Sharm El Sheikh restaurant", "hotel restaurant interior"),
    @("images/hotels/atlantis-the-palm-main.jpg", "Atlantis The Palm hotel", "Dubai resort hotel building"),
    @("images/hotels/atlantis-the-palm-pool.jpg", "Atlantis The Palm pool", "hotel swimming pool resort"),
    @("images/hotels/atlantis-the-palm-room.jpg", "Atlantis The Palm room", "hotel room bed interior"),
    @("images/hotels/atlantis-the-palm-restaurant.jpg", "Atlantis The Palm restaurant", "hotel restaurant interior"),
    @("images/hotels/rove-downtown-main.jpg", "Rove Downtown hotel", "Dubai resort hotel building"),
    @("images/hotels/rove-downtown-pool.jpg", "Rove Downtown pool", "hotel swimming pool resort"),
    @("images/hotels/rove-downtown-room.jpg", "Rove Downtown room", "hotel room bed interior"),
    @("images/hotels/rove-downtown-restaurant.jpg", "Rove Downtown restaurant", "hotel restaurant interior"),
    @("images/hotels/saadiyat-rotana-resort-main.jpg", "Saadiyat Rotana Resort hotel", "Abu Dhabi resort hotel building"),
    @("images/hotels/saadiyat-rotana-resort-pool.jpg", "Saadiyat Rotana Resort pool", "hotel swimming pool resort"),
    @("images/hotels/saadiyat-rotana-resort-room.jpg", "Saadiyat Rotana Resort room", "hotel room bed interior"),
    @("images/hotels/saadiyat-rotana-resort-restaurant.jpg", "Saadiyat Rotana Resort restaurant", "hotel restaurant interior"),
    @("images/hotels/centara-grand-beach-resort-phuket-main.jpg", "Centara Grand Beach Resort Phuket hotel", "Phuket resort hotel building"),
    @("images/hotels/centara-grand-beach-resort-phuket-pool.jpg", "Centara Grand Beach Resort Phuket pool", "hotel swimming pool resort"),
    @("images/hotels/centara-grand-beach-resort-phuket-room.jpg", "Centara Grand Beach Resort Phuket room", "hotel room bed interior"),
    @("images/hotels/centara-grand-beach-resort-phuket-restaurant.jpg", "Centara Grand Beach Resort Phuket restaurant", "hotel restaurant interior"),
    @("images/hotels/hilton-pattaya-main.jpg", "Hilton Pattaya hotel", "Pattaya resort hotel building"),
    @("images/hotels/hilton-pattaya-pool.jpg", "Hilton Pattaya pool", "hotel swimming pool resort"),
    @("images/hotels/hilton-pattaya-room.jpg", "Hilton Pattaya room", "hotel room bed interior"),
    @("images/hotels/hilton-pattaya-restaurant.jpg", "Hilton Pattaya restaurant", "hotel restaurant interior"),
    @("images/hotels/kurumba-maldives-main.jpg", "Kurumba Maldives hotel", "North Male Atoll resort hotel building"),
    @("images/hotels/kurumba-maldives-pool.jpg", "Kurumba Maldives pool", "hotel swimming pool resort"),
    @("images/hotels/kurumba-maldives-room.jpg", "Kurumba Maldives room", "hotel room bed interior"),
    @("images/hotels/kurumba-maldives-restaurant.jpg", "Kurumba Maldives restaurant", "hotel restaurant interior"),
    @("images/hotels/sheraton-batumi-main.jpg", "Sheraton Batumi hotel", "Batumi resort hotel building"),
    @("images/hotels/sheraton-batumi-pool.jpg", "Sheraton Batumi pool", "hotel swimming pool resort"),
    @("images/hotels/sheraton-batumi-room.jpg", "Sheraton Batumi room", "hotel room bed interior"),
    @("images/hotels/sheraton-batumi-restaurant.jpg", "Sheraton Batumi restaurant", "hotel restaurant interior"),
    @("images/hotels/gudauri-lodge-main.jpg", "Gudauri Lodge hotel", "Gudauri resort hotel building"),
    @("images/hotels/gudauri-lodge-pool.jpg", "Gudauri Lodge pool", "hotel swimming pool resort"),
    @("images/hotels/gudauri-lodge-room.jpg", "Gudauri Lodge room", "hotel room bed interior"),
    @("images/hotels/gudauri-lodge-restaurant.jpg", "Gudauri Lodge restaurant", "hotel restaurant interior"),
    @("images/hotels/rooms-hotel-tbilisi-main.jpg", "Rooms Hotel Tbilisi hotel", "Tbilisi resort hotel building"),
    @("images/hotels/rooms-hotel-tbilisi-pool.jpg", "Rooms Hotel Tbilisi pool", "hotel swimming pool resort"),
    @("images/hotels/rooms-hotel-tbilisi-room.jpg", "Rooms Hotel Tbilisi room", "hotel room bed interior"),
    @("images/hotels/rooms-hotel-tbilisi-restaurant.jpg", "Rooms Hotel Tbilisi restaurant", "hotel restaurant interior"),
    @("images/hotels/hotel-artemide-main.jpg", "Hotel Artemide hotel", "Rome resort hotel building"),
    @("images/hotels/hotel-artemide-pool.jpg", "Hotel Artemide pool", "hotel swimming pool resort"),
    @("images/hotels/hotel-artemide-room.jpg", "Hotel Artemide room", "hotel room bed interior"),
    @("images/hotels/hotel-artemide-restaurant.jpg", "Hotel Artemide restaurant", "hotel restaurant interior"),
    @("images/hotels/grand-hotel-rimini-main.jpg", "Grand Hotel Rimini hotel", "Rimini resort hotel building"),
    @("images/hotels/grand-hotel-rimini-pool.jpg", "Grand Hotel Rimini pool", "hotel swimming pool resort"),
    @("images/hotels/grand-hotel-rimini-room.jpg", "Grand Hotel Rimini room", "hotel room bed interior"),
    @("images/hotels/grand-hotel-rimini-restaurant.jpg", "Grand Hotel Rimini restaurant", "hotel restaurant interior"),
    @("images/hotels/h10-marina-barcelona-main.jpg", "H10 Marina Barcelona hotel", "Barcelona resort hotel building"),
    @("images/hotels/h10-marina-barcelona-pool.jpg", "H10 Marina Barcelona pool", "hotel swimming pool resort"),
    @("images/hotels/h10-marina-barcelona-room.jpg", "H10 Marina Barcelona room", "hotel room bed interior"),
    @("images/hotels/h10-marina-barcelona-restaurant.jpg", "H10 Marina Barcelona restaurant", "hotel restaurant interior"),
    @("images/hotels/portaventura-hotel-caribe-main.jpg", "PortAventura Hotel Caribe hotel", "Salou resort hotel building"),
    @("images/hotels/portaventura-hotel-caribe-pool.jpg", "PortAventura Hotel Caribe pool", "hotel swimming pool resort"),
    @("images/hotels/portaventura-hotel-caribe-room.jpg", "PortAventura Hotel Caribe room", "hotel room bed interior"),
    @("images/hotels/portaventura-hotel-caribe-restaurant.jpg", "PortAventura Hotel Caribe restaurant", "hotel restaurant interior"),
    @("images/hotels/akka-antedon-main.jpg", "Akka Antedon hotel", "Kemer resort hotel building"),
    @("images/hotels/akka-antedon-pool.jpg", "Akka Antedon pool", "hotel swimming pool resort"),
    @("images/hotels/akka-antedon-room.jpg", "Akka Antedon room", "hotel room bed interior"),
    @("images/hotels/akka-antedon-restaurant.jpg", "Akka Antedon restaurant", "hotel restaurant interior")
)

$cache = @{}
$used = @{}
$credits = New-Object System.Collections.Generic.List[string]

function Find-Images([string]$query) {
    if ($cache.ContainsKey($query)) { return $cache[$query] }
    $url = $api + "?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=25" +
           "&gsrsearch=" + [uri]::EscapeDataString("filetype:bitmap " + $query) +
           "&prop=imageinfo&iiprop=url|size|mime&iiurlwidth=1400"
    $list = @()
    try {
        $r = Invoke-RestMethod -Uri $url -UserAgent $agent -TimeoutSec 30
        if ($r.query -and $r.query.pages) {
            foreach ($p in $r.query.pages.PSObject.Properties) {
                $info = $p.Value.imageinfo
                if ($null -eq $info) { continue }
                $i = $info[0]
                if ($i.mime -ne "image/jpeg") { continue }
                if ($i.width -lt 900 -or $i.width -lt $i.height) { continue }   # только горизонтальные и крупные
                $list += [pscustomobject]@{ Url = $i.thumburl; Page = $i.descriptionurl; Title = $p.Value.title }
            }
        }
    } catch { }
    $cache[$query] = $list
    return $list
}

$ok = 0; $skip = 0; $fail = 0; $n = 0
foreach ($p in $photos) {
    $n++
    $file = Join-Path $root ($p[0] -replace "/", "\")
    if ((Test-Path $file) -and -not $Force) { $skip++; continue }
    New-Item -ItemType Directory -Force -Path (Split-Path $file) | Out-Null
    $done = $false
    foreach ($query in @($p[1], $p[2])) {
        foreach ($img in (Find-Images $query)) {
            if ($used.ContainsKey($img.Url)) { continue }       # одно фото — только для одного файла
            try {
                Invoke-WebRequest -Uri $img.Url -OutFile $file -UserAgent $agent -UseBasicParsing -TimeoutSec 60
                if ((Get-Item $file).Length -gt 20000) {
                    $used[$img.Url] = $true
                    $credits.Add($p[0] + "  <-  " + $img.Page)
                    $done = $true
                    break
                }
            } catch { }
        }
        if ($done) { break }
    }
    if ($done) { $ok++; Write-Host "[$n/$($photos.Count)] $($p[0])" }
    else {
        $fail++
        Remove-Item $file -ErrorAction SilentlyContinue
        Write-Host "[$n/$($photos.Count)] НЕ НАЙДЕНО: $($p[0])" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 400
}

if ($credits.Count -gt 0) {
    $creditsFile = Join-Path $root "images\CREDITS.txt"
    $header = "Фотографии: Wikimedia Commons, свободные лицензии (CC BY / CC BY-SA / PD). Автор и лицензия — на странице файла."
    if (-not (Test-Path $creditsFile)) { Set-Content -Path $creditsFile -Value $header -Encoding UTF8 }
    Add-Content -Path $creditsFile -Value $credits -Encoding UTF8
}
Write-Host ""
Write-Host "Скачано: $ok, пропущено (уже есть): $skip, не найдено: $fail" -ForegroundColor Green
Write-Host "Если что-то не нашлось — запустите скрипт ещё раз или положите своё фото с тем же именем."
Write-Host "После этого пересоберите клиент: .\mvnw.cmd clean package"
