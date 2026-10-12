#define AppName "Digital Lesson Planner"
#define AppVersion "1.0.0"
#define AppPayloadDir "..\build\installer\payload"

[Setup]
AppId={{7B5730E1-8D24-4D2B-BF02-1D4D669E3E74}
AppName={#AppName}
AppVersion={#AppVersion}
AppPublisher=Digital Lesson Planner
DefaultDirName={localappdata}\Programs\Digital Lesson Planner
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputBaseFilename=DigitalLessonPlanner-Setup
ArchitecturesInstallIn64BitMode=x64compatible
ArchitecturesAllowed=x64compatible
Compression=lzma2
SolidCompression=yes
WizardStyle=modern

[Files]
Source: "{#AppPayloadDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#AppName}"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\LaunchDLP.vbs"""; WorkingDir: "{app}"
Name: "{autodesktop}\{#AppName}"; Filename: "{sys}\wscript.exe"; Parameters: """{app}\LaunchDLP.vbs"""; WorkingDir: "{app}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"

[Dirs]
Name: "{localappdata}\DLP"

[Run]
Filename: "{sys}\wscript.exe"; Parameters: """{app}\LaunchDLP.vbs"""; Description: "Launch {#AppName}"; Flags: postinstall nowait skipifsilent
