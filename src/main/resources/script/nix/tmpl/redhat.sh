#!/bin/sh
#
# #SERVICE_NAME# control script
#
# chkconfig: - 80 20
# description: #SERVICE_NAME# startup script
# processname: #SERVICE_NAME#
# pidfile: #PID_FILE#
# config: /etc/default/#SERVICE_NAME#.conf
#

SERVICE_NAME=#SERVICE_NAME#
SERVICE_DESC="#SERVICE_DESC#"
CTLSCRIPT="#CTLSCRIPT#"
START_ARG="#START_ARG#"
STOP_ARG="#STOP_ARG#"
PID_FILE="#PID_FILE#"
LOG_FILE_OUT="#LOG_FILE_OUT#"
LOG_FILE_ERR="#LOG_FILE_ERR#"
GREP_SUCCESS_FILE="#GREP_SUCCESS_FILE#"
GREP_SUCCESS_STRING="#GREP_SUCCESS_STRING#"

# Source function library.
. /etc/init.d/functions

# Load init.d configuration.
if [ -z "$SERVICE_CONF" ]; then
	SERVICE_CONF="/etc/default/#SERVICE_NAME#.conf"
fi

[ -r "$SERVICE_CONF" ] && . "${SERVICE_CONF}"

# Set defaults.

if [ -z "$SERVICE_USER" ]; then
  SERVICE_USER=#SERVICE_USER#
fi

# Check #SERVICE_NAME# user
id $SERVICE_USER > /dev/null 2>&1
if [ $? -ne 0 -o -z "$SERVICE_USER" ]; then
  echo -n "User \"$SERVICE_USER\" does not exist..."
  exit 1
fi

if [ -z "$SERVICE_PIDFILE" ]; then
	SERVICE_PIDFILE=$PID_FILE
fi
export SERVICE_PIDFILE

if [ -z "$SERVICE_LOG" ]; then
  if [ ! -z "$LOG_FILE_OUT" ]; then
    SERVICE_LOG=$LOG_FILE_OUT
  else
  	SERVICE_LOG=/var/log/#SERVICE_NAME#/console.log
  fi
fi
export SERVICE_LOG

if [ -z "$STARTUP_WAIT" ]; then
	STARTUP_WAIT=30
fi

if [ -z "$SHUTDOWN_WAIT" ]; then
	SHUTDOWN_WAIT=30
fi

if [ -z "$SERVICE_LOCKFILE" ]; then
	SERVICE_LOCKFILE=/var/lock/subsys/#SERVICE_NAME#
fi


prog='#SERVICE_NAME#'

start() {
	echo -n "Starting $prog: "
	if [ -f $SERVICE_PIDFILE ]; then
		read ppid < $SERVICE_PIDFILE
		if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
			echo -n "$prog is already running"
			failure
  	  echo
  		return 1
  	else
  		rm -f $SERVICE_PIDFILE
  	fi
	fi
	mkdir -p $(dirname $SERVICE_LOG)
	cat /dev/null > $SERVICE_LOG

	mkdir -p $(dirname $SERVICE_PIDFILE)
	chown $SERVICE_USER $(dirname $SERVICE_PIDFILE) || true

	if [ ! -z "$SERVICE_USER" ]; then
    su -s /bin/bash $SERVICE_USER -c "$CTLSCRIPT $START_ARG" >> $SERVICE_LOG 2>&1 &
    sleep 2 # fail right away if command fails
    SERVICE_PID=`cat $SERVICE_PIDFILE`
    kill -0 $SERVICE_PID > /dev/null 2>&1
    if [ $? -ne 0 ]; then
      failure
      echo
      return 1
    fi
 	fi

  launched=0
	count=0

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
		echo -n "."
		sleep 1
		let count=$count+1;
	done

  if [ $launched -eq 0 ]; then
    echo "$SERVICE_DESC hasn't started within the timeout allowed"
    echo "please review file \"$SERVICE_LOG\" to see the status of the service"
    failure
    echo
    return 1
  fi

	touch $SERVICE_LOCKFILE
	success
	echo
	return 0
}

stop() {
	echo -n $"Stopping $prog: "
	count=0;

  if [ ! -z "$SERVICE_USER" ]; then
    su -s /bin/bash $SERVICE_USER -c "$CTLSCRIPT $STOP_ARG" >> $SERVICE_LOG 2>&1 &
  fi

	if [ -f $SERVICE_PIDFILE ]; then
		read kpid < $SERVICE_PIDFILE
		let kwait=$SHUTDOWN_WAIT
		

		# Try issuing SIGTERM
		kill -15 $kpid
		until [ `ps --pid $kpid 2> /dev/null | grep -c $kpid 2> /dev/null` -eq '0' ] || [ $count -gt $kwait ]
			do
			sleep 1
			let count=$count+1;
		done

		if [ $count -gt $kwait ]; then
			kill -9 $kpid
		fi
	fi
	rm -f $SERVICE_PIDFILE
	rm -f $SERVICE_LOCKFILE
	success
	echo
}

status() {
	if [ -f $SERVICE_PIDFILE ]; then
		read ppid < $SERVICE_PIDFILE
		if [ `ps --pid $ppid 2> /dev/null | grep -c $ppid 2> /dev/null` -eq '1' ]; then
			echo "$prog is running (pid $ppid)"
			return 0
		else
			echo "$prog dead but pid file exists"
			return 1
		fi
	fi
	echo "$prog is not running"
	return 3
}

case "$1" in
	start)
		start
		;;
	stop)
		stop
		;;
	restart)
		$0 stop
		$0 start
		;;
	status)
		status
		;;
	*)
		## If no parameters are given, print which are avaiable.
		echo "Usage: $0 {start|stop|status|restart|reload}"
		exit 1
		;;
esac
