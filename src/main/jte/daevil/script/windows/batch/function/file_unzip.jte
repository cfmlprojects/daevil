@param daevil.menu.BatchFileBuilder builder

:file_unzip
    set zipFile=%~1
    set unzipToDir=%~2
    if not exist "%zipFile%" (
        echo. Failure: %zipFile% does not exist
        exit /B 1
    )
    echo Expanding %zipFile% with powershell to: %unzipToDir%
<%--
    if not exist "%userprofile%/7z.exe" (
        IF EXIST "%PROGRAMFILES(X86)%" (set bit="-x64") ELSE (set bit="")
        echo.Downloading 7zip
        ${builder.call("url_file","https://www.7-zip.org/a/7zr.exe","%userprofile%/7z.exe")}
    )
    echo. calling 7zip
    echo. %userprofile%/7z.exe e "%zipFile%" -o"%unzipToDir%"
    %userprofile%/7z.exe e "%zipFile%" -o"%unzipToDir%"
--%>
    ${builder.powerShellVariable("unzip","unzipped", "%zipFile%","%unzipToDir%")}
    if %ERRORLEVEL% GEQ 1 pause
    if %ERRORLEVEL% EQU 0 del %zipFile%
    if %ERRORLEVEL% EQU 0 (
        echo. Unzipped %zipFile%
        exit /B 0
    ) else (
        echo. Failure unzipping %zipFile%
        exit /B 1
    )