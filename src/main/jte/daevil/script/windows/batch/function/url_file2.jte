@param daevil.menu.BatchFileBuilder builder

:UrlFile
    set URLFILE_URL=%~1
    set URLFILE_FILE=%~2
    echo Downloading with powershell: %URLFILE_URL%
    powershell.exe -command "Start-BitsTransfer -Source \"%URLFILE_URL%\" -Destination \"%URLFILE_FILE%\""
