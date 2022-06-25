@param Object... args
!{String path = args[0].toString() }
function getFileConent($path) {
    $fullPath = Resolve-Path $path
    $content = [IO.File]::ReadAllText($md5FilePath)
    Write-Host $content
}
getFileContent "${path}"
