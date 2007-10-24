
"""A set of utilities for selecting data and creating displays.
   For use from the Jython shell."""

import ucar.unidata.data.DataChoice as DataChoice


def selectData(name1='Select Field',name2=None,name3=None,name4=None,name5=None):
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
    shell.toFront();
    if(result == None): 
	return None;
    if(result.size()==1):
	return result.get(0);
    return result



def createDisplay(displayType, data, dataName='Data'):
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
        shell.toFront();
        return control


def showLib():
    idv.getJythonManager().showJythonEditor()


def clear():
	shell.clear();


def listVars():
	shell.listVars();


def api(object):
	return idv.listApi(object);

def printType(data):
	if(isinstance(data, DataChoice)==1):
		shell.output('Parameter:' + str(data))
		data = data.getData(None);
	shell.printType(data);


def makeDataSource(path,type=None):
	return idv.makeOneDataSource(path, type,None);


