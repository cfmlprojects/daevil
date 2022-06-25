@template.daevil.script.nix.bash.function.url_file()
@raw
md5_for() {
  if command -v md5 > /dev/null; then
    md5 -q $1
  elif command -v md5sum > /dev/null ; then
    md5sum $1 | awk '{print $1}'
  else
    echo "Neither md5 nor md5sum were found in the PATH"
    exit 1
  fi
}

md5url_file() {
    zipURL=$1
    zipFile=$2
    url_file "${zipURL}" "${zipFile}"
    url_file "${zipURL}.md5" "${zipFile}.md5"
    md5=`cat ${zipFile}.md5`
    calculated_md5=`md5_for ${zipFile}`
    if [ "${calculated_md5}" = "${md5}" ]
        then
            echo "md5 ok"
        else
            echo "md5 did not match!"
            exit 1
    fi
}
@endraw