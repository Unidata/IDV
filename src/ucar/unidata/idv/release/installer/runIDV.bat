@echo off

REM ##############################################################################
REM  Script:  runIDV.bat
REM 
REM  Purpose: script to launch the IDV on Windows
REM 
REM  Syntax:  runIDV.bat <idv options>
REM 
REM  Notes:   In past versions of the IDV, users had to change this script to
REM  manipulate memory settings. The IDV now configures the  appropriate memory. 
REM  Users can also change the memory via the Preferences  menu. In exceptional 
REM  situations where the IDV may not start due to memory issues, it may be 
REM  necessary to bootstrap the memory size. In this case, please uncomment the 
REM  idv_memory section below and subsequently choose memory via the Preferences
REM  menu.  Be sure to comment it out that after setting the memory via the 
REM  Preferences if you want the preference to take effect. 
REM  A similar problem exists with "PermGenSpace" memory.  This can also be set in
REM  the Preferences under the System Tab.  However, if you cannot start the IDV
REM  and have error messages about "PermGenSpace", uncomment the 
REM  idv_maxpermgensize=128 line below and then start the IDV.  Set this value
REM  in the Preferences, then comment out the line again and restart the IDV.
REM ##############################################################################

setlocal

FOR /F "tokens=*" %%i IN ('jre\bin\java -cp idv.jar
    ucar.unidata.idv.IdvCommandLinePrefs %* 2^>NUL') DO SET %%i
    
IF %idv_memory%.==. (
    echo IDV failed to start. Please contact support-idv@unidata.ucar.edu
    GOTO end)
	    
REM Stripping quotes
set idv_memory=%idv_memory:"=%
set idv_maxpermgensize=%idv_maxpermgensize:"=%

REM See important note about this above. To bootstrap the IDV memory, uncomment 
REM the line below and set to a value  in megabytes. 
REM set idv_memory=512

REM See important note about this above. If you cannot start the IDV because of
REM PermGenSpace errors, uncomment the line below.
REM set idv_maxpermgensize=128

@echo on    
jre\bin\java -Djava.ext.dirs=Java3D -Xmx%idv_memory%m -XX:PermSize=%idv_maxpermgensize%m -XX:MaxPermSize=%idv_maxpermgensize%m -XX:+DisableExplicitGC -Didv.enableStereo=false -jar idv.jar %*
@echo off

REM Use the line below instead if you want to use the D3D version of Java 3D
REM jre\bin\java -Djava.ext.dirs=Java3D -Xmx%idv_memory%m -XX:PermSize=%idv_maxpermgensize%m -XX:MaxPermSize=%idv_maxpermgensize%m -XX:+DisableExplicitGC -Dj3d.rend=d3d -jar idv.jar %*

	    
endlocal
   
:end
