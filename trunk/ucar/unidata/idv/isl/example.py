
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
## first way to save an image
writeImage('${islpath}/test.png','matte top=100 background=red',1.0)


#Write the movie. The second argument is option but can be the isl image manipulation xml
writeMovie('/home/jeffmc/test.mov,/home/jeffmc/test.kmz','resize width=100');


##################################################
## Here is a way to find a particular dispay and capture it
## The 'probe' argument is the id of the display control
## You set the from the properties dialog of the display
display = findDisplayControl('probe');
if display!= None:
    image = display.getImage(None);
    writeImage(image,'${islpath}/test1.png');
    #Some of the displays can take an argument that defines
    #what should be captured. For example, in the probe control
    #you can capture just the chart
    image = display.getImage('chart');
    writeImage(image,'${islpath}/test2.png');


##################################################
##Get an image and modify it
image = getImage();
image = resizeImage(image,200);
image = matteImage(image,'red',top=100);

#Write  out the image
writeImage(image,'${islpath}/test.png');

