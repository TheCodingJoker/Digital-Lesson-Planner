Option Explicit

Dim shell, fileSystem, appDir, dataDir, command
Set shell = CreateObject("WScript.Shell")
Set fileSystem = CreateObject("Scripting.FileSystemObject")

appDir = fileSystem.GetParentFolderName(WScript.ScriptFullName)
dataDir = shell.ExpandEnvironmentStrings("%LOCALAPPDATA%") & "\DLP"
If Not fileSystem.FolderExists(dataDir) Then
    fileSystem.CreateFolder(dataDir)
End If

shell.CurrentDirectory = dataDir
command = """" & appDir & "\runtime\bin\javaw.exe"" --module-path """ & appDir & "\javafx\lib"" --add-modules javafx.controls,javafx.fxml -Djava.library.path=""" & appDir & "\javafx\bin"" -cp """ & appDir & "\DLP.jar;" & appDir & "\lib\*"" MainApp"
shell.Run command, 1, False
