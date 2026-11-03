@echo off

REM store the original image dir into a variable
set INSTALLER_ROOT_DIR=%~dp0\.. 

rem resolve UNC path
pushd %~dp0
set root="%cd%"

rem xsetup.bat is in <root>\bin, get the parent directory (<root>)
set parent=%cd%\..

@setlocal enableextensions enabledelayedexpansion
rem Check for exclamation point
rem Remove exclamation point if any in the parent folder path and check if the folder exists
set parent_fixed=%parent:!=%

IF NOT EXIST "%parent_fixed%" (
  start cmd /c "echo ERROR: The character '^!' is not allowed in path. Please correct the problem and try again. && pause"
  endlocal
  set EXITCODE=1
  goto :end
)
endlocal

%SYSTEMROOT%\System32\net session >nul 2>&1
if NOT %errorLevel% == 0 (
  echo ERROR: Administrative permissions are not available. Please restart the command line shell as Administrator.
  set EXITCODE=1
  goto :end
)
CALL %root%\setup-boot-loader.bat %*
rem set ARGS=%ARGS% --add-modules java.se.ee -DINSTALLER_ROOT_DIR="%INSTALLER_ROOT_DIR%"
set ARGS=%ARGS% -DINSTALLER_ROOT_DIR="%INSTALLER_ROOT_DIR%"

REM if the argument -Uninstall was specified, run the uninstaller
IF NOT [%1]==[] (
  IF [%1]==[-Uninstall] (
    set ARGS=%ARGS% -DINSTALLATION_MODE=Uninstall
  )
) 
set ARGS=%ARGS% -Dlogback.configurationFile="%parent%/data/xic-logback.xml"
REM echo %ARGS%
set freshInstall=1
for /F "tokens=2" %%i in ('date /t') do set mydate=%%i
set logName=%mydate%-%time%
set sep=-
call set logName=%%logName:/=%sep%%%
call set logName=%%logName::=%sep%%%
REM CR-1118593
call set logName=%%logName: =%%%

if exist %parent%\.xinstall\NUL (
  echo INFO: This is not a fresh install.
  set freshInstall=0
  set LOG_FILE=%root%\xinstall.log
) else (
  echo INFO: This is a fresh install.
  set LOG_FILE="%USERPROFILE%\.Xilinx\xinstall\xinstall-%logName%.log"
  if not exist "%USERPROFILE%\.Xilinx\xinstall\" (
    mkdir %USERPROFILE%\.Xilinx\xinstall
  )
)
echo INFO: Log file name is %LOG_FILE%

set ARGS=%ARGS% -DLOG_FILE=%LOG_FILE%

IF NOT "%DEBUG_ARGS%" == "" (
 echo adding %DEBUG_ARGS% to %ARGS%
 set ARGS=%DEBUG_ARGS% %ARGS%
)

set ARGS=%ARGS% -DHAS_DYNAMIC_LANGUAGE_BUNDLE=true

IF [%X_BATCH%] == [1] (
  rem Check if it is a 32-bit platform and exit if 32 libraries are not available.
  IF [%ARCH%] == 32 (
    set libDir=%parent%\lib\win32.o
    IF NOT EXIST "%libDir%" (
      echo ERROR: This installation is not supported for 32 bit platforms
      set EXITCODE=1
      goto :end
    )
  )
  echo Running in batch mode...

  FOR %%A IN (%*) DO (  
    IF /I "%%A"=="-b" (  
        SET nextArg=1  
    ) ELSE IF DEFINED nextArg (  
        IF /I "%%A"=="Uninstall" (   
            echo Please note that uninstallation from this directory will remove all products within it.  
         
        ) ELSE IF /I "%%A"=="Install" (  
            echo If you are installing multiple products in the same directory, please note that during the uninstallation process all products will be removed from this directory.  
        )
        SET nextArg=  
    )  
)  

  %X_JAVA_HOME%\bin\java.exe %ARGS% -cp "%X_CLASS_PATH%;%parent%\lib\classes\commons-cli-1.4.jar" com.xilinx.installer.api.InstallerLauncher %*
) ELSE (
  REM CR:180582 Failed to open xsetup installation window in 2024.1 windows OS
  @setlocal enableextensions enabledelayedexpansion
  REM CR-1169127 Vitis Embedded Windows Installer: adjust font size for 4K resolution screen
  REM JAVA still has not fixed the DPI AWARENESS, the workaround is to update registry entry for
  REM a given application: 
  REM https://learn.microsoft.com/en-us/windows/win32/hidpi/setting-the-default-dpi-awareness-for-a-process
  REM Set the DPIUNAWARE for the installer java. 
  REM Creating registry entry does not work for drives other than c:  
  REM The logic is here to detect the drive and created a link in them TEMP folder and update the
  REM registry entry for that link target. 
  set X_JAVA_EXEC="%X_JAVA_HOME%\bin\java.exe"
  REM CR-1212243 Updater doesn't launch from XIC
  IF "%XIC_CLASS_PATH%" == "" (
    REM @echo java_home: %X_JAVA_HOME%
    for %%A in (%X_JAVA_HOME%) do (
      REM echo file name: %%~nA
      REM echo file extension: %%~xA
      REM echo full file name: %%~nxA
      set drive=%%~dA
      REM echo path: %%~pA
      REM echo drive/path: %%~dpA
      REM @echo full qualified filename: %%~fA
      REM echo %drive%
      set X_JAVA_HOME=%%~fA
      REM @echo ABS java_home: %X_JAVA_HOME%
      set X_JAVA_EXEC=%%~fA\bin\java.exe
    )
  )
  REM @echo %X_JAVA_EXEC%
  REM CR:180582 Failed to open xsetup installation window in 2024.1 windows OS
  IF /I NOT "!drive!" == "c:" (
    REM @echo changing java exec: %X_JAVA_EXEC%
    del !TEMP!\jre >nul 2>&1
    mklink !TEMP!\jre !X_JAVA_HOME! > NUL
    set X_JAVA_EXEC=!TEMP!\jre\bin\java.exe
    REM @echo changed X_JAVA_EXEC  to: !X_JAVA_EXEC!
  )
  REM @echo reg add "HKCU\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers" /v "!X_JAVA_EXEC!" /t REG_SZ /d "~ HIGHDPIAWARE" 
  reg add "HKCU\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers" /v "!X_JAVA_EXEC!" /t REG_SZ /d "~ DPIUNAWARE" /F >nul 2>&1
  @echo INFO: Enabled Java High-DPI display support  
  REM %X_JAVA_HOME%\bin\java.exe %ARGS% -cp "%X_CLASS_PATH%;%parent%\lib\classes\commons-cli-1.2.jar" -splash:"%parent%\data\images\splash.png" -jar "%parent%\lib\classes\xinstaller.jar"
  !X_JAVA_EXEC! !ARGS! -cp "!X_CLASS_PATH!;!parent!\lib\classes\commons-cli-1.4.jar" -splash:"!parent!\data\images\splash.png" com.xilinx.installer.api.InstallerLauncher
  REM @echo reg delete "HKCU\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers"  /V "%X_JAVA_EXEC%" /F
  reg delete "HKCU\Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers"  /V "!X_JAVA_EXEC!" /F >nul 2>&1
  del !TEMP!\jre >nul 2>&1
  endlocal
)
set EXITCODE=%errorlevel%
:end
IF NOT [%TEMP_NATIVE_LIB%] == [] (
  IF EXIST [%TEMP_NATIVE_LIB%] (
    RMDIR /Q /S [%TEMP_NATIVE_LIB%]
  )
)
popd
exit /b %EXITCODE%
