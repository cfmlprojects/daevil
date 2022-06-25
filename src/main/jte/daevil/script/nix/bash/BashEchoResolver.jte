@param String toDir
@param String prompt
@param String echo
@template.daevil.script.nix.bash.function.prompt_confirm()
if [ ! -f "${toDir}" ]; then
 echo "${toDir} does not exist, resolving..."
 if prompt_confirm "${prompt}"; then
    echo "${echo}"
 else
    echo "NO download for you"
 fi
else
 echo "${toDir} already existed, not downloading!"
fi
