
####################################################################################
#######  Map related utilities
####################################################################################



def make3DMap(map, topo):
  """Make a 3d map. map -  map line data - topo - topography dataset
  """
  b = DerivedGridFactory.create2DTopography(topo, topo)
  c = b.resample(map)
  return c



def  subsetFromMap(field, mapSets, fillValue=java.lang.Float.NaN,inverse=0):
    """mapSets defines a set of polygons. This procedure fills the areas in the field that are not
    enclosed by the polygons with the fill value. If inverse is 1 then it fills the areas that are
    enclosed
    """
    if (GridUtil.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = subsetRangeFromMap(field.getSample(timeStep), timeStep, mapSets, fillValue, inverse); 
            newData.setSample(timeStep,rangeObject)
        return newData
    return subsetRangeFromMap(field, 0, map, fillValue, inverse);    


def  subsetRangeFromMap(range, timeStep, mapSets, fillValue=java.lang.Float.NaN,inverse=0):
    """mapSets defines a set of polygons. This procedure fills the areas in the field that are not
    enclosed by the polygons with the fill value. If inverse is 1 then it fills the areas that are
    enclosed
    """
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





def  mapsApplyToField(function, field, mapSets,inside):
    """mapSets defines a set of polygons. This procedure fills the areas in the field are enclosed
    by each polygon with the average value within that area
    """
    if (GridUtil.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = mapsApplyToRange(function, field.getSample(timeStep), timeStep, mapSets,inside);
            newData.setSample(timeStep,rangeObject)
        return newData
    else:   
	return mapsApplyToRange(function, field, 0, mapSets);



def  mapsApplyToRange(function, range, timeStep, mapSets,inside):
    """mapSets defines a set of polygons. This procedure fills the areas in the field are enclosed
    by each polygon with the average value within that area
    """
    rangeObject = range.clone();
    if(inside):
	    indices = GridUtil.findContainedIndices(rangeObject.getDomainSet(), mapSets);
    else:
	    indices = GridUtil.findNotContainedIndices(rangeObject.getDomainSet(), mapSets);
    originalValues = rangeObject.getFloats(0)
    newValues = cloneArray(originalValues);

    for mapIdx in xrange(len(indices)):
        indexArray = indices[mapIdx]
	eval(function);
    rangeObject.setSamples(newValues)
    return rangeObject;


def  mapsAverage(originalValues, newValues, indexArray):
    DataUtil.average(originalValues, newValues, indexArray);

def mapsAbsoluteValue(originalValues, newValues, indexArray):
 	DataUtil.absoluteValue(originalValues, newValues, indexArray);

def mapsThresholdUpper(originalValues, newValues, indexArray, threshold):
 	DataUtil.thresholdUpper(originalValues, newValues, indexArray,threshold);

def mapsThresholdLower(originalValues, newValues, indexArray, threshold):
 	DataUtil.thresholdLower(originalValues, newValues, indexArray,threshold);
	


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
        """Return a new set of maps whose property propName satisfies the given operator/value.
        The operators can be ==,!=, <,>,<=,>=, match, !match"""
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
    """Make a field whos lat/lon area is the bounds of the given mapSet. It has length1 points in the x and length2 in the y.
    Fill it with the fill value and the given unit"""
    low = mapSets.getLow();
    hi = mapSets.getHi();
    return Util.makeField(low[0],hi[0],int(length1),low[1],hi[1],int(length2), float(fill),unit);





def  subsetWithProperty(field, mapSets):
    """test code"""
    if (GridUtil.isTimeSequence(field)):
        newData = field.clone()
        for timeStep in range(field.getDomainSet().getLength()):
            rangeObject = subsetRangeWithProperty(field.getSample(timeStep), mapSets);
            newData.setSample(timeStep,rangeObject)
        return newData
    return subsetRangeWithProperty(field, mapSets);



def  subsetRangeWithProperty(range, mapSets):
    """test code"""
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
