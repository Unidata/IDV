

####################################################################################
#######  Map related utilities
####################################################################################

import ucar.unidata.data.grid.GridUtil as gu


def  subsetFromMap(field, map, fillValue=java.lang.Float.NaN,inverse=0,latLonCanChangeWithTime=1):
##Iterate on each time step
    if (gu.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = subsetRangeFromMap(field.getSample(timeStep), timeStep, map, fillValue, inverse); 
            newData.setSample(timeStep,rangeObject)
        return newData
    return subsetRangeFromMap(field, 0, map, fillValue, inverse);    


def  subsetRangeFromMap(range, timeStep, map, fillValue=java.lang.Float.NaN,inverse=0):
    rangeObject = range.clone()
    indices = gu.findContainedIndices(rangeObject.getDomainSet(), map);
    originalValues = rangeObject.getFloats(0)
    if(inverse):
        newValues = originalValues;
    else:
        newValues = makeFloatArray(len(originalValues), len(originalValues[0]), fillValue);
##Look at list of indices in each map
##if we are doing inverse then we set the index to the fill value
##else we already filled the array and we set the index to the original value
    for mapIdx in xrange(len(indices)):
        indexArray = indices[mapIdx]
        for j in xrange(len(indexArray)):
           if(inverse):
               newValues[0][indexArray[j]] = fillValue
           else:
               newValues[0][indexArray[j]] = originalValues[0][indexArray[j]];
##Set the samples
    rangeObject.setSamples(newValues)
    return rangeObject;



def  averageFromMap(field, map,latLonCanChangeWithTime=1):
    if (gu.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = averageRangeFromMap(field.getSample(timeStep), timeStep, map);
            newData.setSample(timeStep,rangeObject)
        return newData
    else:   
        averageRangeFromMap(field,0, map);        


def  averageRangeFromMap(range, timeStep, map):
    rangeObject = range.clone()
    indices = gu.findContainedIndices(rangeObject.getDomainSet(), map);
    originalValues = rangeObject.getFloats(0)
    newValues = makeFloatArray(len(originalValues), len(originalValues[0]), java.lang.Float.NaN);
    totals = {};
    for mapIdx in xrange(len(indices)):
        indexArray = indices[mapIdx]
        total = 0;
        for j in xrange(len(indexArray)):
            total=total+ originalValues[0][indexArray[j]];
        totals.update({mapIdx:total})

    for mapIdx in xrange(len(indices)):
        indexArray = indices[mapIdx]
        if(len(indexArray)==0):
            continue;
        avg = totals[mapIdx]/len(indexArray)
        for j in xrange(len(indexArray)):            
            newValues[0][indexArray[j]] = avg
    rangeObject.setSamples(newValues)
    return rangeObject;





## Get the named property from the given mapData
def getMapProperty(mapData, propName):
        from ucar.visad.data import MapSet
	if(isinstance(mapData, MapSet)):
		return mapData.getProperty(propName);
	return None;


#Return a new set of maps that have the given property value
def getMapsWithProperty(mapData, propName,value):
	return filterMaps(mapData, propName, '==', value);


def filterMaps(mapData, propName,operator,value):
        from ucar.visad import ShapefileAdapter
        from ucar.unidata.util import StringUtil
	goodOnes = java.util.ArrayList();
	sets = mapData.getSets();
        for mapIdx in xrange(len(sets)):
		mapValue =  getMapProperty(sets[mapIdx],propName);
		if(mapValue == None): 
			continue;
		if(operator== '==' and mapValue == value):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== '!=' and mapValue != value):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== '<' and mapValue < value):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== '<=' and mapValue <= value):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== '>' and mapValue > value):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== '>=' and mapValue >= value):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== 'match' and StringUtil.stringMatch(str(mapValue), value)):
			goodOnes.add(sets[mapIdx]);		
		elif(operator== '!match' and not StringUtil.stringMatch(str(mapValue), value)):
			print "not match: " + mapValue;
			goodOnes.add(sets[mapIdx]);		

	return ShapefileAdapter.makeSet(goodOnes);

