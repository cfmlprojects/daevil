@template.daevil.script.nix.bash.function.md5url_file()
@raw
md5url_unzip() {
    zipURL=$1
    zipFile=$2
    destDir=$3
    md5url_file "${zipURL}" "${zipFile}"
    unzip "${zipFile}" -d "${destDir}";
    rm ${zipFile} 2> /dev/null
    rm ${zipFile}.md5 2> /dev/null
}
@endraw