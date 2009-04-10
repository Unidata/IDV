#!/bin/sh

## To run RAMADDA stand-alone just do
echo 'Running RAMADDA on port 8080'
echo 'Access it at: http://localhost:8080/repository'
java -Xmx512m -jar repository.jar -port 8080 


##This will create a directory under ~/unidata/repository to store content and the database
#To change the directory do:
##java -Xmx512m -jar repository.jar -port 8080 -Dramadda_home=/some/other/directory


#The default above is to use Java Derby as the database
#To run with mysql you do:
#java -Xmx512m -jar ramadda.jar -Dramadda.db=mysql

##Or see for more information: 
##http://www.unidata.ucar.edu/software/ramadda/docs/developer/ 
 









