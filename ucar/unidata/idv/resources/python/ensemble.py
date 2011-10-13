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

def ens_uprob(grid, logicalOp1, pValue1, and_or, logicalOp2, pValue2, exptdLoBound, exptdUpBound):
   """ ensemble univariate probability calculation """
   #
   # define a few custom exception types
   #
   class LogicError(Exception):
      # could do more fancy stuff, but just needed to define a new Exception Type
      pass

   class FeatureNotImplemented(Exception):
      # could do more fancy stuff, but just needed to define a new Exception Type
      pass
   #
   # Begin computing probabilities!
   #  note: GridMath.ensembleUProbabilityValues computes P(x < pValue)
   #
   if ((exptdLoBound != '') and (exptdUpBound != '')):
      if (exptdLoBound >= exptdUpBound):
         raise ValueError("The expected lower bound muss be less than the expected upper bound.")

   if pValue1 == '':
      raise Exception("Must have at least one valid probability statement - user input 'a' cannot be blank.")

   if (logicalOp1 == 'lt'):
      prob1 = GridMath.ensembleUProbabilityValues(grid, logicalOp1, pValue1, exptdLoBound, exptdUpBound)
   elif (logicalOp1 == 'gt'):
      prob1 = 1.0 - GridMath.ensembleUProbabilityValues(grid, logicalOp1, pValue1, exptdLoBound, exptdUpBound)

   if (logicalOp2 == 'NotUsed'):
      prob = prob1
   else:
      if pValue2 == '':
         raise Exception("Must have a valid 2nd probability statement - user input 'b' cannot be blank.")
      elif (pValue1 == pValue2):
         raise FeatureNotImplemented("pValue1 and pValue2 are the same -> P(X = pValue) is not handled in the IDV.")

      if (logicalOp2 == 'lt'):
         prob2 = GridMath.ensembleUProbabilityValues(grid, logicalOp2, pValue2, exptdLoBound, exptdUpBound)
      elif (logicalOp2 == 'gt'):
         prob2 = 1.0 - GridMath.ensembleUProbabilityValues(grid, logicalOp2, pValue2, exptdLoBound, exptdUpBound)
      else:
         raise FeatureNotImplemented(exceptMsg[2])

      if (logicalOp1 == logicalOp2):
         prob = prob1
      elif (logicalOp1 != logicalOp2):
         #
         # Create probability or logical statements based on user input (used in exception error messages)
         #
         errLogicOp = {'gt' : ('<', '>'),'lt' : ('>', '<')}
         errAndOr = ('or','and')

         errorLogicStr = ["P(X %s %s) %s P(X %s %s)"%(errLogicOp[logicalOp2][0],
                                                      str(pValue1),
                                                      errAndOr[0],
                                                      errLogicOp[logicalOp2][1],
                                                      str(pValue2)),
                          "X %s %s %s X %s %s"%(errLogicOp[logicalOp2][0],
                                                str(pValue1),
                                                errAndOr[1],
                                                errLogicOp[logicalOp2][1],
                                                str(pValue2))]
         #
         # Construct exception error messages
         #
         exceptMsg = ["%s would be P(X contained on the real number line)."%(errorLogicStr[0]),
                      "%s cannot happen."%(errorLogicStr[1]),
                      "Not handled: %s. Please report this error to support-idv@unidata.ucar.edu"%(errorLogicStr[0])]
         #
         # Could reduce the size of the following statements, but will keep as is for clarity.
         #
         if logicalOp2 == 'gt':
            if ((and_or == 'or') and (pValue1 < pValue2)):
               prob = prob1 + prob2
            elif ((and_or == 'and') and (pValue1 > pValue2)):
               prob = 1.0 - abs(prob1 - prob2)
            elif ((and_or == 'or') and (pValue1 > pValue2)):
               raise LogicError(exceptMsg[0])
            elif ((and_or == 'and') and (pValue1 < pValue2)):
               raise LogicError(exceptMsg[1])
            else:
               raise FeatureNotImplemented(exceptMsg[2])
         elif (logicalOp2 == 'lt'):
            if ((and_or == 'or') and (pValue1 > pValue2)):
               prob = prob1 + prob2
            elif ((and_or == 'and') and (pValue1 < pValue2)):
               prob = 1.0 - abs(prob1 - prob2)
            elif ((and_or == 'or') and (pValue1 < pValue2)):
               raise LogicError(exceptMsg[0])
            elif ((and_or == 'and') and (pValue1 > pValue2)):
               raise LogicError(exceptMsg[1])
            else:
               raise FeatureNotImplemented(exceptMsg[2])
         else:
            raise FeatureNotImplemented(exceptMsg[2])
      else:
         # not sure the user could get to this, but here for future expansion where logicalOp* could be based
         #  on user input rather than a controlled list (2011/10/03 SCA).
         raise FeatureNotImplemented(exceptMsg[2])

   return prob

def ens_srng(grid):
   """ max - min grid value """
   return GridMath.ensembleRangeValues(grid)

def ens_mode(grid):
   """ mode value """
   return GridMath.ensembleModeValues(grid)
