#These are example jython procedures that get called by the JythonControl 
#at different times. See below for details and examples
from javax.swing import *
from javax.swing.JLabel import *
	
#This gets called when the JythonControl is first created.
def handleInit (displayControl):
	exampleInit(displayControl);

#This gets called when there is data that has been loaded
def handleData (displayControl):
	exampleData(displayControl);

#This gets called when the probe position has changed. 
def handleProbe (displayControl, probeLocation):
	exampleProbe(displayControl,probeLocation);

#This gets called when the animation time changes
def handleTime (displayControl, probeLocation):
	exampleTime(displayControl,probeLocation);


#This gets called when the JythonControl is first created.
def exampleInit (displayControl):
	#You can add any number of menuitems to the different menus
	#displayControl.addSaveMenuItem('save 2','menuCallback');
	#displayControl.addFileMenuItem('file 1','menuCallback');
	#displayControl.addEditMenuItem('edit 1','menuCallback');
	#displayControl.addViewMenuItem('view 1','menuCallback');

        #Create a JTextArea and put it into the display's window
	comp = JTextArea  ("",8,30)
        comp.setEditable(0)


	#setJythonComponent sets the GUI
	displayControl.setJythonComponent (JScrollPane(comp))

        #Save state with the setVar/getVar methods on the display
	displayControl.setVar ('comp', comp)

	#You can also set a component in the legend
	legendComp = JLabel("Side legend component");
	displayControl.setLegendComponent (legendComp);




#This gets called when there is data that has been loaded
def exampleData (displayControl):
	#The getDataList method returns a list of the data, one per data choice
	dataList = displayControl.getDataList();
        comp = displayControl.getVar ('comp');
	comp.setText ("data list size=" + str (dataList.size()))


#This gets called when the probe position has changed. 
#The probeLocation is an array of EarthLocationTuple-s
#For point, level and  vertical probes the length is 1
#For the horizonal probe the length is 2 (the end points of the probe line)
#For the area probe the  length is 4 (upper-left, upper-right, lower-right, lower-left)
def exampleProbe (displayControl, probeLocation):
        #You can get the current animation time with:
	animationTime = displayControl.getAnimationTime();

	#You can get a list of all animation times with:
	#times = displayControl.getAnimationTimes();

	#You can get a list of all times from the data with:
	#times = displayControl.getTimesFromData();

        #This samples the data at the current probe location
        sample = displayControl.sampleAtTime();
        comp = displayControl.getVar ('comp');
	comp.setText ('sample: ' +str(sample) + ' at:' + str(animationTime))
        #You can also just call sample to get all values across time:
        #sample = displayControl.sample();



#This gets called when the animation time changes
def exampleTime (displayControl, probeLocation):
        #Just turn around and call handleProbe
        exampleProbe(displayControl, probeLocation);



def menuCallback(displayControl):
        comp = displayControl.getVar ('comp');
	comp.setText ('menuCallback called')

