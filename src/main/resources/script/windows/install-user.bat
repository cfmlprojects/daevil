@Echo Off
CD /D %~dp0
Set StartupFolder=%AppData%\Microsoft\Windows\Start Menu\Programs\Startup
If Exist "%StartupFolder%" Goto :FoundStartup
Set StartupFolder=%UserProfile%\Start Menu\Programs\Startup
If Exist "%StartupFolder%" Goto :FoundStartup
Echo Cannot find Startup folder.
Exit /B

:FoundStartup
Copy "start-@{name}.bat" "%StartupFolder%"