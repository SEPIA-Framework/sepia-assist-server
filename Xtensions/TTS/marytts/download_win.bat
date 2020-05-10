@echo off
setlocal enabledelayedexpansion
echo.
SET thispath=%~dp0
SET downloadurl="https://github.com/fquirin/marytts/releases/latest/download/marytts.zip"
echo Downloading MaryTTS server. This might take a few minutes ...
echo.
echo URL: %downloadurl%
echo.
IF EXIST marytts.zip (
	del marytts.zip
)
powershell.exe -command "[Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12; (new-object System.Net.WebClient).DownloadFile('%downloadurl%','marytts.zip')"
if "%errorlevel%" == "1" (
	echo Download failed!
	goto bottom
) else (
	echo Extracting Zip file ...
	cscript //nologo unzip.vbs "%thispath%" "%thispath%marytts.zip"
	SET errorcode=!errorlevel!
	if "!errorcode!" == "1" (
		echo Extraction failed!
		goto bottom
	) else (
		echo Cleaning up ...
		del marytts.zip
		echo Done.
		goto bottom
	)
)
:bottom
echo.
pause
exit