
def wrf_tk(P, PB, T):
  # calculate absolute temperature (K)
  # Rd / CP   = 0.28571 (dimensionless)
  # Pair      = P + PB
  # theta     = Tk * (100000./Pair)^(Rd/Cp)
  # --> Tk    = theta * (Pair / 100000.)^(R\/Cp)
  #------------------------------------
  Rd          = 287.0
  Cp          = 7.0*Rd / 2.0
  Rd_Cp       = Rd / Cp
  Pair        = noUnit(P + PB)
  theta       = noUnit(T) + 300.
  tk          = theta * (( Pair/100000. )**(Rd_Cp))
  tk0         = newUnit(tk, "temperature", "kelvin")
  return tk

def wrf_rh(T, QVAPOR, P, PB, flag=1 ):
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
  Tair      = wrf_tk(P, PB, T)
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
  rh        =  QV/QVS
  rh        =  substitute(rh, -10, 0, 0)
  rh        =  substitute(rh, 1, 10, 1)
  return rh