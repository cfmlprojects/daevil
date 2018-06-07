#!/bin/bash
#title           :@name@-install.sh
#description     :The script to install @name@
#usage           :/bin/bash @name@-install.sh [user]
 
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CTLSCRIPT_DIR="$(cd $SCRIPT_DIR/../../; pwd)"

SERVICE_DIR=$CTLSCRIPT_DIR
SERVICE_NAME=@name@
SERVICE_DESC="@title@"
CTLSCRIPT="$SERVICE_DIR/@ctl-script@"
START_ARG="@arg-start@"
START_ARG_DARWIN="@arg-start-darwin@"
STOP_ARG="@arg-stop@"
PID_FILE="@pid-file@"
GREP_SUCCESS_FILE="@grep-success-file@"
GREP_SUCCESS_STRING="@grep-success-string@"
LOG_FILE_OUT="@log-file-out@"
LOG_FILE_ERR="@log-file-err@"

function replaceTokens {
#    echo "Replacing tokens in $1"
    sed -i -e "s,#SERVICE_DIR#,$SERVICE_DIR,g" $1
    sed -i -e "s,#SERVICE_NAME#,$SERVICE_NAME,g" $1
    sed -i -e "s,#SERVICE_DESC#,$SERVICE_DESC,g" $1
    sed -i -e "s,#SERVICE_USER#,$SERVICE_USER,g" $1
    sed -i -e "s,#CTLSCRIPT#,$CTLSCRIPT,g" $1
    sed -i -e "s,#PID_FILE#,$PID_FILE,g" $1
    sed -i -e "s,#START_ARG#,$START_ARG,g" $1
    sed -i -e "s,#START_ARG_DARWIN#,$START_ARG_DARWIN,g" $1
    sed -i -e "s,#STOP_ARG#,$STOP_ARG,g" $1
    sed -i -e "s,#GREP_SUCCESS_FILE#,$GREP_SUCCESS_FILE,g" $1
    sed -i -e "s,#GREP_SUCCESS_STRING#,$GREP_SUCCESS_STRING,g" $1
    sed -i -e "s,#LOG_FILE_OUT#,$LOG_FILE_OUT,g" $1
    sed -i -e "s,#LOG_FILE_ERR#,$LOG_FILE_ERR,g" $1
}

if [ -z "$1" ]; then
  SERVICE_USER="$USER"
else
  SERVICE_USER="$1"
  id $SERVICE_USER > /dev/null 2>&1
  if [ $? -ne 0 -o -z "$SERVICE_USER" ]; then
    if [[ $EUID -ne 0 ]]; then
       echo "To add a user you must run this script as root"
       exit 1
    fi
    if [ "$(uname)" == "Darwin" ]; then
      echo "User \"$SERVICE_USER\" does not exist, trying to create"
      SERVICE_USER_GID=`dscl . -readall /Users PrimaryGroupID | awk '/PrimaryGroupID/{print $2}' | sort -n | egrep -v "\b[5-9][0-9]{2,5}\b" | tail -n 1`
      SERVICE_USER_UID=`dscl . -readall /Users UniqueID | awk '/UniqueID/{print $2}' | sort -n | egrep -v "\b[5-9][0-9]{2,5}\b" | tail -n 1`
      let "SERVICE_USER_GID=$SERVICE_USER_GID+1"
      let "SERVICE_USER_UID=$SERVICE_USER_UID+1"
      dscl . -create /Users/$SERVICE_USER
      dscl . -create /Users/$SERVICE_USER RealName "$SERVICE_USER"
      dscl . -create /Users/$SERVICE_USER NFSHomeDirectory "$SERVICE_DIR"
      dscl . -create /Users/$SERVICE_USER PrimaryGroupID $SERVICE_USER_GID
      dscl . -create /Users/$SERVICE_USER UniqueID $SERVICE_USER_UID
      dscl . -create /Users/$SERVICE_USER UserShell /usr/bin/false
      dscl . -passwd /Users/$SERVICE_USER '*'
    else
      if [ -r /lib/lsb/init-functions ]; then
        # debianish
        adduser --system --no-create-home --home $SERVICE_DIR --disabled-login $SERVICE_USER
      else
        adduser --system --no-create-home --home $SERVICE_DIR --shell /sbin/nologin $SERVICE_USER
      fi
    fi
    if [ $? -ne 0 -o -z "$SERVICE_USER" ]; then
      echo "could not add user $SERVICE_USER"
      exit 1
    else
      echo "created user $SERVICE_USER"
    fi
  fi
fi
chown -R $SERVICE_USER $SERVICE_DIR

echo "Registrating $SERVICE_NAME as service..."
if [ "$(uname)" == "Darwin" ]; then
    # Do something under Mac OS X platform 
    echo "Darwin distribution"
    if [ -w /Library/LaunchDaemons ] ; then
      echo "Installing for system"
      PLIST_FILE="/Library/LaunchDaemons/$SERVICE_NAME.plist"
    else
      echo "Installing for current user only (cannot write to /Library/LaunchDaemons)"
      PLIST_FILE="$HOME/Library/LaunchAgents/$SERVICE_NAME.plist"
    fi
    if [ -r $PLIST_FILE ]; then
      echo "Unloading existing service"
      /bin/launchctl unload -w $PLIST_FILE
    fi
    cp $SCRIPT_DIR/tmpl/darwin.plist $PLIST_FILE
    replaceTokens $PLIST_FILE
    replaceTokens $PLIST_FILE  #twice in case tokens had tokens
    if [ -w /Library/LaunchDaemons ] ; then
      sed -i -e "s,#SERVICE_USER#,$SERVICE_USER,g" $PLIST_FILE
    else
      sed -i -e "s,<key>UserName</key><string>.*</string>,,g" $PLIST_FILE
    fi
    @additional@ 
    /bin/launchctl load -w $PLIST_FILE
else
    if [[ $EUID -ne 0 ]]; then
       echo "This script must be run as root."
       exit 1
    fi
     
    echo "Cleaning up any previous installs..."
    rm -rf "/var/run/$SERVICE_NAME/"
    rm -f "/etc/init.d/$SERVICE_NAME"

    if [ -r /lib/lsb/init-functions ]; then
        echo "Debian-like distribution"
        cp $SCRIPT_DIR/tmpl/debian.sh /etc/init.d/$SERVICE_NAME
        SERVICE_NAME_CONF=/etc/default/$SERVICE_NAME
        ENABLE_SERVICE_CMD="update-rc.d -f $SERVICE_NAME defaults"
    elif [ -r /etc/init.d/functions ]; then
        echo "RHEL-like distribution"
        cp $SCRIPT_DIR/tmpl/redhat.sh /etc/init.d/$SERVICE_NAME
        SERVICE_NAME_CONF=/etc/default/$SERVICE_NAME.conf
        ENABLE_SERVICE_CMD="chkconfig $SERVICE_NAME on"
    else
        cp $SCRIPT_DIR/tmpl/generic.sh /etc/init.d/$SERVICE_NAME
        SERVICE_NAME_CONF=/etc/default/$SERVICE_NAME.conf
        ENABLE_SERVICE_CMD="chkconfig $SERVICE_NAME on"
    fi
    echo "CTLSCRIPT=$CTLSCRIPT"
    echo "SERVICE_DIR=$SERVICE_DIR"
    echo "SERVICE_USER=$SERVICE_USER"
    replaceTokens /etc/init.d/$SERVICE_NAME
    replaceTokens /etc/init.d/$SERVICE_NAME  #twice in case tokens had tokens
    @additional@
    chmod 755 /etc/init.d/$SERVICE_NAME
    service $SERVICE_NAME start
    $ENABLE_SERVICE_CMD
fi 
echo "Done."