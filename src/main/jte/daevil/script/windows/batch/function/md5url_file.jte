@param daevil.menu.BatchFileBuilder builder

:md5url_file
    set md5urlFileUrl=%~1
    set md5urlFileFile=%~2
    ${builder.call("url_file","%md5urlFileUrl%","%md5urlFileFile%")}
    ${builder.call("url_file","%md5urlFileUrl%.md5","%md5urlFileFile%.md5")}
    ${builder.call("md5_file","%md5urlFileFile%", "%md5urlFileFile%.md5")}
    IF ERRORLEVEL 1 (
    exit /B 1
    ) else (
    exit /B 0
    )
