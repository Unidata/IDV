""" This is the doc for the test module """

def changeRange(d):
   return   applyToRangeValues('testApplyToRange2',d);

def testApplyToRange(d,**args):
    r = d.getFloats(0)
    total = 0
    for i in xrange(len(r[0])):
        total= total+r[0][i]
    avg = total/len(r[0])
    for i in xrange(len(r[0])):
        if(r[0][i]<avg):
            r[0][i] = 0;
    d.setSamples(r)
    return d

def testApplyToRange2(r,**args):
    keys = args.keys()
    total = 0
    for i in xrange(len(r[0])):
        total= total+r[0][i]
    avg = total/len(r[0])
    for i in xrange(len(r[0])):
        r[0][i] = avg-r[0][i];
    return r

