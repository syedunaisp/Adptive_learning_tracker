$javacDir = (Get-Command javac).Source | Split-Path
$cp = (Get-ChildItem -Path lib/*.jar | Select-Object -ExpandProperty FullName) -join ';'
$jfx = "lib/javafx/javafx-sdk-21.0.2/lib"

Write-Host "Compiling Java sources..."
& "$javacDir\javac.exe" -d bin -cp $cp --module-path $jfx --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed! Exiting."
    exit $LASTEXITCODE
}

Write-Host "Copying resources (FXML, CSS, Images)..."
$srcFolder = (Get-Item src).FullName
Get-ChildItem -Path src -Recurse -File -Include *.fxml,*.css,*.png,*.jpg,*.jpeg | ForEach-Object {
    $relativePath = $_.FullName.Substring($srcFolder.Length + 1)
    $dest = Join-Path -Path bin -ChildPath $relativePath
    $destDir = Split-Path $dest
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Force -Path $destDir | Out-Null }
    Copy-Item -Path $_.FullName -Destination $dest -Force
}

Write-Host "Starting Application..."
& "$javacDir\java.exe" --enable-native-access=ALL-UNNAMED --module-path $jfx --add-modules javafx.controls,javafx.fxml -cp "bin;$cp" tracker.ui.fx.ALIPApplication
