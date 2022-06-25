@param daevil.menu.BatchFileBuilder builder
:url_file
    set URLFILE_URL=%~1
    set URLFILE_FILE=%~2
    echo."Downloading with powershell: %URLFILE_URL% -> %URLFILE_FILE%"
    ${builder.powerShellVariable("downloadFile","downloaded","%URLFILE_URL%","%URLFILE_FILE%")}
    if exist "%URLFILE_FILE%" (
        echo.%downloaded%
        echo.
        exit /B 0
    ) else (
        echo."Failed to download %URLFILE_URL%"
        exit 1
    )