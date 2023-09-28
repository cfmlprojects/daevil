@param daevil.menu.BatchFileBuilder builder

:file_unzip
    set zipFile=%~1
    set unzipToDir=%~2
    if not exist "%zipFile%" (
        echo. Failure: %zipFile% does not exist
        exit /B 1
    )
    echo Expanding %zipFile% with powershell to: %unzipToDir%
    ${builder.powerShellVariable("unzip","unzipped", "%zipFile%","%unzipToDir%")}
    if %ERRORLEVEL% EQU 0 del %zipFile%
    if %ERRORLEVEL% EQU 0 (
        echo. Unzipped %zipFile%
        exit /B 0
    ) else (
        echo. Failure unzipping %zipFile%
        exit /B 1
    )