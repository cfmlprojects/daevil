@echo off
setlocal EnableExtensions EnableDelayedExpansion
REM -------------------------------------------------------------------------
REM  @name@ Service Install
REM ==================================================================
REM If more then one service is needed, copy this file to another name
REM and use unique names for SHORTNAME, DISPLAYNAME, DESCRIPTION
REM ==================================================================
set SHORTNAME=@name@
set DISPLAYNAME="@name@"
set DESCRIPTION="@title@"
REM ========================================================

set "DIRNAME=%~dp0%"
set "THISDIR=%~dp0%"
cd > nul
pushd %DIRNAME%..\..
set "RESOLVED_SERVICE_HOME=%CD%"
popd
set DIRNAME=

if "x%SERVICE_HOME%" == "x" (
  set "SERVICE_HOME=%RESOLVED_SERVICE_HOME%" 
)

pushd "%SERVICE_HOME%"
set "SANITIZED_SERVICE_HOME=%CD%"
popd

if "%RESOLVED_SERVICE_HOME%" NEQ "%SANITIZED_SERVICE_HOME%" (
    echo WARNING SERVICE_HOME may be pointing to a different installation - unpredictable results may occur.
    echo %RESOLVED_SERVICE_HOME%
    echo %SANITIZED_SERVICE_HOME%
    echo Should be the same, please check that this script is located in the correct directory
    goto cmdEnd
)

if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
  echo Using the X86-64bit version of prunsrv
  set PRUNSRV="%THISDIR%\amd64\@name@.exe"
) else (
  echo Using the X86-32bit version of prunsrv
  set PRUNSRV="%THISDIR%\@name@.exe"
)
REM echo Service home: %SERVICE_HOME%
if /I "%1" == "install"   goto cmdInstall
if /I "%1" == "uninstall" goto cmdUninstall
if /I "%1" == "start"     goto cmdStart
if /I "%1" == "stop"      goto cmdStop
if /I "%1" == "restart"   goto cmdRestart

:cmdUsage
echo Usage:
echo   service install 
echo   service uninstall
echo   service start
echo   service stop
echo   service restart
goto endBatch

:cmdInstall
shift

:doInstall
set STARTPARAM="/c \"set NOPAUSE=Y ^^^&^^^& \"@ctl-script@\" @arg-start@\""
set STOPPARAM="/c \"set NOPAUSE=Y ^^^&^^^& \"@ctl-script@\" @arg-stop@\""
set LOGLEVEL=Info
set LOGOUT=@log-file-out@
set LOGERR=@log-file-err@

%PRUNSRV% //IS//%SHORTNAME% --DisplayName=%DISPLAYNAME% --Description %DESCRIPTION% --Startup=auto --LogLevel=%LOGLEVEL% --StdOutput="%LOGOUT%" --StdError="%LOGERR%" --StartMode=exe --StartImage=cmd.exe --StartPath="%SERVICE_HOME%" ++StartParams=%STARTPARAM% --StopMode=exe --StopImage=cmd.exe --StopPath="%SERVICE_HOME%"  ++StopParams=%STOPPARAM%
goto cmdEnd

:cmdUninstall
echo stopping %SHORTNAME%
%PRUNSRV% //SS//%SHORTNAME%
if "%errorlevel%" == "0" (
  echo deleting %SHORTNAME%
  %PRUNSRV% //DS//%SHORTNAME%
) else (
  echo Unable to stop the service
)
goto cmdEnd

:cmdStart
%PRUNSRV% //ES//%SHORTNAME%
goto cmdEnd

:cmdStop
%PRUNSRV% //SS//%SHORTNAME%
goto cmdEnd

:cmdRestart
%PRUNSRV% //SS//%SHORTNAME%
if "%errorlevel%" == "0" (
  %PRUNSRV% //ES//%SHORTNAME%
) else (
  echo Unable to stop the service
)
goto cmdEnd

:cmdEnd
REM need to add other error messages (list higher nr first !)
if errorlevel 8 (
  echo ERROR: The service %SHORTNAME% already exists
  goto endBatch
)
if errorlevel 2 (
  echo ERROR: Failed to load service configuration
  goto endBatch
)
if errorlevel 0 (
  echo Success
  goto endBatch
)
echo errorlevel=%errorlevel%

rem nothing below, exit
:endBatch
