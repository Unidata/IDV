#!/bin/sh

#This runs ramadda with a java derby data base
#java -Xmx512m -jar repository.jar

#It will listen to port 8080. To change the port do:
#java -Xmx512m -jar repository.jar -port <some port>


#To run with mysql you do:
java -Xmx512m -jar repository.jar -Djdms.db=mysql

#The mysql database and user/password is defined in the repository.properties 
#file. To change it make a repository.properties file with the following 
#3 lines and put it in ~/.unidata/repository

#jdms.db.mysql.url=jdbc:mysql://localhost:3306/repository?zeroDateTimeBehavior=convertToNull
#jdms.db.mysql.user=jeff
#jdms.db.mysql.password=mypassword






