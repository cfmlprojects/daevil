@param daevil.menu.BatchFileBuilder builder
@param String url
@param String toFile
@param String prompt
@param String echo

if not exist "${toFile}" (
  echo "${toFile} does not exist, resolving..."
) else (
  echo "HAS DIR"
  goto echoResolved${new java.io.File(toFile).getName()}
)
${builder.call("prompt_confirm","Download? (Y/N)")}
IF ERRORLEVEL 1 (
    ${builder.call("url_file",url, toFile)}
    IF ERRORLEVEL 0 (
    echo. && echo "${echo}"
    ) else (
    echo. && echo "Error url_file"
    )
) else (
echo. && echo "NO download for you"
)

:echoResolved${new java.io.File(toFile).getName()}
