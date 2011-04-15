@echo off

setlocal

FOR /F "tokens=*" %%i IN ('jre\bin\java -cp idv.jar
    ucar.unidata.idv.IdvCommandLinePrefs %* 2^>NUL') DO SET %%i
    
IF %idv_memory%.==. (
    echo IDV failed to start. Please contact support-idv@unidata.ucar.edu
    GOTO end)
	    
REM Stripping quotes
set idv_memory=%idv_memory:"=%
	    
jre\bin\java -Xmx%idv_memory%m -Didv.enableStereo=false -jar idv.jar %*
	    
REM Use the line below instead if you want to use the D3D version of Java 3D
REM jre\bin\java -Xmx512m -Dj3d.rend=d3d -jar idv.jar %*
	    
endlocal
   
:end
	    