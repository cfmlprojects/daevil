@param daevil.menu.BatchFileBuilder builder



:md5_file
    set md5filePath=%~1
    set md5fileMD5Path=%~2

    echo.Comparing %md5filePath% with hash from %md5fileMD5Path%
    if not exist "%md5filePath%" (
        echo. Failure: %md5filePath% does not exist
        exit /B 1
    )
    if not exist "%md5fileMD5Path%" (
        echo. Failure: %md5fileMD5Path% does not exist
        exit /B 1
    )
    echo.MD5ing with powershell: %md5filePath% %md5fileMD5Path%
    ${builder.powerShellVariable("md5", "md5matches", "%md5filePath%", "%md5fileMD5Path%")}
    echo.%md5matches%
    if "%md5matches:~0,4%"=="True" (
        echo. MD5 MATCH: %md5matches%
        exit /B 0
    ) else (
        echo. Failure: MD5 MISMATCH: %md5matches%
        exit /B 1
    )
