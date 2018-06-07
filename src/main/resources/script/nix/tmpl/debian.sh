#!/bin/sh
#
# /etc/init.d/#SERVICE_NAME# -- startup script for #SERVICE_NAME#
#
### BEGIN INIT INFO
# Provides:             #SERVICE_NAME#
# Required-Start:       $remote_fs $network
# Required-Stop:        $remote_fs $network
# Default-Start:        2 3 4 5
# Default-Stop:         0 1 6
# Short-Description:    #SERVICE_DESC#
# Description:          #SERVICE_DESC#
### END INIT INFO

SERVICE_DIR=#SERVICE_DIR#
SERVICE_NAME=#SERVICE_NAME#
SERVICE_DESC="#SERVICE_DESC#"
CTLSCRIPT="#CTLSCRIPT#"
START_ARG="#START_ARG#"
PID_FILE="#PID_FILE#"
STOP_ARG="#STOP_ARG#"
LOG_FILE_OUT="#LOG_FILE_OUT#"
LOG_FILE_ERR="#LOG_FILE_ERR#"
GREP_SUCCESS_FILE="#GREP_SUCCESS_FILE#"
GREP_SUCCESS_STRING="#GREP_SUCCESS_STRING#"


# Check privileges
if [ `id -u` -ne 0 ]; then
  echo "You need root privileges to run this script"
  exit 1
fi

# Make sure #SERVICE_NAME# is started with system locale
if [ -r /etc/default/locale ]; then
  . /etc/default/locale
  export LANG
fi

. /lib/lsb/init-functions

if [ -r /etc/default/rcS ]; then
  . /etc/default/rcS
fi

# Overwrite settings from default file
if [ -f "$DEFAULT" ]; then
  . "$DEFAULT"
fi

# Location of #SERVICE_NAME#
if [ -z "$SERVICE_HOME" ]; then
  SERVICE_HOME=$SERVICE_DIR
fi
export SERVICE_HOME

# Run as #SERVICE_NAME# user
# Example of user creation for Debian based:
# adduser --system --group --no-create-home --home $SERVICE_HOME --disabled-login #SERVICE_USER#
if [ -z "$SERVICE_USER" ]; then
  SERVICE_USER=#SERVICE_USER#
fi

# Check #SERVICE_NAME# user
id $SERVICE_USER > /dev/null 2>&1
if [ $? -ne 0 -o -z "$SERVICE_USER" ]; then
  log_failure_msg "User \"$SERVICE_USER\" does not exist..."
  exit 1
fi

# Check owner of SERVICE_HOME
if [ ! $(stat -L -c "%U" "$SERVICE_HOME") = $SERVICE_USER ]; then
  log_failure_msg "The user \"$SERVICE_USER\" is not owner of \"$SERVICE_HOME\""
  exit 1
fi

# Check startup file
#if [ ! -x "$SERIVE_SCRIPT" ]; then
#  log_failure_msg "$SERVICE_SCRIPT is not an executable!"
#  exit 1
#fi

# The amount of time to wait for startup
if [ -z "$STARTUP_WAIT" ]; then
  STARTUP_WAIT=30
fi

# The amount of time to wait for shutdown
if [ -z "$SHUTDOWN_WAIT" ]; then
  SHUTDOWN_WAIT=30
fi

if [ -z "$SERVICE_LOG" ]; then
  if [ ! -z "$LOG_FILE_OUT" ]; then
    SERVICE_LOG=$LOG_FILE_OUT
  else
    SERVICE_LOG=/var/log/#SERVICE_NAME#/console.log
  fi
fi
export SERVICE_LOG


# Location to set the pid file
if [ -z "$SERVICE_PIDFILE" ]; then
  SERVICE_PIDFILE=$PID_FILE
fi
export SERVICE_PIDFILE

# Helper function to check status of #SERVICE_NAME# service
check_status() {
  log_daemon_msg "Checking $SERVICE_PIDFILE for status"
  if [ -f $SERVICE_PIDFILE ]; then
    read ppid < $SERVICE_PIDFILE
    if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
      log_daemon_msg "$SERVICE_NAME is running (pid $ppid)"
      return 0
    else
     log_daemon_msg "$SERVICE_NAME dead but pid file exists"
      return 1
    fi
  fi
  log_daemon_msg "$SERVICE_NAME is not running"
  return 3
}

case "$1" in
 start)
  log_daemon_msg "Starting $SERVICE_DESC as $SERVICE_USER" "$SERVICE_NAME"
  check_status
  status_start=$?
  if [ $status_start -eq 3 ]; then
    mkdir -p $(dirname "$SERVICE_PIDFILE")
    mkdir -p $(dirname "$SERVICE_LOG")
    touch $SERVICE_LOG
    chown $SERVICE_USER $SERVICE_LOG
    chown $SERVICE_USER $(dirname "$SERVICE_PIDFILE") || true
    cat /dev/null > "$SERVICE_LOG"
    log_daemon_msg "CALLING start-stop-daemon --start --user "$SERVICE_USER" --pidfile "$SERVICE_PIDFILE" $CTLSCRIPT $START_ARG > \$SERVICE_LOG 2>&1 &"
    start-stop-daemon --start --user "$SERVICE_USER"\
    --chuid "$SERVICE_USER" --chdir "$SERVICE_HOME" --pidfile "$SERVICE_PIDFILE" \
    --exec /bin/bash -- -c "$CTLSCRIPT $START_ARG > $SERVICE_LOG 2>&1 &"
    sleep 2  # check that it didn't error out right away
    check_status
    status_start=$?
    if [ $status_start -ne 0 ]; then
      log_failure_msg "$SERVICE_DESC could not start"
      exit 1
    fi
    count=0
    launched=0
    until [ $count -gt $STARTUP_WAIT ]
    do
      if [ -z "$GREP_SUCCESS_STRING" ] ; then
        launched=1
        break
      fi
      grep "$GREP_SUCCESS_STRING" "$GREP_SUCCESS_FILE" > /dev/null
      if [ $? -eq 0 ] ; then
        launched=1
        break
      fi
      sleep 1
      count=$((count + 1));
    done

    if check_status; then
      log_end_msg 0
    else
      log_end_msg 1
    fi

    if [ $launched -eq 0 ]; then
      log_warning_msg "$SERVICE_DESC hasn't started within the timeout allowed"
      log_warning_msg "please review file \"$SERVICE_LOG\" to see the status of the service"
    fi
  elif [ $status_start -eq 1 ]; then
    log_failure_msg "$SERVICE_DESC is not running but the pid file exists"
    exit 1
  elif [ $status_start -eq 0 ]; then
    log_success_msg "$SERVICE_DESC (already running)"
  fi
 ;;
 stop)
  check_status
  status_stop=$?
  if [ $status_stop -eq 0 ]; then
    read kpid < "$SERVICE_PIDFILE"
    log_daemon_msg "Stopping $SERVICE_DESC" "$SERVICE_NAME"
    
    children_pids=$(pgrep -P $kpid)

    start-stop-daemon --stop --quiet --pidfile "$SERVICE_PIDFILE" \
    --user "$SERVICE_USER" --retry=TERM/$SHUTDOWN_WAIT/KILL/5 \
    >/dev/null 2>&1
    
    if [ $? -eq 2 ]; then
      log_failure_msg "$SERVICE_DESC can't be stopped"
      exit 1
    fi
    
    for child in $children_pids; do
      /bin/kill -9 $child >/dev/null 2>&1
    done
    if [ -r "$SERVICE_PIDFILE" ]; then
      rm $SERVICE_PIDFILE
    fi
    
    log_end_msg 0
  elif [ $status_stop -eq 1 ]; then
    log_action_msg "$SERVICE_DESC is not running but the pid file exists, cleaning up"
    rm -f $SERVICE_PIDFILE
  elif [ $status_stop -eq 3 ]; then
    log_action_msg "$SERVICE_DESC is not running"
  fi
 ;;
 restart)
  check_status
  status_restart=$?
  if [ $status_restart -eq 0 ]; then
    $0 stop
  fi
  $0 start
 ;;
 reload|force-reload)
  check_status
  status_reload=$?
  if [ $status_reload -eq 0 ]; then
    log_daemon_msg "Reloading $SERVICE_DESC config" "$SERVICE_NAME"

    $0 restart

    if [ $? -eq 0 ]; then
      log_end_msg 0
    else
      log_end_msg 1
    fi
  else
    log_failure_msg "$SERVICE_DESC is not running"
  fi
 ;;
 status)
  check_status
  status=$?
  if [ $status -eq 0 ]; then
    read pid < $SERVICE_PIDFILE
    log_action_msg "$SERVICE_DESC is running with pid $pid"
    exit 0
  elif [ $status -eq 1 ]; then
    log_action_msg "$SERVICE_DESC is not running and the pid file exists"
    exit 1
  elif [ $status -eq 3 ]; then
    log_action_msg "$SERVICE_DESC is not running"
    exit 3
  else
    log_action_msg "Unable to determine $SERVICE_NAME status"
    exit 4
  fi
 ;;
 *)
 log_action_msg "Usage: $0 {start|stop|restart|reload|force-reload|status}"
 exit 2
 ;;
esac

exit 0
