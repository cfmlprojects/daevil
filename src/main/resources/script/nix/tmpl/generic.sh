#!/bin/sh
### BEGIN INIT INFO
# Provides:          #SERVICE_NAME#
# Required-Start:    $local_fs $remote_fs $network $syslog
# Required-Stop:     $local_fs $remote_fs $network $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start/Stop #SERVICE_NAME#
### END INIT INFO
 
SERVICE_USER=#SERVICE_USER#
SERVICE_CTL=#CTLSCRIPT#

case "$1" in
start)
echo "Starting #SERVICE_NAME#..."
start-stop-daemon --start --background --chuid $SERVICE_USER --exec $SERVICE_CTL -- #START_ARG#
exit $?
;;
stop)
echo "Stopping #SERVICE_NAME#..."
 
start-stop-daemon --start --quiet --background --chuid $SERVICE_USER --exec $SERVICE_CTL -- #STOP_ARG#
exit $?
;;
log)
echo "Showing #SERVICE_NAME#.log..."
tail -500f #LOG_FILE_OUT#
;;
*)
echo "Usage: /etc/init.d/#SERVICE_NAME# {start|stop}"
exit 1
;;
esac
exit 0