@param Object... args
!{String url = args[0].toString()}
!{String file = args[1].toString()}
function downloadFile($url, $targetFile) {
    $WebClient = New-Object System.Net.WebClient
    $WebClient.DownloadFile($url,$targetFile)
    Write-Host "*****Downloaded $url to $targetFile"
}
downloadFile "${url}" "${file}"