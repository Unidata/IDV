#!/bin/sh

#This runs ramadda with a mysql data base
#The database and user/password is defined in the repository.properties file
#To change it make a repository.properties file with the following 3 lines.
#Put it in ~/.unidata/repository

#jdms.db.mysql.url=jdbc:mysql://localhost:3306/repository?zeroDateTimeBehavior=convertToNull
#jdms.db.mysql.user=jeff
#jdms.db.mysql.password=mypassword


java -Xmx512m -jar repository.jar -Djdms.db=mysql


#To run the repository using a local java derby DB just do:
#java -Xmx512m -jar repository.jar 


