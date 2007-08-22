#
#A jython library of wrapper routines around the isl interpreter
#

import java

import sys;

sys.add_package('visad');
sys.add_package('visad.python');

sys.add_package('java.util');

from visad.python.JPythonMethods import *
from java.util import ArrayList

def pause():
    idv.waitUntilDisplaysAreDone();


def setOffScreen(offScreen=1):
    idv.getArgsManager().setIsOffScreen(offScreen);

def setDebug(debug=1):
    interp.setDebug(debug);

# You can provide a python 'dictionary' of datasource:pattern pairs
# which provide the 'setfiles' functionality in the bundle isl tag
def loadBundle(bundleFile, setFilesDict=None):
    if (setFilesDict != None):
      setFiles = ArrayList()
      keys = setFilesDict.keys()
      for k in keys:
        setFiles.add(k)
        setFiles.add(setFilesDict.get(k))

    else:
      setFiles = None
      
    interp.loadBundle(bundleFile,setFiles);
      
# If you want to modify the image, add in the isl-defined parameters
# in the form:  params="resize width=300 height=400;matte bottom=30"
def writeImage(file, params="", quality=1.0):
     interp.writeImage(file, params, quality);


# If you want to modify the movie, add in the isl-defined parameters
# in the form:  params="resize width=300 height=400;matte bottom=30"
def writeMovie(file,params=''):
    interp.writeMovie(file,params);


## The following are for dealing with an Image...

## Returns an image from the current display
def getImage():
    return interp.getImage();

## to change the size of the image
def resizeImage(image,width=-1,height=-1):
    return interp.resizeImage(image,str(width),str(height));

## to matte the image
def matteImage(image,color='white',top=0,left=0,bottom=0,right=0):
    return interp.matteImage(image,color,top,left,bottom,right);

## once you find the DisplayControl, use dc.getImage() to get particular image
## use writeImageToFile() to save the image
def findDisplayControl(id):
    return interp.findDisplayControl(id);

## Write an image to a file.  The file extension (.jpg, .png, .gif)
## determines the type.
def writeImageToFile(image,file):
    interp.writeImage(image,file);

