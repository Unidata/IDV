""" This is the doc for the GEMPAK Grid Diagnostics module """

GRAVTY = DerivedGridFactory.GRAVITY

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
  """ Coriolis Parameter for all points in a grid """
  return TWO_OMEGA*sin(latr(s))

def ddx(g):
  """ Take the derivative with respect to the domain's X coordinate """
  return DerivedGridFactory.ddx(g);

def ddy(g):
  """ Take the derivative with respect to the domain's Y coordinate """
  return DerivedGridFactory.ddy(g);

def div(u,v):
  """ Horizontal Divergence """
  return ddx(u) + ddy(v)

def jcbn(s1,s2):
  """ Jacobian Determinant """
  return ddx(s1)*ddy(s2) - ddy(s1)*ddx(s2)

def latr(s):
  """ Latitudue all points in a grid """
  return DerivedGridFactory.getLatitudeGrid(s)

def lav(s,top,bottom):
  """ Layer Average """
  return layerAverage(s,top,bottom);

def ldf(s,top,bottom):
  """ Layer Average """
  return layerDiff(s,top,bottom);

def mag(u,v):
  """ Magnitude of a vector """
  return DerivedGridFactory.createVectorMagnitude(u,v);

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

def vor(u,v):
  """ Relative Vorticity """
  return ddx(v)-ddy(u)

# Vector output
def geo(z):
  """  geostrophic wind from height """
  ug = newName(-GRAVTY*ddy(z), "UGEO", 0)
  vg = newName(GRAVTY*ddx(z), "VGEO", 0)
  return vecr(ug,vg)

def grad(s):
  """ Gradient """
  return vecr(ddx(s),ddy(s))
  
def vecr(s1,s2):
  """ Make a vector from two components """
  return makeVector(s1,s2)

def vldf(u,v,level1, level2):
  """ calculate the u and v layer difference and return as vector """
  return windShearVector(u, v, top, bottom)

