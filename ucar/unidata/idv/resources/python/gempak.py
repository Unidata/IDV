""" This is the doc for the GEMPAK Grid Diagnostics module """

GRAVTY = DerivedGridFactory.GRAVITY

# Math functions
def atn2(s1,s2):
  """ Wrapper for atan2 built-in """
  return atan2(s1,s2)

def add(s1,s2):
  """ Addition """
  return s1+s2

def mul(s1,s2):
  """ Multiply """
  return s1*s2

def quo(s1,s2):
  """ Multiply """
  return s1/s2

def sub(s1,s2):
  """ Multiply """
  return s1-s2

# Scalar quantities
def adv(p,u,v):
  """ Horizontal Advection, negative by convention """
  return -(u*ddx(p) + v*ddy(p))

def avg(s1,s2):
  """ Average of 2 scalars """
  return (s1+s2)/2;

def avor(u,v):
  """ Absolute Vorticity """
  relv = vor(u,v)
  return relv + corl(relv)

def corl(s):
  """ Coriolis Parameter for all points in a grid (TWO_OMEGA*sin(latr)"""
  return DerivedGridFactory.createCoriolisGrid(s)

def ddx(g):
  """ Take the derivative with respect to the domain's X coordinate """
  return DerivedGridFactory.ddx(g);

def ddy(g):
  """ Take the derivative with respect to the domain's Y coordinate """
  return DerivedGridFactory.ddy(g);

def div(u,v):
  """ Horizontal Divergence """
  return ddx(u) + ddy(v)

def dot(v1,v2):
  """ Dot product of two vectors (u1*u2+v1*v2)"""
  product = v1*v2
  return ur(product)+vr(product)

def jcbn(s1,s2):
  """ Jacobian Determinant """
  return ddx(s1)*ddy(s2) - ddy(s1)*ddx(s2)

def latr(s):
  """ Latitudue all points in a grid """
  return DerivedGridFactory.createLatitudeGrid(s)

def lav(s,top,bottom):
  """ Layer Average """
  return layerAverage(s,top,bottom);

def ldf(s,top,bottom):
  """ Layer Average """
  return layerDiff(s,top,bottom);

def mag(*a):
  """ Magnitude of a vector """
  if (len(a) == 1):
    return DerivedGridFactory.createVectorMagnitude(a[0]);
  else: 
    return DerivedGridFactory.createVectorMagnitude(a[0],a[1]);

def mixr(temp,rh):
  """ Mixing Ratio from Temperature, RH (requires pressure domain) """
  return DerivedGridFactory.createMixingRatio(temp,rh)

def sdiv(s,u,v):
  """ Horizontal Flux Divergence """
  return s*(div(u,v)) + (u*ddx(s) + v*ddy(s))

def shr(u,v):
  """ Shear Deformation """
  return ddx(v)+ddy(u)

def str(u,v):
  """ Stretching Deformation """
  return ddx(u)-ddy(v)

def thta(temp):
  """ Potential Temperature from Temperature (requires pressure domain) """
  return DerivedGridFactory.createPotentialTemperature(temp)

def thte(temp,rh):
  """ Equivalent Potential Temperature from Temperature and Relative
      humidity (requires pressure domain) """
  return DerivedGridFactory.createPotentialTemperature(temp,rh)

def un(vector):
  return DerivedGridFactory.getUComponent(DerivedGridFactory.createTrueFlowVector(vector))

def ur(vector):
  return DerivedGridFactory.getUComponent(vector)

def vn(vector):
  return DerivedGridFactory.getVComponent(DerivedGridFactory.createTrueFlowVector(vector))

def vr(vector):
  return DerivedGridFactory.getVComponent(vector)

def vor(u,v):
  """ Relative Vorticity """
  return ddx(v)-ddy(u)

# Vector output
def age(obs,geo):
  """  Ageostrophic wind """
  return obs-geo

def dvdx(v):
  """ Partial x derivative of a vector
      DVDX ( V ) = [ DDX (u), DDX (v) ] """
  return ddx(v)

def dvdy(v):
  """ Partial x derivative of a vector
      DVDY ( V ) = [ DDY (u), DDY (v) ] """
  return ddy(v)

def geo(z):
  """  geostrophic wind from height """
  return DerivedGridFactory.createGeostrophicWindVector(z)

def grad(s):
  """ Gradient of a scalar  
      GRAD ( S ) = [ DDX ( S ), DDY ( S ) ] """
  return vecr(ddx(s),ddy(s))
  
def inad(v1,v2):
  """ INAD  Inertial advective wind 
      INAD ( V1, V2 ) = [ DOT ( V1, GRAD (u2) ),
                          DOT ( V1, GRAD (v2) ) ] """
  return vecr(dot(v1,grad(ur(v2))),dot(v1,grad(vr(v2))))

def qvec(s,v):
  """ QVEC ( S, V ) = [ - ( DOT ( DVDX (V), GRAD (S) ) ),
                      - ( DOT ( DVDY (V), GRAD (S) ) ) ] 
                      where S can be any thermal paramenter, usually THTA. """
  grads = grad(s)
  qvecu = newName(-dot(dvdx(v),grads),"qvecu")
  qvecv = newName(-dot(dvdy(v),grads),"qvecv")
  return vecr(qvecu,qvecv)

def vecr(s1,s2):
  """ Make a vector from two components """
  return makeVector(s1,s2)

def vldf(u,v,level1, level2):
  """ calculate the u and v layer difference and return as vector """
  return windShearVector(u, v, top, bottom)

