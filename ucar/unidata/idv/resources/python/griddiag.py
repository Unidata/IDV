""" 
   This is the doc for the Grid Diagnostics module.  These functions
   are based on the grid diagnostics from the GEneral Meteorological 
   PAcKage (GEMPAK).  Note that the names are case sensitive and some
   are named slightly different from GEMPAK functions to avoid conflicts
   with Jython built-ins (e.g. str).
   <P>
   In the following operators, scalar operands are named Si and 
   vector operands are named Vi.  Lowercase u and v refer to the
   grid relative components of a vector.
"""


def GRAVITY():
  """ Gravity constant """
  return DerivedGridFactory.GRAVITY;

# Math functions
def atn2(S1,S2):
  """ Wrapper for atan2 built-in 
  <div class=jython>
      ATN2 (S1, S2) = ATAN ( S1 / S2 )
  </div>
  """
  return GridMath.atan2(S1,S2)

def add(S1,S2):
  """ Addition 
  <div class=jython>
      ADD  (S1, S2) = S1 + S2
  </div>
  """
  return GridMath.add(S1,S2)

def mul(S1,S2):
  """ Multiply 
  <div class=jython>
      MUL  (S1, S2) = S1 * S2
  </div>
  """
  return GridMath.multiply(S1,S2)

def quo(S1,S2):
  """ Divide 
  <div class=jython>
      QUO  (S1, S2) = S1 / S2
  </div>
  """
  return GridMath.divide(S1,S2)

def sub(S1,S2):
  """ Subtract 
  <div class=jython>
      SUB  (S1, S2) = S1 - S2
  </div>
  """
  return GridMath.subtract(S1,S2)

# Scalar quantities
def adv(S,V):
  """ Horizontal Advection, negative by convention 
  <div class=jython>
      ADV ( S, V ) = - ( u * DDX (S) + v * DDY (S) )  
  </div>
  """
  return -add(mul(ur(V),ddx(S)),mul(vr(V),ddy(S)))

def avg(S1,S2):
  """ Average of 2 scalars 
  <div class=jython>
      AVG  (S1, S2) = ( S1 + S2 ) / 2
  </div>
  """
  return add(S1,S2)/2

def avor(V):
  """ Absolute Vorticity 
  <div class=jython>
      AVOR ( V ) = VOR ( V ) + CORL(V)
  </div>
  """
  relv = vor(V)
  return add(relv,corl(relv))

def corl(S):
  """ Coriolis Parameter for all points in a grid 
  <div class=jython>
      CORL = TWO_OMEGA*sin(latr)                  
  </div>
  """
  return DerivedGridFactory.createCoriolisGrid(S)

def cros(V1,V2):
  """ Vector cross product magnitude
  <div class=jython>
      CROS ( V1, V2 ) = u1 * v2 - u2 * v1 
  </div>
  """
  return sub(mul(ur(V1),vr(V2)),mul(ur(V2),vr(V1)))
 
def ddx(S):
  """ Take the derivative with respect to the domain's X coordinate 
  """
  return DerivedGridFactory.ddx(S);

def ddy(S):
  """ Take the derivative with respect to the domain's Y coordinate 
  """
  return DerivedGridFactory.ddy(S);

def defr(V):
  """ Total deformation  
  <div class=jython>
      DEF ( V ) = ( STRD (V) ** 2 + SHR (V) ** 2 ) ** .5 
  </div>
  """
  return mag(strd(V),shr(V))
  
def div(V):
  """ Horizontal Divergence 
  <div class=jython>
      DIV ( V ) = DDX ( u ) + DDY ( v ) 
  </div>
  """
  return add(ddx(ur(V)),ddy(vr(V)))

def dirn(V):
  """ North relative direction of a vector
  <div class=jython>
      DIRN ( V ) = DIRR ( un(v), vn(v) )
  </div>
  """
  return dirr(DerivedGridFactory.createTrueFlowVector(V))

def dirr(V):
  """ Grid relative direction of a vector
  """
  return DerivedGridFactory.createVectorDirection(V)

def dot(V1,V2):
  """ Vector dot product
  <div class=jython>
      DOT ( V1, V2 ) = u1 * u2 + v1 * v2 
  </div>
  """
  product = mul(V1,V2)
  return add(ur(product),vr(product))

def jcbn(S1,S2):
  """ Jacobian Determinant 
  <div class=jython>
      JCBN ( S1, S2 ) = DDX (S1) * DDY (S2) - DDY (S1) * DDX (S2)
  </div>
  """
  return sub(mul(ddx(S1),ddy(S2)),mul(ddy(S1),ddx(S2)))

def latr(S):
  """ Latitudue all points in a grid 
  """
  return DerivedGridFactory.createLatitudeGrid(S)

def lap(S):
  """ Laplacian operator 
  <div class=jython>
      LAP ( S ) = DIV ( GRAD (S) )
  </div>
  """
  grads = grad(S)
  return div(ur(grads),vr(grads))

def lav(S,level1,level2):
  """ Layer Average 
  <div class=jython>
      LAV ( S ) = ( S (level1) + S (level2) ) / 2.
  </div>
  """
  return layerAverage(S,level1,level2);

def ldf(S,level1,level2):
  """ Layer Average 
  <div class=jython>
      LDF ( S ) = S (level1) - S (level2)
  </div>
  """
  return layerDiff(S,level1,level2);

def mag(*a):
  """ Magnitude of a vector 
  """
  if (len(a) == 1):
    return DerivedGridFactory.createVectorMagnitude(a[0]);
  else: 
    return DerivedGridFactory.createVectorMagnitude(a[0],a[1]);

def mixr(temp,rh):
  """ Mixing Ratio from Temperature, RH (requires pressure domain) 
  """
  return DerivedGridFactory.createMixingRatio(temp,rh)

def sdiv(S,V):
  """ Horizontal Flux Divergence 
  <div class=jython>
      SDIV ( S, V ) = S * DIV ( V ) + DOT ( V, GRAD ( S ) )
  </div>
  """
  return add(S*(div(V)) , dot(V,grad(S)))

def shr(V):
  """ Shear Deformation 
  <div class=jython>
      SHR ( V ) = DDX ( v ) + DDY ( u ) 
  </div>
  """
  return add(ddx(vr(V)),ddy(ur(V)))

def strd(V):
  """ Stretching Deformation 
  <div class=jython>
      STRD ( V ) = DDX ( u ) - DDY ( v ) 
  </div>
  """
  return sub(ddx(ur(V)),ddy(vr(V)))

def thta(temp):
  """ Potential Temperature from Temperature (requires pressure domain) 
  """
  return DerivedGridFactory.createPotentialTemperature(temp)

def thte(temp,rh):
  """ Equivalent Potential Temperature from Temperature and Relative
      humidity (requires pressure domain) 
  """
  return DerivedGridFactory.createPotentialTemperature(temp,rh)

def un(V):
  """ North relative u component 
  """
  return ur(DerivedGridFactory.createTrueFlowVector(V))

def ur(V):
  """ Grid relative u component 
  """
  return DerivedGridFactory.getUComponent(V)

def vn(V):
  """ North relative v component 
  """
  return vr(DerivedGridFactory.createTrueFlowVector(V))

def vor(V):
  """ Relative Vorticity 
  <div class=jython>
      VOR ( V ) = DDX ( v ) - DDY ( u )
  </div>
  """
  return sub(ddx(vr(V)),ddy(ur(V)))

def vr(V):
  """ Grid relative v component 
  """
  return DerivedGridFactory.getVComponent(V)

def wshr(V, Z, top, bottom):
  """  Magnitude of the vertical wind shear in a layer
  <div class=jython>
      WSHR ( V ) = MAG [ VLDF (V) ] / LDF (Z)
  </div>
  """
  dv = mag(vldf(V,top,bottom))
  dz = ldf(Z,top,bottom)
  return dv/dz

# Vector output
def age(obs,geo):
  """  Ageostrophic wind 
  <div class=jython>
      AGE ( S ) = [ u (OBS) - u (GEO(S)), v (OBS) - v (GEO(S)) ]
  </div>
  """
  return obs-geo

def dvdx(V):
  """ Partial x derivative of a vector
  <div class=jython>
      DVDX ( V ) = [ DDX (u), DDX (v) ] 
  </div>
  """
  return ddx(V)

def dvdy(V):
  """ Partial x derivative of a vector
  <div class=jython>
      DVDY ( V ) = [ DDY (u), DDY (v) ] 
  </div>
  """
  return ddy(V)

def frnt(S,V):
  """  Frontogenesis function from theta and the wind
  <div class=jython>
      FRNT ( THTA, V ) = 1/2 * MAG ( GRAD (THTA) ) *
                         ( DEF * COS (2 * BETA) - DIV )
 
                         Where: BETA = ASIN ( (-DDX (THTA) * COS (PSI)
                                          - DDY (THTA) * SIN (PSI))/
                                       MAG ( GRAD (THTA) ) )
                                PSI  = 1/2 ATAN2 ( SHR / STR )
  </div>
  """
  shear = shr(V)
  strch = strd(V)
  psi = .5*atn2(shear,strch)
  dxt = ddx(S)
  dyt = ddy(S)
  cosd = cos(psi)
  sind = sin(psi)
  gradt = grad(S)
  mgradt = mag(gradt)
  a = -cosd*dxt-sind*dyt
  beta = asin(a/mgradt)
  frnto = .5*mgradt*(defr(V)*cos(2*beta)-div(V))
  return frnto

def geo(z):
  """  geostrophic wind from height 
  <div class=jython>
      GEO ( S )  = [ - DDY (S) * const / CORL, DDX (S) * const / CORL ]
  </div>
  """
  return DerivedGridFactory.createGeostrophicWindVector(z)

def grad(S):
  """ Gradient of a scalar  
  <div class=jython>
      GRAD ( S ) = [ DDX ( S ), DDY ( S ) ] 
  </div>
  """
  return vecr(ddx(S),ddy(S))
  
def inad(V1,V2):
  """ Inertial advective wind 
  <div class=jython>
      INAD ( V1, V2 ) = [ DOT ( V1, GRAD (u2) ),
                          DOT ( V1, GRAD (v2) ) ] 
  </div>
  """
  return vecr(dot(V1,grad(ur(V2))),dot(V1,grad(vr(V2))))

def qvec(S,V):
  """ Q-vector at a level ( K / m / s )
  <div class=jython>
      QVEC ( S, V ) = [ - ( DOT ( DVDX (V), GRAD (S) ) ),
                      - ( DOT ( DVDY (V), GRAD (S) ) ) ] 
                      where S can be any thermal paramenter, usually THTA. 
  </div>
  """
  grads = grad(S)
  qvecu = newName(-dot(dvdx(V),grads),"qvecu")
  qvecv = newName(-dot(dvdy(V),grads),"qvecv")
  return vecr(qvecu,qvecv)

def thrm(S, level1, level2):
  """ Thermal wind 
  <div class=jython>
      THRM ( S ) = [ u (GEO(S)) (level1) - u (GEO(S)) (level2),	
                     v (GEO(S)) (level1) - v (GEO(S)) (level2) ] 
  </div>
  """
  return vldf(geo(S),level1,level2)

def vadd(V1,V2):
  """ add the components of 2 vectors
  <div class=jython>
      VADD (V1, V2) = [ u1+u2, v1+v2 ] 
  </div>
  """
  return add(V1,V2)

def vecr(S1,S2):
  """ Make a vector from two components 
  <div class=jython>
      VECR ( S1, S2 ) = [ S1, S2 ]
  </div>
  """
  return makeVector(S1,S2)

def vlav(V,level1,level2):
  """ calculate the vector layer average 
  <div class=jython>
      VLDF(V) = [(u(level1) - u(level2))/2,
                 (v(level1) - v(level2))/2] 
  </div>
  """
  return layerAverage(V, level1, level2)
  
def vldf(V,level1,level2):
  """ calculate the vector layer difference 
  <div class=jython>
      VLDF(V) = [u(level1) - u(level2),
                 v(level1) - v(level2)] 
  </div>
  """
  return layerDiff(V,level1,level2)

def vmul(V1,V2):
  """ Multiply the components of 2 vectors
  <div class=jython>
      VMUL (V1, V2) = [ u1*u2, v1*v2 ] 
  </div>
  """
  return mul(V1,V2)

def vquo(V1,V2):
  """ Divide the components of 2 vectors
  <div class=jython>
      VQUO (V1, V2) = [ u1/u2, v1/v2 ] 
  </div>
  """
  return quo(V1,V2)

def vsub(V1,V2):
  """ subtract the components of 2 vectors
  <div class=jython>
      VSUB (V1, V2) = [ u1-u2, v1-v2 ] 
  </div>
  """
  return sub(V1,V2)

