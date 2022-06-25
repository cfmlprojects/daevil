@template.daevil.script.nix.bash.function.url_file()
@raw
sha_for() {
	if command -v sha256sum >/dev/null; then
		sha256sum "$1" | awk '{print $1}'
	elif command -v shasum >/dev/null; then
		shasum -a 256 "$1" | awk '{print $1}'
	else
		echo "Neither shasum nor sha256sum were found in the PATH"
		exit 1
	fi
}

shaurl_file() {
	archiveURL=$1
	shaURL=$2
	archiveFile=$3
	url_file "${shaURL}" "${archiveFile}.sha256.txt"
	url_file "${archiveURL}" "${archiveFile}"
	sha=$(awk '{print $1}' <"${archiveFile}.sha256.txt")
	calculated_sha=$(sha_for "${archiveFile}")
	if [ "${calculated_sha}" = "${sha}" ]; then
		echo "sha ok"
	else
		echo "sha did not match!"
		exit 1
	fi
}
@endraw