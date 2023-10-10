@param Object... args
!{String path = args[0].toString(); }
!{String md5File = args[1].toString(); }
    $path = '${path}'
    $md5File = '${md5File}'
    $fullPath = Resolve-Path $path
    $md5FilePath = Resolve-Path $md5File
    $hash = (Get-FileHash $fullPath -Algorithm MD5).hash
    $content = [IO.File]::ReadAllText($md5FilePath).toUpper()
    if($hash.equals($content)) { $operator = '==' } else { $operator = '=/=' }
    Write-Host $hash.equals($content) $hash $operator $content -NoNewline
