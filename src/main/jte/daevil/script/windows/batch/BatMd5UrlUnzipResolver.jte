@param daevil.menu.BatchFileBuilder builder
@param String url
@param String toDir
@param String prompt

if not exist "${toDir}" (
  echo "${toDir} does not exist, resolving..."
) else (
  echo "HAS DIR"
  goto echoResolved${new java.io.File(toDir).getName()}
)

${builder.call("prompt_confirm",prompt)}
IF ERRORLEVEL 1 (
    ${builder.call("md5url_unzip",url,toDir+".zip", toDir)}
    IF ERRORLEVEL 0 (
    echo. && echo "Successfully unzipped"
    ) else (
    echo. && echo "Error md5url_unzip"
    )
) else (
echo. && echo "NO download for you"
)

:echoResolved${new java.io.File(toDir).getName()}
