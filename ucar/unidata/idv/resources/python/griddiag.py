""" 
   This is the doc for the Grid Diagnostics module.  These functions
   are based on the grid diagnostics from the General Meteorological 
   Package (GEMPAK).

   In the following operators, scalar operands are named Si and 
   vector operands arenamed Vi.  Lowercase u and v refer to the
   grid relative components of a vector.
"""

GRAVTY = DerivedGridFactory.GRAVITY

# Math functions
def atn2(S1,S2):
  """ Wrapper for atan2 built-in """
  return atan2(S1,S2)

def add(S1,S2):
  """ Addition """
  return S1+S2

def mul(S1,S2):
  """ Multiply """
  return S1*S2

def quo(S1,S2):
  """ Multiply """
  return S1/S2

def sub(S1,S2):
  """ Multiply """
  return S1-S2

# Scalar quantities
def adv(S,V):
  """ Horizontal Advection, negative by convention 
      ADV ( S, V ) = - ( u * DDX (S) + v * DDY (S) )  """
  return -add(mul(ur(V),ddx(S)),mul(vr(V),ddy(S)))

def avg(S1,S2):
  """ Average of 2 scalars """
  return quo(add(S1,S2),2);

def avor(V):
  """ Absolute Vorticity """
  relv = vor(V)
  return add(relv,corl(relv))

def corl(S):
  """ Coriolis Parameter for all points in a grid 
      CORL = TWO_OMEGA*sin(latr)                  """
  return DerivedGridFactory.createCoriolisGrid(S)

def cros(V1,V2):
  """ Vector cross product magnitude
      CROS ( V1, V2 ) = u1 * v2 - u2 * v1 """
  return sub(mul(ur(V1),vr(V2)),mul(ur(V2),vr(V1)))
 
def ddx(S):
  """ Take the derivative with respect to the domain's X coordinate """
  return DerivedGridFactory.ddx(S);

def ddy(S):
  """ Take the derivative with respect to the domain's Y coordinate """
  return DerivedGridFactory.ddy(S);

def defr(V):
  """ Total deformation  
      DEF ( V ) = ( STR (V) ** 2 + SHR (V) ** 2 ) ** .5 """
  return mag(str(V),shr(v))
  
def div(V):
  """ Horizontal Divergence 
      DIV ( V ) = DDX ( u ) + DDY ( v ) """
  return add(ddx(ur(V)),ddy(vr(V)))

def dot(V1,V2):
  """ Vector dot product
      DOT ( V1, V2 ) = u1 * u2 + v1 * v2 """
  product = mul(V1,V2)
  return add(ur(product),vr(product))

def jcbn(S1,S2):
  """ Jacobian Determinant 
      JCBN ( S1, S2 ) = DDX (S1) * DDY (S2) - DDY (S1) * DDX (S2)"""
  return sub(mul(ddx(S1),ddy(S2)),mul(ddy(S1),ddx(S2)))

def latr(S):
  """ Latitudue all points in a grid """
  return DerivedGridFactory.createLatitudeGrid(S)

def lap(S):
  """ Laplacian operator """
  grads = grad(S)
  return div(ur(grads),vr(grads))

def lav(S,top,bottom):
  """ Layer Average """
  return layerAverage(S,top,bottom);

def ldf(S,top,bottom):
  """ Layer Average """
  return layerDiff(S,top,bottom);

def mag(*a):
  """ Magnitude of a vector """
  if (len(a) == 1):
    return DerivedGridFactory.createVectorMagnitude(a[0]);
  else: 
    return DerivedGridFactory.createVectorMagnitude(a[0],a[1]);

def mixr(temp,rh):
  """ Mixing Ratio from Temperature, RH (requires pressure domain) """
  return DerivedGridFactory.createMixingRatio(temp,rh)

def sdiv(S,V):
  """ Horizontal Flux Divergence 
      SDIV ( S, V ) = S * DIV ( V ) + DOT ( V, GRAD ( S ) )"""
  return add(S*(div(V)) , dot(V,grad(S)))

def shr(V):
  """ Shear Deformation 
      SHR ( V ) = DDX ( v ) + DDY ( u ) """
  return add(ddx(vr(V)),ddy(ur(V)))

def str(V):
  """ Stretching Deformation 
      STR ( V ) = DDX ( u ) - DDY ( v ) """
  return sub(ddx(ur(V)),ddy(vr(V)))

def thta(temp):
  """ Potential Temperature from Temperature (requires pressure domain) """
  return DerivedGridFactory.createPotentialTemperature(temp)

def thte(temp,rh):
  """ Equivalent Potential Temperature from Temperature and Relative
      humidity (requires pressure domain) """
  return DerivedGridFactory.createPotentialTemperature(temp,rh)

def un(vector):
  """ North relative u component """
  return ur(DerivedGridFactory.createTrueFlowVector(vector))

def ur(vector):
  """ Grid relative u component """
  return DerivedGridFactory.getUComponent(vector)

def vn(vector):
  """ North relative v component """
  return vr(DerivedGridFactory.createTrueFlowVector(vector))

def vor(V):
  """ Relative Vorticity """
  return sub(ddx(vr(V)),ddy(ur(V)))

def vr(vector):
  """ Grid relative v component """
  return DerivedGridFactory.getVComponent(vector)

# Vector output
def age(obs,geo):
  """  Ageostrophic wind """
  return obs-geo

def dvdx(V):
  """ Partial x derivative of a vector
      DVDX ( V ) = [ DDX (u), DDX (v) ] """
  return ddx(V)

def dvdy(V):
  """ Partial x derivative of a vector
      DVDY ( V ) = [ DDY (u), DDY (v) ] """
  return ddy(V)

def geo(z):
  """  geostrophic wind from height """
  return DerivedGridFactory.createGeostrophicWindVector(z)

def grad(S):
  """ Gradient of a scalar  
      GRAD ( S ) = [ DDX ( S ), DDY ( S ) ] """
  return vecr(ddx(S),ddy(S))
  
def inad(V1,V2):
  """ INAD  Inertial advective wind 
      INAD ( V1, V2 ) = [ DOT ( V1, GRAD (u2) ),
                          DOT ( V1, GRAD (v2) ) ] """
  return vecr(dot(V1,grad(ur(V2))),dot(V1,grad(vr(V2))))

def qvec(S,V):
  """ QVEC ( S, V ) = [ - ( DOT ( DVDX (V), GRAD (S) ) ),
                      - ( DOT ( DVDY (V), GRAD (S) ) ) ] 
                      where S can be any thermal paramenter, usually THTA. """
  grads = grad(S)
  qvecu = newName(-dot(dvdx(V),grads),"qvecu")
  qvecv = newName(-dot(dvdy(V),grads),"qvecv")
  return vecr(qvecu,qvecv)

def vecr(S1,S2):
  """ Make a vector from two components """
  return makeVector(S1,S2)

def vldf(V,level1, level2):
  """ calculate the u and v layer difference and return as vector """
  return windShearVector(ur(V), vr(V), top, bottom)

