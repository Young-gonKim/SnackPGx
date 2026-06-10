@echo off
rem ===========================================================================
rem  SnackPGx launcher (fallback). Use this if Windows Script Host (.vbs) is
rem  disabled by IT policy. Briefly shows a console window, then launches the
rem  bundled signed javaw.exe and exits. Same rationale as SnackPGx.vbs:
rem  avoids the unsigned jpackage stub that AhnLab V3 may terminate.
rem ===========================================================================
start "" "%~dp0runtime\bin\javaw.exe" -Dapp.dir="%~dp0app" -cp "%~dp0app\*" snackpgx.Launcher
