@param daevil.menu.BatchFileBuilder builder
@param String toDir
@param String prompt
@param String echo
if not exist "${toDir}" (
  echo "${toDir} does not exist, resolving..."
) else (
  echo "HAS DIR"
  goto echoResolved${new java.io.File(toDir).getName()}
)
${builder.call("prompt_confirm","Download? (Y/N)")}
IF ERRORLEVEL 1 (
echo. && echo "${echo}"
) else (
echo. && echo "NO download for you"
)
:echoResolved${new java.io.File(toDir).getName()}
