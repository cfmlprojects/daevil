file_age() {
	file_created_time=$(date -r "$1" +%s)
	time_now=$(date +%s)
	age=$((time_now - file_created_time))
	echo "$age"
}
