
"""A set of miscellaneous utilities. """

def makeFloatArray(rows,cols,value):
    """ A utility to make a 2 dimensional float array filled 
        with the given value 
    """
    return DataUtil.makeFloatArray(rows,cols,value);

def idveval(formula):
  """  evaluate a formula """
  from ucar.unidata.data import DerivedDataChoice
  ddc = DerivedDataChoice(idv,formula)
  return ddc.getData(None)
