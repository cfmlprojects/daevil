#!/bin/bash
#title           :@name@-remove.sh
#description     :The script to remove @name@ daemon
#usage           :/bin/bash @name@-remove.sh [user]
 
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CTLSCRIPT_DIR="$(cd $SCRIPT_DIR/../../; pwd)"

SERVICE_DIR=$CTLSCRIPT_DIR
SERVICE_NAME=@name@
SERVICE_DESC="@title@"
CTLSCRIPT="$SERVICE_DIR/@ctl-script@"
START_ARG="@arg-start@"
STOP_ARG="@arg-stop@"
PID_FILE="@pid-file@"
GREP_SUCCESS_FILE="@grep-success-file@"
GREP_SUCCESS_STRING="@grep-success-string@"

echo "Unregistrating $SERVICE_NAME as service..."
if [ "$(uname)" == "Darwin" ]; then
    # Do something under Mac OS X platform 
    echo "Darwin distribution"
    if [ -w /Library/LaunchDaemons ] ; then
      echo "Removing from system"
      PLIST_FILE="/Library/LaunchDaemons/$SERVICE_NAME.plist"
    else
      echo "Removing for current user only (cannot write to /Library/LaunchDaemons)"
      PLIST_FILE="$HOME/Library/LaunchAgents/$SERVICE_NAME.plist"
    fi
    if [ -r $PLIST_FILE ]; then
      echo "Unloading existing service"
      /bin/launchctl unload -w $PLIST_FILE
    fi
    rm $PLIST_FILE
else
    if [[ $EUID -ne 0 ]]; then
       echo "This script must be run as root."
       exit 1
    fi
    if [ -r /lib/lsb/init-functions ]; then
        DISABLE_SERVICE_CMD="update-rc.d -f $SERVICE_NAME remove"
    elif [ -r /etc/init.d/functions ]; then
        DISABLE_SERVICE_CMD="chkconfig --del $SERVICE_NAME"
    else
        DISABLE_SERVICE_CMD="chkconfig --del $SERVICE_NAME"
    fi

    service $SERVICE_NAME stop
    rm -rf "/var/run/$SERVICE_NAME/"
    $DISABLE_SERVICE_CMD
    rm -f "/etc/init.d/$SERVICE_NAME"
fi 

# REMOVE USER IF ADDED
if [ -z "$1" ]; then
  SERVICE_USER="$USER"
else
  SERVICE_USER="$1"
  if [[ "$SERVICE_USER" == "$USER" ]]; then
     echo "You cannot remove yourself!"
     exit 1
  fi
  id $SERVICE_USER > /dev/null 2>&1
  if [ $? -ne 0 -o -z "$SERVICE_USER" ]; then
       echo "No user \"$SERVICE_USER\" found"
       exit 1
  else
    if [[ $EUID -ne 0 ]]; then
       echo "To remove a user you must run this script as root"
       exit 1
    fi
    if [ "$(uname)" == "Darwin" ]; then
      echo "Removing user \"$SERVICE_USER\""
      dscl . delete /Groups/$SERVICE_USER > /dev/null 2>&1
      dscl . delete /Users/$SERVICE_USER
    else
      userdel $SERVICE_USER
    fi
    id $SERVICE_USER > /dev/null 2>&1
    if [ $? -ne 0 -o -z "$SERVICE_USER" ]; then
      echo "Removed user $SERVICE_USER"
    else
      echo "Could not remove $SERVICE_USER"
      exit 1
    fi
  fi
fi

echo "Done."