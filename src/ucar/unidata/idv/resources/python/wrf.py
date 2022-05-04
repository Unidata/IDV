
def wrf_rh0(T, QVAPOR, P, PB, flag=1 ):
  # calculate Relative humidity (%) with respect to liquid water
  # this function use "Tetens formula"
  #------------------------------------
  # input variables
  # - T: perturbationj potential temperature (theta-t0)";
  # - QVAPOR: Water vapor mixing ratio (kg kg-1)
  # - P: perturbation pressure
  # - PB: base state pressure
  # # es is calculated with respect to : flag=1 water, flag=-1 ice, flag=0 mix
  #------------------------------------
  # epsi    = 0.622
  # Rd / CP = 0.28571 (dimensionless)
  # Pair    = P + PB   # (Pa)
  # theta   = T + 300  # (K)
  # Tair    = theta * ( Pair /100000.)**(Rd/Cp)    # (K)
  # e_sat   = 0.01 * 6.1078 * 10^( a * Tdeg / (b + Tdeg) )  # Teten's formula
  # q       = QVAPOR   # (kg kg-1), water vapor mixing ratio
  # e       = q * Pair / (q + epsi)
  #------------------------------------
  # rh      = e / e_sat * 100.
  #------------------------------------
  EP_3      = 0.622
  QV        = QVAPOR
  Pair      = P  +  PB
  Tair      = T + 300
  #-- calc es -----------------------------
  a_liq = 7.5
  b_liq = 237.3
  a_ice = 9.5
  b_ice = 265.3
  rTliq = 273.15  #   0 deg.C
  rTice = 250.15  # -23 deg.C
  #
  Tdeg     =  Tair- 273.16
  #
  if (flag == 1):
    es = 100. * 6.1078 * 10.0**(a_liq * Tdeg/ (b_liq + Tdeg))
  elif (flag == -1):
    es = 100. * 6.1078 * 10.0**(a_ice * Tdeg/ (b_ice + Tdeg))
  elif (flag == 0):
    if ( Tdeg >= Tdegliq):
      es = 100. * 6.1078 * 10.0**(a_liq * Tdeg/ (b_liq + Tdeg))
    elif ( Tdeg <= Tdegice ):
      es = 100. * 6.1078 * 10.0**(a_ice * Tdeg/ (b_ice + Tdeg))
    else:
      es_liq = 100.* 6.1078 * 10.0**(a_liq * Tdeg/ (b_liq + Tdeg))
      es_ice = 100.* 6.1078 * 10.0**(a_ice * Tdeg/ (b_ice + Tdeg))
      es = ((Tdeg - Tdegice)*es_liq + (Tdegliq - Tdeg)*es_ice)/(Tdegliq - Tdegice)
  #----------------------------------------
  QVS       = EP_3 * es / (Pair - es)     # [kg kg-1]
  rh        =  noUnit(QV)/noUnit(QVS) * 100
  rh        =  substitute(rh, -100, 0, 0)
  rh        =  substitute(rh, 100, 1000, 100)
  rh        =  newUnit(rh, "rh", "%")
  return rh

def wrf_es(T, flag=1):
    # calculate saturation vapor pressure(Es)
    #  T is temperature (K)
    #------------------------------------
  if flag == 1:
    a      = 7.5
    b      = 237.3
  elif flag == -1:
    a      = 9.5
    b      = 265.5
    #---
  tdeg   = T - 273.16
  es = 6.11 * 10**(a * tdeg / (b + tdeg)) * 100.0
  return es

def wrf_rh(T, QVAPOR, P, PB, flag=1 ):
  temp = T + 300
  press = P + PB
  rh = DerivedGridFactory.createRelativeHumidity(temp, press, QVAPOR, 0)
  return rh

def wrf_qs(P, PB, T, flag=1):
  # calculate Mixing Ratio
  #  qs (kg kg-1)
  #  p  (Pa)
  #  T  (K)
  Pair      = P  +  PB
  epsi   = 0.62185
  es     = wrf_es(T, flag)
  qs     = 0.62185 * es / (Pair - es)
  return qs

def wrf_dewpoint(T, QVAPOR, P, PB, flag=1 ):
  # calculate dewpoint temperature
  # this function use "Tetens formula"
  #------------------------------------
  # input variables
  # - T: perturbationj potential temperature (theta-t0)";
  # - QVAPOR: Water vapor mixing ratio (kg kg-1)
  # - P: perturbation pressure
  # - PB: base state pressure
  # # es is calculated with respect to : flag=1 water, flag=-1 ice, flag=0 mix
  #------------------------------------
  temp = T + 300
  press = P + PB
  rh = DerivedGridFactory.createRelativeHumidity(temp, press, QVAPOR,0)
  return DerivedGridFactory.createDewpoint (temp, rh)

def wrf_theta(P, PB, T):
  # calculate potential temperature (K)
  # T is perturbation temperature
  # Rd / CP   = 0.28571 (dimensionless)
  # Pair      = P + PB
  # theta     = Tk * (100000./Pair)^(Rd/Cp)
  #------------------------------------
  temp = T + 300
  press = P + PB
  theta = DerivedGridFactory.createPotentialTemperature(temp, press)
  return theta

def wrf_thetaE(P, PB, T, QVAPOR):
  temp = T + 300
  press = P + PB
  rh = DerivedGridFactory.createRelativeHumidity(temp, press, QVAPOR,0)
  thetaE = DerivedGridFactory.createEquivalentPotentialTemperature(temp, press, rh)
  return thetaE