""" 
   This is the doc for the Grid Diagnostics module.  These functions
   are based on the grid diagnostics from the GEneral Meteorological 
   PAcKage (GEMPAK).  Note that the names are case sensitive and some
   are named slightly different from GEMPAK functions to avoid conflicts
   with Jython built-ins (e.g. str).
   <P>
   In the following operators, scalar operands are named S<sub>n</sub> and 
   vector operands are named V<sub>n</sub>.  Lowercase u and v refer to the
   grid relative components of a vector.
"""


def GRAVITY():
  """ Gravity constant """
  return DerivedGridFactory.GRAVITY;

# Math functions
def atn2(S1,S2,WA=0):
  """ Wrapper for atan2 built-in 
  <div class=jython>
      ATN2 (S1, S2) = ATAN ( S1 / S2 )<br>
      WA = use WEIGHTED_AVERAGE (default NEAREST_NEIGHBOR)
  </div>
  """
  return GridMath.atan2(S1,S2,WA)

def add(S1,S2,WA=0):
  """ Addition 
  <div class=jython>
      ADD  (S1, S2) = S1 + S2<br>
      WA = use WEIGHTED_AVERAGE (default NEAREST_NEIGHBOR)
  </div>
  """
  return GridMath.add(S1,S2,WA)

def mul(S1,S2,WA=0):
  """ Multiply 
  <div class=jython>
      MUL  (S1, S2) = S1 * S2<br>
      WA = use WEIGHTED_AVERAGE (default NEAREST_NEIGHBOR)
  </div>
  """
  return GridMath.multiply(S1,S2,WA)

def quo(S1,S2,WA=0):
  """ Divide 
  <div class=jython>
      QUO  (S1, S2) = S1 / S2<br>
      WA = use WEIGHTED_AVERAGE (default NEAREST_NEIGHBOR)
  </div>
  """
  return GridMath.divide(S1,S2,WA)

def sub(S1,S2,WA=0):
  """ Subtract 
  <div class=jython>
      SUB  (S1, S2) = S1 - S2<br>
      WA = use WEIGHTED_AVERAGE (default NEAREST_NEIGHBOR)
  </div>
  """
  return GridMath.subtract(S1,S2,WA)

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

def circs(S, D=2):
  """ 
  <div class=jython>
 Apply a circular aperature smoothing to the grid points.  The weighting 
 function is the circular aperature diffraction function.  D is
 the radius of influence in grid increments, increasing D increases 
 the smoothing. (default D=2)
  </div>
  """
  return GridUtil.smooth(S, "CIRC", int(D))

def corl(S):
  """ Coriolis Parameter for all points in a grid 
  <div class=jython>
      CORL = TWO_OMEGA*sin(latr)                  
  </div>
  """
  return DerivedGridFactory.createCoriolisGrid(S)

def cress(S, D=2):
  """ 
  <div class=jython>
 Apply a Cressman smoothing to the grid points.  The smoothed value
 is given by a weighted average of surrounding grid points.  D is
 the radius of influence in grid increments, 
 increasing D increases the smoothing. (default D=2)
  </div>
  """
  return GridUtil.smooth(S, "CRES", int(D))


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
  return GridMath.ddx(S);

def ddy(S):
  """ Take the derivative with respect to the domain's Y coordinate 
  """
  return GridMath.ddy(S);

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

def gwfs(S, N=6):
  """ 
  <div class=jython>
 Horizontal smoothing using normally distributed weights 
 with theoretical response of 1/e for N * delta-x wave.  
 Increasing N increases the smoothing. (default N=6)
  </div>
  """
  return GridUtil.smooth(S, "GWFS", int(N))

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
  return div(grads)

def lav(S,level1=None,level2=None, unit=None):
  """ Layer Average of a multi layer grid
  <div class=jython>
      LAV ( S ) = ( S (level1) + S (level2) ) / 2.
  </div>
  """
  if level1 == None:
     return GridMath.applyFunctionOverLevels(S, GridMath.FUNC_AVERAGE)
  else:
     return layerAverage(S,level1,level2, unit)

def ldf(S,level1,level2, unit=None):
  """ Layer Difference 
  <div class=jython>
      LDF ( S ) = S (level1) - S (level2)
  </div>
  """
  return layerDiff(S,level1,level2, unit);

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

def relh(temp,mixr):
  """ Create Relative Humidity from Temperature, mixing ratio (requires pressure domain) 
  """
  return DerivedGridFactory.createMixingRatio(temp,rh)

def pvor(S,V):
  """ Potetial Vorticity (usually from theta and wind)
  """
  return DerivedGridFactory.createPotentialVorticity(S,V)

def rects(S, D=2):
  """ 
  <div class=jython>
 Apply a rectangular aperature smoothing to the grid points.  The weighting 
 function is the product of the rectangular aperature diffraction function
 in the x and y directions.  D is the radius of influence in grid 
 increments, increasing D increases the smoothing. (default D=2)
  </div>
  """
  return GridUtil.smooth(S, "RECT", int(D))

def savg(S):
  """ Average over whole grid
  <div class=jython>
      SAVG ( S ) = average of all non-missing grid point values
  </div>
  """
  return GridMath.applyFunctionToLevels(S, GridMath.FUNC_AVERAGE)

def savs(S):
  """ Average over grid subset
  <div class=jython>
      SAVS ( S ) = average of all non-missing grid point values in the subset 
                   area
  </div>
  """
  return savg(S)

def sdiv(S,V):
  """ Horizontal Flux Divergence 
  <div class=jython>
      SDIV ( S, V ) = S * DIV ( V ) + DOT ( V, GRAD ( S ) )
  </div>
  """
  return add(mul(S,(div(V))) , dot(V,grad(S)))

def shr(V):
  """ Shear Deformation 
  <div class=jython>
      SHR ( V ) = DDX ( v ) + DDY ( u ) 
  </div>
  """
  return add(ddx(vr(V)),ddy(ur(V)))

def sm5s(S):
  """ Smooth a scalar grid using a 5-point smoother
  <div class=jython>
     SM5S ( S ) = .5 * S (i,j) + .125 * ( S (i+1,j) + S (i,j+1) +
                                          S (i-1,j) + S (i,j-1) ) 

  </div>
  """
  return GridUtil.smooth(S, "SM5S")

def sm9s(S):
  """ Smooth a scalar grid using a 9-point smoother
  <div class=jython>
      SM9S ( S ) = .25 * S (i,j) + .125 * ( S (i+1,j) + S (i,j+1) +
                                            S (i-1,j) + S (i,j-1) )
                                 + .0625 * ( S (i+1,j+1) +
                                             S (i+1,j-1) +
                                             S (i-1,j+1) +
                                             S (i-1,j-1) )

  </div>
  """
  return GridUtil.smooth(S, "SM9S")

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
  return DerivedGridFactory.createEquivalentPotentialTemperature(temp,rh)

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

def xav(S):
  """ Average along a grid row
  <div class=jython>
      XAV (S) = ( S (X1) + S (X2) + ... + S (KXD) ) / KNT
                KXD = number of points in row
                KNT = number of non-missing points in row
                XAV for a row is stored at every point in that row.
  </div>
  """
  return GridMath.applyFunctionToAxis(S, GridMath.FUNC_AVERAGE, GridMath.AXIS_X)

def xsum(S):
  """ Sum along a grid row
  <div class=jython>
      XSUM (S) = ( S (X1) + S (X2) + ... + S (KXD) )
                 KXD = number of points in row
                 XSUM for a row is stored at every point in that row.
  </div>
  """
  return GridMath.applyFunctionToAxis(S, GridMath.FUNC_SUM, GridMath.AXIS_X)

def yav(S):
  """ Average along a grid column
  <div class=jython>
      YAV (S) = ( S (Y1) + S (Y2) + ... + S (KYD) ) / KNT
                KYD = number of points in column
                KNT = number of non-missing points in column
  </div>
  """
  return GridMath.applyFunctionToAxis(S, GridMath.FUNC_AVERAGE, GridMath.AXIS_Y)

def ysum(S):
  """ Sum along a grid column
  <div class=jython>
      YSUM (S) = ( S (Y1) + S (Y2) + ... + S (KYD) )
                 KYD = number of points in row
                 YSUM for a column is stored at every point in that column.
  </div>
  """
  return GridMath.applyFunctionToAxis(S, GridMath.FUNC_SUM, GridMath.AXIS_Y)

def zav(S):
  """ Average across the levels of a grid at all points
  <div class=jython>
      ZAV (S) = ( S (Z1) + S (Z2) + ... + S (KZD) ) / KNT
                KZD = number of levels
                KNT = number of non-missing points in column
  </div>
  """
  return GridMath.applyFunctionToLevels(S, GridMath.FUNC_AVERAGE)

def zsum(S):
  """ Sum across the levels of a grid at all points
  <div class=jython>
      ZSUM (S) = ( S (Z1) + S (Z2) + ... + S (KZD) )
                 KYD = number of levels
                 ZSUM for a vertical column is stored at every point
  </div>
  """
  return GridMath.applyFunctionOverLevels(S, GridMath.FUNC_SUM)

def wshr(V, Z, top, bottom):
  """  Magnitude of the vertical wind shear in a layer
  <div class=jython>
      WSHR ( V ) = MAG [ VLDF (V) ] / LDF (Z)
  </div>
  """
  dv = mag(vldf(V,top,bottom))
  dz = ldf(Z,top,bottom)
  return quo(dv,dz)

# Vector output
def age(obs,geo):
  """  Ageostrophic wind 
  <div class=jython>
      AGE ( S ) = [ u (OBS) - u (GEO(S)), v (OBS) - v (GEO(S)) ]
  </div>
  """
  return sub(obs,geo)

def circv(S, D=2):
  """ 
  <div class=jython>
 Apply a circular aperature smoothing to the grid points.  The weighting 
 function is the circular aperature diffraction function.  D is
 the radius of influence in grid increments, increasing D increases 
 the smoothing. (default D=2)
  </div>
  """
  return GridUtil.smooth(S, "CIRC", int(D))

def cresv(S, D=2):
  """ 
  <div class=jython>
 Apply a Cressman smoothing to the grid points.  The smoothed value
 is given by a weighted average of surrounding grid points.  D is
 the radius of influence in grid increments, 
 increasing D increases the smoothing. (default D=2)
  </div>
  """
  return GridUtil.smooth(S, "CRES", int(D))


def dvdx(V):
  """ Partial x derivative of a vector
  <div class=jython>
      DVDX ( V ) = [ DDX (u), DDX (v) ] 
  </div>
  """
  return vecr(ddx(ur(V)), ddx(vr(V)))

def dvdy(V):
  """ Partial x derivative of a vector
  <div class=jython>
      DVDY ( V ) = [ DDY (u), DDY (v) ] 
  </div>
  """
  return vecr(ddy(ur(V)), ddy(vr(V)))

def frnt(S,V):
  """  Frontogenesis function from theta and the wind
  <div class=jython>
      FRNT ( THTA, V ) = 1/2 * MAG ( GRAD (THTA) ) *                   
                         ( DEF * COS (2 * BETA) - DIV )                <p>
                                                                        
                         Where: BETA = ASIN ( (-DDX (THTA) * COS (PSI) <br>
                                          - DDY (THTA) * SIN (PSI))/   <br>
                                       MAG ( GRAD (THTA) ) )           <br>
                                PSI  = 1/2 ATAN2 ( SHR / STR )         <br>
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
  
def gwfv(V, N=6):
  """ 
  <div class=jython>
 Horizontal smoothing using normally distributed weights 
 with theoretical response of 1/e for N * delta-x wave.  
 Increasing N increases the smoothing. (default N=6)
  </div>
  """
  return gwfs(V, N)

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

def qvcl(THTA,V):
  """ Q-vector ( K / m / s )
  <div class=jython>
      QVCL ( THTA, V ) = ( 1/( D (THTA) / DP ) ) *
                            [ ( DOT ( DVDX (V), GRAD (THTA) ) ),
                              ( DOT ( DVDY (V), GRAD (THTA) ) ) ]
  </div>
  """
  dtdp  = GridMath.partial(THTA,2)
  gradt = grad(THTA)
  qvecudp = newName(quo(dot(dvdx(V),gradt),dtdp),"qvecudp")
  qvecvdp = newName(quo(dot(dvdy(V),gradt),dtdp),"qvecvdp")
  return vecr(qvecudp,qvecvdp)

def rectv(S, D=2):
  """ 
  <div class=jython>
 Apply a rectangular aperature smoothing to the grid points.  The weighting 
 function is the product of the rectangular aperature diffraction function
 in the x and y directions.  D is the radius of influence in grid 
 increments, increasing D increases the smoothing. (default D=2)
  </div>
  """
  return GridUtil.smooth(S, "RECT", int(D))

def sm5v(V):
  """ Smooth a scalar grid using a 5-point smoother (see sm5s)
  """
  return sm5s(V)

def sm9v(V):
  """ Smooth a scalar grid using a 9-point smoother (see sm9s)
  """
  return sm9s(V)

def thrm(S, level1, level2, unit=None):
  """ Thermal wind 
  <div class=jython>
      THRM ( S ) = [ u (GEO(S)) (level1) - u (GEO(S)) (level2),	
                     v (GEO(S)) (level1) - v (GEO(S)) (level2) ] 
  </div>
  """
  return vldf(geo(S),level1,level2, unit)

def vadd(V1,V2):
  """ add the components of 2 vectors
  <div class=jython>
      VADD (V1, V2) = [ u1+u2, v1+v2 ] 
  </div>
  """
  return add(V1,V2)

def vecn(S1,S2):
  """ Make a true nort vector from two components 
  <div class=jython>
      VECN ( S1, S2 ) = [ S1, S2 ]
  </div>
  """
  return makeTrueVector(S1,S2)

def vecr(S1,S2):
  """ Make a vector from two components 
  <div class=jython>
      VECR ( S1, S2 ) = [ S1, S2 ]
  </div>
  """
  return makeVector(S1,S2)

def vlav(V,level1,level2, unit=None):
  """ calculate the vector layer average 
  <div class=jython>
      VLDF(V) = [(u(level1) - u(level2))/2,
                 (v(level1) - v(level2))/2] 
  </div>
  """
  return layerAverage(V, level1, level2, unit)
  
def vldf(V,level1,level2, unit=None):
  """ calculate the vector layer difference 
  <div class=jython>
      VLDF(V) = [u(level1) - u(level2),
                 v(level1) - v(level2)] 
  </div>
  """
  return layerDiff(V,level1,level2, unit)

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

