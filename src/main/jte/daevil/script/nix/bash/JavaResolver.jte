@param String baseUrl
@param String prompt
@raw
# If $JVM_JAVA_HOME is specified it will override JAVA_HOME and Java will not automatically be installed
#set -x

JVM_MAJOR_VERSION_DEFAULT=8
JVM_RELEASE_DEFAULT=latest
JVM_IMPL_DEFAULT=hotspot
JVM_TYPE_DEFAULT=jdk
set -e
if [ `getconf LONG_BIT` = "64" ]; then
    JVM_ARCH_DEFAULT=x64
else
    JVM_ARCH_DEFAULT=x32
fi
JVM_OS_DEFAULT="linux"
case "${OSTYPE}" in
darwin*)  JVM_OS_DEFAULT="mac" ;;
esac

# Check for environment JVM version and release variables, otherwise specify the default.
if [ -z "$JVM_MAJOR_VERSION" ]; then
	JVM_MAJOR_VERSION=${JVM_MAJOR_VERSION_DEFAULT}
fi
if [ -z "$JVM_RELEASE" ]; then
	JVM_RELEASE=${JVM_RELEASE_DEFAULT} # latest, jdk8u192-b12, ...
fi
if [ -z "$JVM_TYPE" ]; then
	JVM_TYPE=${JVM_TYPE_DEFAULT} # jdk, jre
fi
if [ -z "$JVM_IMPL" ]; then
	JVM_IMPL=${JVM_IMPL_DEFAULT} # hotspot, openj9
fi
if [ -z "$JVM_OS" ]; then
	JVM_OS=${JVM_OS_DEFAULT} # windows, linux, mac
fi
if [ -z "$JVM_ARCH" ]; then
	JVM_ARCH=${JVM_ARCH_DEFAULT} # x64, x32, ppc64, s390x, ppc64le, aarch64
fi
if [ -z "$JVM_HEAP" ]; then
	JVM_HEAP=normal # normal, large
fi

# Check for environment variables for where to store the JVM, otherwise specify the default.
if [ -z "$JVM_DIR" ]; then
	JVM_DIR="${PROJECT_ROOT}/gradle/jvm/${JVM_MAJOR_VERSION}/${JVM_TYPE}${JVM_RELEASE}"
fi
if [ -z "$JVM_INFO_FILE" ]; then
	JVM_INFO_FILE="/tmp/jvm_info_${JVM_RELEASE}.json"
fi
if [ -z "$JVM_ARCHIVE" ]; then
	JVM_ARCHIVE="/tmp/jvm_${JVM_RELEASE}.tar.gz"
fi
# Check for environment variable defining different http client, otherwise use curl
if [ -z "$HTTP_CLIENT" ]; then
	HTTP_CLIENT="curl -sS -f -L -o"
fi
@endraw
@template.daevil.script.nix.bash.function.sha256url_unarchive()
@template.daevil.script.nix.bash.function.string_replace()
@template.daevil.script.nix.bash.function.file_age()
@raw
jvm_install() {
	echo "Requesting Java ${JVM_MAJOR_VERSION} ${JVM_RELEASE} release"
	needsJvmInfo=true
	if [ -f "${JVM_INFO_FILE}" ]; then
		fileAge=$(file_age "${JVM_INFO_FILE}")
		if [ "$fileAge" -lt 86400 ]; then
			needsJvmInfo=false
			echo "${JVM_INFO_FILE} is less than 24 hours old, not re-downloading"
		fi
	fi
	if [ "$needsJvmInfo" = "true" ]; then
		jvmInfoURL="https://api.adoptopenjdk.net/v2/info/releases/openjdk${JVM_MAJOR_VERSION}?openjdk_impl=${JVM_IMPL}&arch=${JVM_ARCH}&os=${JVM_OS}&type=${JVM_TYPE}&release=${JVM_RELEASE}"
		url_file "${jvmInfoURL}" "${JVM_INFO_FILE}"
	fi
	releaseName=$(grep -Po '"release_name":.*?[^\\]",' "${JVM_INFO_FILE}" | awk -F'"' '{print $4}')
	binaryURL=$(grep -Po '"binary_link":.*?[^\\]",' "${JVM_INFO_FILE}" | awk -F'"' '{print $4}')
	shaURL=$(grep -Po '"checksum_link":.*?[^\\]",' "${JVM_INFO_FILE}" | awk -F'"' '{print $4}')
	needsJvmDownload=true
	if [ "$JVM_RELEASE" = "latest" ]; then
		JVM_DIR=$(string_replace "${JVM_DIR}" "latest" "${releaseName}")
	fi
	if [ -f "${JVM_DIR}/bin/java" ] && [ -x "${JVM_DIR}/bin/java" ]; then
		needsJvmDownload=false
		echo "${JVM_DIR} exists, not re-downloading"
	fi
	if [ "$needsJvmDownload" = "true" ]; then
		echo "Downloading ${binaryURL}"
		shaurl_unarchive "${binaryURL}" "${shaURL}" "${JVM_ARCHIVE}" "${JVM_DIR}"
	fi
	export JAVA_HOME=${JVM_DIR}
	echo "Java ${JVM_TYPE} ${JVM_MAJOR_VERSION} ${releaseName} - JAVA_HOME: ${JVM_DIR}"
}

if [ -z "$JVM_JAVA_HOME" ]; then
	if [ -f "${JVM_DIR}/bin/java" ] && [ -x "${JVM_DIR}/bin/java" ]; then
		JAVA_HOME=${JVM_DIR}
	else
		echo "${JVM_DIR} does not exist!  Getting JVM..."
		jvm_install
	fi
else
	echo "Using JVM_JAVA_HOME: $JVM_JAVA_HOME"
	JAVA_HOME=$JVM_JAVA_HOME
fi
export JAVA_HOME
export javacmd=${JAVA_HOME}/bin/java
@endraw