@param Object... args
!{String zipFile = args[0].toString();}
!{String unzipToDir = args[1].toString(); }
function unzip($zipFile, $unzipToDir) {
    Write-Host "Beginning Unzipping $zipFile to $unzipToDir"
    $Shell = New-Object -com Shell.Application
    $zipPath = Resolve-Path $zipFile
    $destinationPath = Resolve-Path $unzipToDir
    Write-Host "Unzipping paths $zipPath to $destinationPath"
    $zip_file = $Shell.namespace($zipPath)
    $destination = $Shell.namespace($destinationPath)
    $destination.Copyhere($zip_file.items())
}
unzip "${zipFile}" "${unzipToDir}"
