REM To run RAMADDA stand-alone just do

java -Xmx512m -jar repository.jar -port 8080 %*


REM This will create a directory under ~/unidata/repository to store content and the database
REM To change the directory do:
REM java -Xmx512m -jar repository.jar -port 8080 -Dramadda_home=/some/other/directory


REM The default above is to use Java Derby as the database
REM To run with mysql you do:
REM java -Xmx512m -jar ramadda.jar -Dramadda.db=mysql

REM Or see for more information: 
REM http://www.unidata.ucar.edu/software/ramadda/docs/developer/ 
 









