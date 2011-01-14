
"""A set of utilities for selecting data and creating displays.
   For use from the Jython shell."""

import ucar.unidata.data.DataChoice as DataChoice

shell=None

def selectData(name1='Select Field',name2=None,name3=None,name4=None,name5=None):
    """Select up to 5 data fields. This returns a List of the actual Data objects """
    result = selectDataChoice(name1,name2,name3,name4,name5);
    if(result == None): 
        return None;
    if(isinstance(result, DataChoice)==1):
        return result.getData(None);
    dataList = java.util.ArrayList();
    for i in range(result.size()):
        dataList.add(result.get(i).getData(None));
    if(dataList.size()==1):
        return dataList.get(0);
    return dataList


def selectDataChoice(name1='Select Field',name2=None,name3=None,name4=None,name5=None):
    """Select up to 5 data choices. This returns a List of the data choices, not the actual Data
     To get the data do:   <div class=jython>dataList.get(0).getData(None)</div>"""
    list = java.util.ArrayList();
    list.add(name1);
    if(name2!=None):
        list.add(name2);
    if(name3!=None):
        list.add(name3);
    if(name4!=None):
        list.add(name4);
    if(name5!=None):
        list.add(name5);
    result = idv.selectDataChoices(list);
    if (shell != None):
       shell.toFront();
    if(result == None): 
        return None;
    if(result.size()==1):
        return result.get(0);
    return result



def createDisplay(displayType, data, dataName='Data'):
        """create a display of type displayType. Right click in input field to select particular displayType.
        The data is can be a data object, a datachoice or a list of data or datachoices
        The dataName is used to name the data, i.e., its the parameter name
        """
        import ucar.unidata.data.DataDataChoice as DataDataChoice
        import ucar.unidata.data.DataChoice as DataChoice
        if(isinstance(data, java.util.List)==0):
                tmp = java.util.ArrayList();            
                tmp.add(data);
                data = tmp;

        dataList = java.util.ArrayList();
        for i in range(data.size()):
            obj = data.get(i)
            if(isinstance(obj, DataChoice)==0):
                label = dataName
                if(data.size()>1):
                    label = label +str(i)
                obj = DataDataChoice(label,obj);
            dataList.add(obj);
        control = idv.doMakeControl(displayType, dataList);
        if (shell != None):
            shell.toFront();
        return control


def showLib():
    """Bring up the jython library dialog """
    idv.getJythonManager().showJythonEditor()


def clear():
        """Clear the shell """
        shell.clear();


def listVars():
        """List all of the variables defined in the shell's interpreter """
        shell.listVars();


def printType(data):
        """Print out the math type of the given data """
        if(isinstance(data, DataChoice)==1):
                shell.output('Parameter:' + str(data))
                data = data.getData(None);
        shell.printType(data);


def makeDataSource(path,type=None):
        """Create a datasource from the given file name or url. The optional type parameter
        is used to specify the type of data"""
        return idv.makeOneDataSource(path, type,None);


def findDataSource(name=None):
    """Find the data source object with the given name. If no name is given then this will return the first (non-formula)
    data source"""
    return idv.getDataManager().findDataSource(name);


def getDataChoice(dataSourceName=None,dataChoiceName=None):
    """Find the data source with the given name and the data choice on that data source with the given name.
    If no dataSourceName is given then use the first one in the list
    If no dataChoiceName is given then use the first one held by the data source
    Return the data choice
    If no data source or data choice is found then return null"""

    dataSource=idv.getDataManager().findDataSource(dataSourceName);
    if(dataSource == None):
        return None;
    return  dataSource.findDataChoice(dataChoiceName);


def getData(dataSourceName=None,dataChoiceName=None):
    """Find the data source with the given name and the data choice on that data source with the given name.
    If no dataSourceName is given then use the first one in the list
    If no dataChoiceName is given then use the first one held by the data source
    Return the data for the data choice.
    If no data source or data choice is found then return null"""
    dataChoice = getDataChoice(dataSourceName, dataChoiceName);
    if(dataChoice==None):
        return None;
    return dataChoice.getData(None);



def setDataSources():
    """This procedure will define a set of jython variables, 'dataSource0, dataSource1, ...'  that correspond to 
    loaded data sources."""
    dataSources  = idv.getDataSources();
    for i in range(dataSources.size()):
        dataSource = dataSources.get(i);
        interpreter.set('dataSource'+str(i),dataSource);
        shell.output('dataSource' + str(i) +'= ' +str(dataSource));
        shell.output('<br>');
         

def setDataChoices(dataSource=None):
    """The given dataSource can be an actual data source or the name of a data source.
    This procedure will define a set of jython variables that correspond to the data choices
    held by the given data source. """
    if(dataSource==None):
        dataSource = findDataSource();
    if(dataSource == None):
        return;
    if(isinstance(dataSource,java.lang.String)==1):
        dataSource = findDataSource(dataSource);
    if(dataSource == None):
        return;
    choices  = dataSource.getDataChoices();
    for i in range(choices.size()):
        dataChoice  = choices.get(i)
        interpreter.set(dataChoice.getName(), dataChoice);
        shell.output(dataChoice.getName() + '=' +dataChoice.getDescription());
        shell.output('<br>');
