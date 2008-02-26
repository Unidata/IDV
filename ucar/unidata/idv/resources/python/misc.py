
"""A set of miscellaneous utilities. """

def makeFloatArray(rows,cols,value):
    """ A utility to make a 2 dimensional float array filled 
        with the given value 
    """
    return DataUtil.makeFloatArray(rows,cols,value);

def idveval(formula):
  """  evaluate a formula """
  from ucar.unidata.data import DerivedDataChoice
  from ucar.unidata.data import DataCancelException
  derivedDataChoice.setName(formula)
  try:
    ddc = DerivedDataChoice(idv,formula)
    return ddc.getData(None)  
  except DataCancelException, dce:
    return None;    






def printSoundings(d):
        """ Print out the values of the set of sounding data """
	numSoundings = d.getDimension();
        for i in range(numSoundings):
                sounding = d.getComponent(i);
                printSounding(sounding);


def printSounding(sounding):
        """ Print out the values of the sounding data """
        from ucar.unidata.util import StringUtil;
        numFields = sounding.getDimension();
        dateTime = sounding.getComponent(0);
        location = sounding.getComponent(1);
        print "date:" + str(dateTime);
        print "location:" + str(location);

        for fieldIdx in range(2,numFields):
                rowBuffers =  ArrayList();
                sb = java.lang.StringBuffer();
                rowBuffers.add(sb);
                field = sounding.getComponent(fieldIdx);
                rangeType= field.getType().getRange();
                domainSamples = field.getDomainSet().getSamples();a
                rows = field.getDomainSet().getLength();
                fieldName= str(field.getType().getRange());
                domainType = field.getDomainSet().getType().getDomain();
                sb.append(str(domainType));
                sb.append(", ");
                fieldName = fieldName.replace("(","");
                fieldName = fieldName.replace(")","");
                sb.append(fieldName);
                for row in range(rows):
                        sb = java.lang.StringBuffer();
                        rowBuffers.add(sb);
                        sb.append(str(domainSamples[0][row]));
                        sb.append(", ");
                        data = field.getSample(row);
                        dataString = str(data);
                        # a hack to deal with the spd/dir tuple
                        dataString = dataString.replace("(","");
                        dataString = dataString.replace(")","");
                        sb.append(dataString);
                print StringUtil.join("\n",rowBuffers);
                print "\n\n";

