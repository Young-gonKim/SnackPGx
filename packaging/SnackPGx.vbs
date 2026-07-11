' ============================================================================
'  SnackPGx launcher (recommended — no console window)
'
'  Runs the bundled, code-signed javaw.exe directly instead of the unsigned
'  jpackage launcher stub (SnackPGx.exe). Korean endpoint AV products such as
'  AhnLab V3 intermittently terminate the unsigned stub on startup; the signed
'  javaw is trusted and starts reliably. Self-locating, so the whole folder can
'  be unzipped and moved anywhere. Window mode 0 = hidden (no console flash).
' ============================================================================
Set fso = CreateObject("Scripting.FileSystemObject")
base  = fso.GetParentFolderName(WScript.ScriptFullName)
javaw = base & "\runtime\bin\javaw.exe"
app   = base & "\app"
cmd = """" & javaw & """ -Dapp.dir=""" & app & """ -cp """ & app & "\*"" snackpgx.Launcher"
CreateObject("WScript.Shell").Run cmd, 0, False
