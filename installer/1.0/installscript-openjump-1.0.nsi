
; OpenJUMP script for Nullsoft Installer
; Original version: Robert Chou
; Modified by Steve Tanner 

  ;Compression options
  CRCCheck on
  SetCompress force
  SetCompressor lzma
  SetDatablockOptimize on

Name "OpenJump"
# Defines
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION "1.0"
!define COMPANY "OpenJump"
!define URL http://www.openjump.org/

# MUI defines
!define MUI_ICON "OpenJump.ico"
!define MUI_UNICON "OpenJump.ico"
  
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP header.bmp
!define MUI_WELCOMEFINISHPAGE_BITMAP side_left.bmp 
!define MUI_FINISHPAGE_SHOWREADME_TEXT "Show release notes"
!define MUI_FINISHPAGE_SHOWREADME "$INSTDIR\OpenJump-1.0-ReleaseNotes.txt"
!define MUI_FINISHPAGE_RUN_TEXT "Show start menu group"
!define MUI_FINISHPAGE_RUN "explorer"
!define MUI_FINISHPAGE_RUN_PARAMETERS "$SMPROGRAMS\OpenJump ${VERSION}"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_FINISHPAGE_NOREBOOTSUPPORT
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKCU
!define MUI_STARTMENUPAGE_NODISABLE
!define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\OpenJump 1.0"
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULT_FOLDER "OpenJump"
!define MUI_LANGDLL_REGISTRY_ROOT HKLM
!define MUI_LANGDLL_REGISTRY_KEY ${REGKEY}
!define MUI_LANGDLL_REGISTRY_VALUENAME InstallerLanguage
!define MUI_ABORTWARNING

!define TEMP1 $R0
!define TEMP2 $R1
  
# Included files
!include "Sections.nsh"
!include "MUI.nsh"
!include "StrFunc.nsh"

# Reserved Files
!insertmacro MUI_RESERVEFILE_LANGDLL
!insertmacro MUI_RESERVEFILE_INSTALLOPTIONS
  ReserveFile "jvm.ini"
  ReserveFile "config.ini"
  
  
  ;Product information
  VIAddVersionKey ProductName "OpenJump"
  VIAddVersionKey CompanyName "OpenJump"
  VIAddVersionKey LegalCopyright "Copyright (c) 2006 OpenJump"
  VIAddVersionKey FileDescription "OpenJump Installer"
  VIAddVersionKey FileVersion ${VERSION}
  VIAddVersionKey ProductVersion ${VERSION}
  VIAddVersionKey Comments "Robert Chou, Steve Tanner"
  VIAddVersionKey InternalName "OpenJump"
  VIProductVersion "${VERSION}.0.1"


${StrRep}
  Var "JavaHome"


  ;General
  OutFile "..\distrib\OpenJump-${VERSION}-win32installer.exe"

  ;Install Options pages
  LangString TEXT_JVM_TITLE ${LANG_ENGLISH} "Java Virtual Machine"
  LangString TEXT_JVM_SUBTITLE ${LANG_ENGLISH} "Java Virtual Machine path selection."
  LangString TEXT_JVM_PAGETITLE ${LANG_ENGLISH} ": Java Virtual Machine path selection"

  LangString TEXT_CONF_TITLE ${LANG_ENGLISH} "Configuration"
  LangString TEXT_CONF_SUBTITLE ${LANG_ENGLISH} "OpenJump basic configuration."
  LangString TEXT_CONF_PAGETITLE ${LANG_ENGLISH} ": Configuration Options"

  ;Install Page order
  !insertmacro MUI_PAGE_WELCOME
  !insertmacro MUI_PAGE_LICENSE ..\etc\license.txt
  !insertmacro MUI_PAGE_COMPONENTS
  !insertmacro MUI_PAGE_DIRECTORY
  ;Page custom SetConfiguration Void "$(TEXT_CONF_PAGETITLE)"
  Page custom SetChooseJVM CheckChooseJVM "$(TEXT_JVM_PAGETITLE)"
  !insertmacro MUI_PAGE_INSTFILES
  !insertmacro MUI_PAGE_FINISH

  ;Uninstall Page order
  !insertmacro MUI_UNPAGE_CONFIRM
  !insertmacro MUI_UNPAGE_INSTFILES

  ;License dialog
  LicenseData ..\etc\license.txt

  ;Component-selection page
    ;Descriptions
;    LangString DESC_SecOpenJump ${LANG_ENGLISH} "Install J2SE 5.0 and OpenJump."
    LangString DESC_SecOpenJumpCore ${LANG_ENGLISH} "Install OpenJump using an existing J2SE 5.0 installation."
;    LangString DESC_SecOpenJumpSource ${LANG_ENGLISH} "Install the OpenJump source code."
    LangString DESC_SecMenu ${LANG_ENGLISH} "Create a Start Menu program group for OpenJump."
    LangString DESC_SecExamples ${LANG_ENGLISH} "Installs some sample data files."

  ;Language
  !insertmacro MUI_LANGUAGE English

  ;Folder-select dialog
  InstallDir "$PROGRAMFILES\OpenJump"

  ;Install types
  InstType Normal
  InstType Minimum
  InstType Full




;--------------------------------
;Installer Sections

SubSection "OpenJump" SecOpenJump

Section "Core" SecOpenJumpCore

  SectionIn 1 2 3 RO

  ;LogSet on

  IfSilent +2 0
  Call checkJvm

  CreateDirectory "$INSTDIR\logs"
  SetOutPath $INSTDIR
  File ..\etc\license.txt
  File ..\etc\apache.txt
  File release_notes.txt
  File /r ..\lib
  SetOutPath $INSTDIR\bin
  File OpenJump.exe
  File ..\scripts\JUMPWorkbench.bat
  File ..\etc\log4j.xml
  SetOutPath $INSTDIR\conf

  ; find path to java.exe
  IfSilent 0 +3
  Call findJavaPath
  Pop $2

  IfSilent +2 0
  !insertmacro MUI_INSTALLOPTIONS_READ $2 "jvm.ini" "Field 2" "State"

  StrCpy "$JavaHome" $2
  Call findJVMPath
  Pop $2

  DetailPrint "Using Jvm: $2"

  ; configure setup
  Call configure

  ClearErrors

SectionEnd

;Section "Source Code" SecOpenJumpSource
;
;  SectionIn 3
;  SetOutPath $INSTDIR
;  File /r src
;
;SectionEnd

SubSectionEnd

Section "Start Menu Items" SecMenu

  SectionIn 1 2 3

  !insertmacro MUI_INSTALLOPTIONS_READ $2 "jvm.ini" "Field 2" "State"

  SetOutPath "$SMPROGRAMS\OpenJump ${VERSION}"
  CreateDirectory "$SMPROGRAMS\OpenJump ${VERSION}"

  CreateShortCut "$SMPROGRAMS\OpenJump ${VERSION}\license.lnk" \
                 "$INSTDIR\license.txt"

  CreateShortCut "$SMPROGRAMS\OpenJump ${VERSION}\release_notes.lnk" \
                 "$INSTDIR\release_notes.txt"

/* causes start menu folder to not be deleted */
  CreateShortCut "$SMPROGRAMS\OpenJump ${VERSION}\Uninstall.lnk" \
                 "$INSTDIR\Uninstall.exe"

  SetOutPath "$INSTDIR\bin"

  CreateShortCut "$SMPROGRAMS\OpenJump ${VERSION}\OpenJump.lnk" \
                 "$INSTDIR\bin\OpenJump.exe" \
                 ""

  IfErrors 0 NoErrors
    MessageBox MB_OK "Could not create one or more shortcuts."
  ClearErrors

  NoErrors:

SectionEnd

Section "Sample Data Files" SecExamples

  SectionIn 1 3

  SetOverwrite on
  SetOutPath $INSTDIR
  ;File /r ..\data_files

SectionEnd

Section -post

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr HKLM "SOFTWARE\OPENJUMP\OpenJump\${VERSION}" "InstallPath" $INSTDIR
  WriteRegStr HKLM "SOFTWARE\OPENJUMP\OpenJump\${VERSION} "Version" "${VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OpenJump ${VERSION}" \
                   "DisplayName" "OpenJump ${VERSION} (remove only)"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OpenJump ${VERSION}" \
                   "UninstallString" '"$INSTDIR\Uninstall.exe"'

SectionEnd

Function .onInit

  ;Extract Install Options INI Files
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT "config.ini"
  !insertmacro MUI_INSTALLOPTIONS_EXTRACT "jvm.ini"

FunctionEnd

Function SetChooseJVM
  !insertmacro MUI_HEADER_TEXT "$(TEXT_JVM_TITLE)" "$(TEXT_JVM_SUBTITLE)"
  Call findJavaPath
  Pop $3
  !insertmacro MUI_INSTALLOPTIONS_WRITE "jvm.ini" "Field 2" "State" $3
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY "jvm.ini"
FunctionEnd

Function CheckChooseJVM
  ; check java version
  !insertmacro MUI_INSTALLOPTIONS_READ $3 "jvm.ini" "Field 2" "State"
  IfFileExists "$3\bin\java.exe" "" JavaError
  nsExec::ExecToStack '"$3\bin\java.exe" -version:1.5'
  Pop $0
  Pop $0
  StrCpy $1 $0 5
  StrCmp $1 "Usage" JavaOk

  JavaError:
    MessageBox MB_OK|MB_ICONSTOP \
      "Please choose a Java 1.5 (J2SE 5.0) or newer installation."
  Abort

  JavaOk:
FunctionEnd

Function SetConfiguration
  !insertmacro MUI_HEADER_TEXT "$(TEXT_CONF_TITLE)" "$(TEXT_CONF_SUBTITLE)"
  !insertmacro MUI_INSTALLOPTIONS_DISPLAY "config.ini"
FunctionEnd

Function Void
FunctionEnd

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
;  !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenJump} $(DESC_SecOpenJump)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenJumpCore} $(DESC_SecOpenJumpCore)
;  !insertmacro MUI_DESCRIPTION_TEXT ${SecOpenJumpSource} $(DESC_SecOpenJumpSource)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecMenu} $(DESC_SecMenu)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecExamples} $(DESC_SecExamples)
!insertmacro MUI_FUNCTION_DESCRIPTION_END


; =====================
; FindJavaPath Function
; =====================
;
; Find the JAVA_HOME used on the system, and put the result on the top of the
; stack
; Will return an empty string if the path cannot be determined
;
Function findJavaPath

  ;ClearErrors

  ;ReadEnvStr $1 JAVA_HOME

  ;IfErrors 0 FoundJDK

  ClearErrors

  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "JavaHome"
  ReadRegStr $3 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$2" "RuntimeLib"

  ; if version is < 1.5, then set error causing an empty string to be returned
  StrCpy $R9 $2 "" 2
  IntCmp $R9 5 VersionOk lessthan VersionOk
VersionOk:
  Goto NoErrors

lessthan:
  SetErrors

  ;FoundJDK:

  IfErrors 0 NoErrors
  StrCpy $1 ""

NoErrors:

  ClearErrors

  ; Put the result in the stack
  Push $1

FunctionEnd


; ====================
; FindJVMPath Function
; ====================
;
; Find the full JVM path, and put the result on top of the stack
; Argument: JVM base path (result of findJavaPath)
; Will return an empty string if the path cannot be determined
;
Function findJVMPath

  ClearErrors
  
  ;Step one: Is this a JRE path (Program Files\Java\XXX)
  StrCpy $1 "$JavaHome"
  
  StrCpy $2 "$1\bin\hotspot\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\server\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\client\jvm.dll"  
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\classic\jvm.dll"
  IfFileExists "$2" FoundJvmDll

  ;Step two: Is this a JDK path (Program Files\XXX\jre)
  StrCpy $1 "$JavaHome\jre"
  
  StrCpy $2 "$1\bin\hotspot\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\server\jvm.dll"
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\client\jvm.dll"  
  IfFileExists "$2" FoundJvmDll
  StrCpy $2 "$1\bin\classic\jvm.dll"
  IfFileExists "$2" FoundJvmDll

  ClearErrors
  ;Step tree: Read defaults from registry
  
  ReadRegStr $1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$1" "RuntimeLib"
  
  IfErrors 0 FoundJvmDll
  StrCpy $2 ""

  FoundJvmDll:
  ClearErrors

  ; Put the result in the stack
  Push $2

FunctionEnd


; ====================
; CheckJvm Function
; ====================
;
Function checkJvm

  !insertmacro MUI_INSTALLOPTIONS_READ $3 "jvm.ini" "Field 2" "State"
  IfFileExists "$3\bin\java.exe" NoErrors1
  MessageBox MB_OK|MB_ICONSTOP "No Java Virtual Machine found in folder:$\r$\n$3"
  Quit
NoErrors1:
  StrCpy "$JavaHome" $3
  Call findJVMPath
  Pop $4
  StrCmp $4 "" 0 NoErrors2
  MessageBox MB_OK|MB_ICONSTOP "No Java Virtual Machine found in folder:$\r$\n$3"
  Quit
NoErrors2:

FunctionEnd

; ==================
; Configure Function
; ==================
;
; Display the configuration dialog boxes, read the values entered by the user,
; and build the configuration files
;
Function configure

/*
  !insertmacro MUI_INSTALLOPTIONS_READ $R0 "config.ini" "Field 2" "State"
  !insertmacro MUI_INSTALLOPTIONS_READ $R1 "config.ini" "Field 5" "State"
  !insertmacro MUI_INSTALLOPTIONS_READ $R2 "config.ini" "Field 7" "State"

  IfSilent 0 +2
  StrCpy $R4 'port="8080"'

  IfSilent +2 0
  StrCpy $R4 'port="$R0"'

  IfSilent 0 +2
  StrCpy $R5 ''

  IfSilent Silent 0

  ; Escape XML
  Push $R1
  Call xmlEscape
  Pop $R1
  Push $R2
  Call xmlEscape
  Pop $R2
  
  StrCpy $R5 '<user name="$R1" password="$R2" roles="admin,manager" />'

Silent:
  DetailPrint 'HTTP/1.1 Connector configured on port "$R0"'
  DetailPrint 'Admin user added: "$R1"'

  SetOutPath $TEMP
  File /r confinstall

  ; Build final server.xml
  Delete "$INSTDIR\conf\server.xml"
  FileOpen $R9 "$INSTDIR\conf\server.xml" w

  Push "$TEMP\confinstall\server_1.xml"
  Call copyFile
  FileWrite $R9 $R4
  Push "$TEMP\confinstall\server_2.xml"
  Call copyFile

  FileClose $R9

  DetailPrint "server.xml written"

  ; Build final OpenJump-users.xml
  
  Delete "$INSTDIR\conf\OpenJump-users.xml"
  FileOpen $R9 "$INSTDIR\conf\OpenJump-users.xml" w

  Push "$TEMP\confinstall\OpenJump-users_1.xml"
  Call copyFile
  FileWrite $R9 $R5
  Push "$TEMP\confinstall\OpenJump-users_2.xml"
  Call copyFile

  FileClose $R9

  DetailPrint "OpenJump-users.xml written"

  RMDir /r "$TEMP\confinstall"
*/

FunctionEnd


Function xmlEscape
  Pop $0
  ${StrRep} $0 $0 "&" "&amp;"
  ${StrRep} $0 $0 "$\"" "&quot;"
  ${StrRep} $0 $0 "<" "&lt;"
  ${StrRep} $0 $0 ">" "&gt;"
  Push $0
FunctionEnd


; =================
; CopyFile Function
; =================
;
; Copy specified file contents to $R9
;
Function copyFile

  ClearErrors

  Pop $0

  FileOpen $1 $0 r

 NoError:

  FileRead $1 $2
  IfErrors EOF 0
  FileWrite $R9 $2

  IfErrors 0 NoError

 EOF:

  FileClose $1

  ClearErrors

FunctionEnd


;--------------------------------
;Uninstaller Section

Section Uninstall

  Delete "$INSTDIR\modern.exe"
  Delete "$INSTDIR\Uninstall.exe"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\OpenJump ${VERSION}"
  RMDir /r "$SMPROGRAMS\OpenJump ${VERSION}"
  RMDir "$SMPROGRAMS\OpenJump ${VERSION}"
  Delete "$INSTDIR\license.txt"
  Delete "$INSTDIR\release_notes.txt"
  RMDir /r "$INSTDIR\bin"
  RMDir /r "$INSTDIR\conf"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\logs"
  RMDir /r "$INSTDIR\data_files"
  RMDir "$INSTDIR"

  IfSilent Removed 0

  ; if $INSTDIR was removed, skip these next ones
  IfFileExists "$INSTDIR" 0 Removed 
    MessageBox MB_YESNO|MB_ICONQUESTION \
      "Remove all files in your OpenJump ${VERSION} directory? (If you have anything  \
 you created that you want to keep, click No)" IDNO Removed
    RMDir /r "$INSTDIR"
    RMDir "$INSTDIR"
    Sleep 500
    IfFileExists "$INSTDIR" 0 Removed 
      MessageBox MB_OK|MB_ICONEXCLAMATION \
                 "Note: $INSTDIR could not be removed."
  Removed:

SectionEnd

;eof
