
def ddx(g):
  """ Take the derivative with respect to the domain's X coordinate """
  return DerivedGridFactory.ddx(g);

def ddy(g):
  """ Take the derivative with respect to the domain's Y coordinate """
  return DerivedGridFactory.ddy(g);

def div(u,v):
  """ Horizontal Divergence """
  return ddx(u) + ddy(v)

def fdiv(a,u,v):
  return u*ddx(a) + v*ddy(a) + a*(ddx(u)+ddy(v))

def adv(p,u,v):
  """ Horizontal Advection, negative by convention """
  return -(u*ddx(p) + v*ddy(p))

def vor(u,v):
  """ Relative Vorticity """
  return ddx(v)-ddy(u)

def avor(u,v):
  """ Absolute Vorticity """
  relv = vor(u,v)
  return relv + corl(relv)

def corl(a):
  """ Coriolis Parameter for all points in a grid """
  return TWO_OMEGA*sin(latr(a))

def latr(a):
  """ Latitudue all points in a grid """
  return DerivedGridFactory.getLatitudeGrid(a)

