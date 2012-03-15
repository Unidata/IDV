

def exampleSubsetting():
	#Create a data source
	dataSource = makeDataSource("ruc.nc")

	#Find the temperature dataChoice
	dataChoice = dataSource.findDataChoice("T")

	#create a DataSelection
	dataSelection = DataSelection()

	#Set both the x and y stride on it
	dataSelection.setXYStride(5);

        #Note: you can also do:
        #dataSelection.setXStride(5);
        #dataSelection.setYStride(5);
        #dataSelection.setZStride(5);

	#Set the spatial bounds. This takes north, west, south, east
	dataSelection.setBounds(45,-120,40,-100)

	#create a list of times and add an integer 0 to it
	#This says use the first time
	times = ArrayList()
	times.add(Integer(0))
	dataSelection.setTimes(times)

        #Set the level to use. For example, this is the 5th level (they are 0 based)
        dataSelection.setLevel(Integer(4))

	#Now get the data and create the display
	tempField = dataSource.getData(dataChoice, None,dataSelection,None)
	createDisplay('planviewcolor', tempField,"T")

        

        
