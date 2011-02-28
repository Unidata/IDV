""" This is the doc for the ensemble grid module """

def ens_savg(grid):
   """ basic ensemble average """
   return GridMath.averageOverMembers(grid)

def ens_ssprd(grid):
   """ basic ensemble average """
   return GridMath.ensembleStandardDeviation(grid)

def ens_smax(grid):
   """ max value of all member """
   return GridMath.ensembleHighestValues(grid)

def ens_smin(grid):
   """ min value of all member """
   return GridMath.ensembleLowestValues(grid)

def ens_prcntl(grid, percent):
   """ percentile value """
   return GridMath.ensemblePercentileValues(grid, percent)

def ens_srng(grid):
   """ max - min grid value """
   return GridMath.ensembleRangeValues(grid)