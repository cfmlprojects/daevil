@raw
if [ "${HTTP_CLIENT}" = "" ]; then
    if type -p curl >/dev/null 2>&1; then
        if [ -n "${https_proxy}" ]; then
            CURL_PROXY="-x ${https_proxy}"
        fi
        HTTP_CLIENT="curl ${CURL_PROXY} -f -L -o"
    else
        HTTP_CLIENT="wget -O"
    fi
fi

url_file() {
    sourceURL=$1
    destFile=$2
    echo "Getting ${sourceURL} to ${destFile}..."
    ${HTTP_CLIENT} "${destFile}" "${sourceURL}"
    if [ $? != 0 ]; then
        echo "Failed to download ${sourceURL}"
        echo "If you have an old version of libssl you may not have the correct"
        echo "certificate authority. Either upgrade or set HTTP_CLIENT to insecure:"
        echo "  export HTTP_CLIENT=\"wget --no-check-certificate -O\" # or"
        echo "  export HTTP_CLIENT=\"curl --insecure -f -L -o"
        rm ${destFile} 2> /dev/null
        exit 1
    fi
}
@endraw