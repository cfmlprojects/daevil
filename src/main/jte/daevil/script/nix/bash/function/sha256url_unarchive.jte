@template.daevil.script.nix.bash.function.sha256url_file()
@raw
shaurl_unarchive() {
	archiveURL=$1
	shaURL=$2
	archiveFile=$3
	destDir=$4
	shaurl_file "${archiveURL}" "${shaURL}" "${archiveFile}"
	mkdir -p "${destDir}"
	echo "Expanding ${archiveURL} to ${destDir}"
	if tar -xf "${archiveFile}" --strip-components=2 -C "${destDir}"; then
		if ! rm "${archiveFile}" || ! rm "${archiveFile}.sha256.txt"; then
			echo "Could not delete either ${archiveFile} or ${archiveFile}.sha256.txt"
		fi
	else
		echo "Failed to expand ${archiveURL} to ${destDir}"
		exit 1
	fi
}
@endraw