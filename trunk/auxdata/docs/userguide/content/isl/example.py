
##################################################
#This is an example jython script
#Run this:
#idv -islfile example.py
##################################################


##Turn on off screen mode 
setOffScreen(1);
setDebug(1);


##################################################
##Load in a bundle. The islpath is just like the one in the normal isl xml
##i.e., the directory of this file
loadBundle ('${islpath}/test.xidv');

#Note, you can set the files used in the data sources
#ala the setfiles tag in the isl using:
#loadBundle ('${islpath}/test.xidv',dict);
#where "dict" is a dictionary of datasource:file names.


##################################################
##wait for the dsiplays to finish rendering
pause()

##################################################
## now, save the image with a top (north) matte of red
writeImage('${islpath}/test.png','matte top=100 background=red',1.0)


