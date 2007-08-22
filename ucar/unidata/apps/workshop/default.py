from visad.python.JPythonMethods import *
#
# temperature anomaly from U.S. Standard Atmosphere 
def tempAnom(t):
    # get pressure levels of temperature grids
    p=extractPressureFromNWPGrid(t)
    # calculate temperature for a constant lapse rate (6.5 C/km) atmosphere
    tstd=288.15*(p/1013.25)**(287.05*.0065/9.806)
    # change temperature in stratosphere to isothermal (216.65 K)
    for i in range(len(p)):
      if p[i] < 225.0: 
        tstd[i]=216.65
    # calculate the temperature anomaly
    tanom=t-tstd
    return tanom

#
# function dBz2R to calculate rainfall rate from dBz reflectivity
#
def dBz2R(dBz,a=200,b=1.6):
      a = float(a)
      b = float(b)
      # a Z to R relation, where a and b are 
      # Marshall-Palmer constants. Results in in/hr.
      c=10.**(dBz/10.)
      d=c/a
      r=d**(1.0/b)
      r=0.039*r
      return r


