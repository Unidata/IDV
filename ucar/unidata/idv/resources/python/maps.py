
####################################################################################
#######  Map related utilities
####################################################################################



def make3DMap(map, topo):
  """Make a 3d map    map -  map line set - topo - topography dataset """
  b = DerivedGridFactory.create2DTopography(topo, topo)
  c = b.resample(map)
  return c



def  subsetFromMap(field, mapSets, fillValue=java.lang.Float.NaN,inverse=0):
##Iterate on each time step
    if (GridUtil.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = subsetRangeFromMap(field.getSample(timeStep), timeStep, mapSets, fillValue, inverse); 
            newData.setSample(timeStep,rangeObject)
        return newData
    return subsetRangeFromMap(field, 0, map, fillValue, inverse);    


def  subsetRangeFromMap(range, timeStep, mapSets, fillValue=java.lang.Float.NaN,inverse=0):
    rangeObject = range.clone()
    indices = GridUtil.findContainedIndices(rangeObject.getDomainSet(), mapSets);
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



def  averageFromMap(field, mapSets):
    if (GridUtil.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = averageRangeFromMap(field.getSample(timeStep), timeStep, mapSets);
            newData.setSample(timeStep,rangeObject)
        return newData
    else:   
        averageRangeFromMap(field,0, mapSets);        


def  averageRangeFromMap(range, timeStep, mapSets):
    rangeObject = range.clone()
    indices = GridUtil.findContainedIndices(rangeObject.getDomainSet(), mapSets);
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



def getMapProperty(polygon, propName):
        """Get the named property from the given mapData"""
        from ucar.visad.data import MapSet
	if(isinstance(polygon, MapSet)):
            return polygon.getProperty(propName);
	return None;


def getMapsWithProperty(mapSets, propName,value):
        """Return a new set of maps that have the given property value"""
	return filterMaps(mapSets, propName, '==', value);


def filterMaps(mapSets, propName,operator,value):
        from ucar.visad import ShapefileAdapter
        from ucar.unidata.util import StringUtil
	goodOnes = java.util.ArrayList();
	sets = mapSets.getSets();
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




def makeFieldFromMapBounds(mapSets, length1, length2, fill, unit):
    low = mapSets.getLow();
    hi = mapSets.getHi();
    return Util.makeField(low[0],hi[0],int(length1),low[1],hi[1],int(length2), float(fill),unit);





def  subsetWithProperty(field, mapSets):
    if (GridUtil.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = subsetRangeWithProperty(field.getSample(timeStep), mapSets);
            newData.setSample(timeStep,rangeObject)
        return newData
    return subsetRangeWithProperty(field, mapSets);



def  subsetRangeWithProperty(range, mapSets):
    rangeObject = range.clone()
    indices = GridUtil.findContainedIndices(rangeObject.getDomainSet(), mapSets);
    originalValues = rangeObject.getFloats(0)
    newValues = makeFloatArray(len(originalValues), len(originalValues[0]), java.lang.Float.NaN);
    polygons   = mapSets.getSets();
    for mapIdx in xrange(len(indices)):
        polygon =polygons[mapIdx]
        value = getMapProperty(polygon, 'LENGTH');
        print value
        indexArray = indices[mapIdx]
        for j in xrange(len(indexArray)):
            if(value==None):
                 newValues[0][indexArray[j]] = originalValues[0][indexArray[j]];
            else:
                newValues[0][indexArray[j]] = float(value);
    rangeObject.setSamples(newValues)
    return rangeObject;