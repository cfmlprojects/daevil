@param  daevil.menu.BatchFileBuilder builder
@param String baseUrl
@param String prompt
set JRE_ZIP=%DIR%jre.zip
set JRE_DIR=%DIR%jre
if "%OS%"=="Windows_NT" @@setlocal
if "%OS%"=="WINNT" @@setlocal

set javacmd=%JAVACMD%
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%javacmd%" == "" set javacmd=%JAVA_HOME%\bin\java.exe
goto hasjava
:noJavaHome
if exist "%JRE_DIR%\bin\java.exe" (
  set JAVA_HOME=%JRE_DIR%
  set javacmd=%JRE_DIR%\bin\java.exe
  goto hasjava
)

if "%javacmd%" == "" (
  for %%X in (java.exe) do (set FOUND=%%~$PATH:X)
  if not defined FOUND goto nojava
)

:noJava
set downloadJRE=
set /p downloadJRE=${prompt}
if /I '%downloadJRE%'=='n' goto hasjava
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set BITTYPE=32 || set BITTYPE=64
if %BITTYPE%==32 echo This is a 32bit operating system
if %BITTYPE%==64 echo This is a 64bit operating system
mkdir "%JRE_DIR%"
set JRE_URL="${baseUrl}%BITTYPE%.zip"
${builder.call("md5url_unzip","%JRE_URL%","%JRE_ZIP%", "%JRE_DIR%")}
if %ERRORLEVEL% GEQ 1 pause
if %ERRORLEVEL% EQU 0 del %JRE_ZIP%
set JAVA_HOME=%JRE_DIR%
set javacmd=%JRE_DIR%\bin\java.exe

:hasjava
