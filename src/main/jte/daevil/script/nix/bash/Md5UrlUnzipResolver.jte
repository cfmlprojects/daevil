@param String url
@param String toDir
@param String prompt
@template.daevil.script.nix.bash.function.md5url_unzip()
@template.daevil.script.nix.bash.function.prompt_confirm()

if [ ! -d "${toDir}" ]; then
    if prompt_confirm "${prompt}"; then
      md5url_unzip "${url}" "${toDir}**@.zip" "${toDir}"
    fi
fi
