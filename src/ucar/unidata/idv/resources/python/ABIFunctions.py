# A collection of functions to create displays of different ABI RGB
# products and band subtractions.

# abitrucol is derived from McIDAS-X's ABITRUCOL function
# which creates an ABI RGB by deriving a green band
# McIDAS-X's ABITRUCOL is based on the CIMSS Natural True Color method
# http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_CIMSSRGB_v2.pdf
def ABITruColRGB(red, grn, blu):
    # red = band 2
    # grn = band 3
    # blu = band 1

    # multiply bands by coefficient and add together
    # to make corrected RGB
    redCoef = red * 0.45
    grnCoef = grn * 0.1
    bluCoef = blu * 0.45

    combGrn = redCoef + grnCoef + bluCoef

    # mask values greater than those passed through the first
    # rescale below.  If this isn't done, then anything outside
    # of the range set by the first rescale will be set to the
    # max value of the rescaling (10)
    redMasked = mask(red, '<', 33, 0) * red
    combGrnMasked = mask(combGrn, '<', 40, 0) * combGrn
    bluMasked = mask(blu, '<', 50, 0) * blu

    # first rescale for the lower end values
    redScaled1 = rescale(redMasked, 0, 33, 0, 10)
    grnScaled1 = rescale(combGrnMasked, 0, 40, 0, 10)
    bluScaled1 = rescale(bluMasked, 0, 50, 0, 10)

    # second rescale for higher end values
    redScaled2 = rescale(red, 33, 255, 10, 255)
    grnScaled2 = rescale(combGrn, 40, 255, 10, 255)
    bluScaled2 = rescale(blu, 50, 255, 10, 255)

    # sum the two rescaled objects together
    final_red = redScaled1 + redScaled2
    final_grn = grnScaled1 + grnScaled2
    final_blu = bluScaled1 + bluScaled2

    # create RGB object
    rgb = combineRGB(final_red, final_grn, final_blu)
    return rgb

# The functions below were created using the Quick Guides
# linked from CIRA's VISIT Quick Guides page:
# https://rammb2.cira.colostate.edu/training/visit/quick_reference/#tab17
# Information about each RGB/band subtraction below can be
# found on the above webpage.  Note that these RGBs and band
# subtractions were submitted by a variety of sources, all of
# which are referenced on each individual product's page.

# Some of these functions include a line containing the "resampleGrid"
# function.  This is done to resample the resolution of the red band
# to the highest resolution band being passed through the composite.
# The reason for this is that combineRGB returns a data object that
# is the resolution of the red band.  Doing this resampleGrid allows
# for a higher resolution display than would otherwise be available.

# ABI Airmass RGB
def ABIAirmassRGB(b8T, b10T, b12T, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_AirMassRGB_final-1.pdf
    # red = band8 - band10; -26.2C to 0.6C rescaled to 0 to 255
    # grn = band12 - band13; -43.2C to 6.7C rescaled to 0 to 255
    # blu = band8 inverted; 243.65K to 208.53K rescaled to 0 to 255
    red = rescale(b8T-b10T, -26.2, 0.6, 0, 255)
    grn = rescale(b12T-b13T, -43.2, 6.7, 0, 255)
    blu = rescale(b8T, 243.65, 208.53, 0, 255)
    return combineRGB(red, grn, blu)

# ABI SO2 RGB
def ABISo2RGB(b9T, b10T, b11T, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/Quick_Guide_SO2_RGB-1.pdf
    # red = band9 - band10; -4C to 2C rescaled to 0 to 255
    # grn = band13 - band11; -4C to 5C rescaled to 0 to 255
    # blu = band13; 243.05K to 302.95K rescaled to 0 to 255
    red = rescale(b9T-b10T, -4, 2, 0, 255)
    grn = rescale(b13T-b11T, -4, 5, 0, 255)
    blu = rescale(b13T, 243.05, 302.95, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Cloud Phase Distinction RGB
def ABIDayCloudPhaseRGB(b2A, b5A, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_DayCloudPhaseDistinction_final_v2.pdf
    # red = band 13; 280.65K to 219.65K rescaled to 0 to 255
    # grn = band 2; 0% to 78% rescaled to 0 to 255
    # blu = band 5; 1% to 59% rescaled to 0 to 255
    time_steps = b13T.getLength()
    for i in range(time_steps):
       b13T.setSample(i, resampleGrid(b13T[i], b2A[i]))
    red = rescale(b13T, 280.65, 219.65, 0, 255)
    grn = rescale(b2A, 0, 78, 0, 255)
    blu = rescale(b5A, 1, 59, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Ash RGB
def ABIAshRGB(b11T, b13T, b14T, b15T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/GOES_Ash_RGB-1.pdf
    # red = band15 - band13; -6.7C to 2.6C rescaled to 0 to 255
    # grn = band14 - band11; -6.0C to 6.3C rescaled to 0 to 255
    # blu = band13; 243.6K to 302.4K rescaled to 0 to 255
    red = rescale(b15T-b13T, -6.7, 2.6, 0, 255)
    grn = rescale(b14T-b11T, -6.0, 6.3, 0, 255)
    blu = rescale(b13T, 243.6, 302.4, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Land Cloud RGB
def ABIDayLandCloudRGB(b2A, b3A, b5A):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_daylandcloudRGB_final-1.pdf
    # red = band5; 0% to 97.5% rescaled to 0 to 255
    # grn = band3; 0% to 108.6% rescaled to 0 to 255
    # blu = band2; 0% to 100% rescaled to 0 to 255
    time_steps = b5A.getLength()
    for i in range(time_steps):
       b5A.setSample(i, resampleGrid(b5A[i], b2A[i]))
    red = rescale(b5A, 0, 97.5, 0, 255)
    grn = rescale(b3A, 0, 108.6, 0, 255)
    blu = rescale(b2A, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Fire RGB
def ABIDayFireRGB(b2A, b3A, b6A):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_DayLandCloudFireRGB_final-1.pdf
    # red = band6; 0% to 100% rescaled to 0 to 255
    # grn = band3; 0% to 100% rescaled to 0 to 255
    # blu = band2; 0% to 100% rescaled to 0 to 255
    time_steps = b6A.getLength()
    for i in range(time_steps):
       b6A.setSample(i, resampleGrid(b6A[i], b2A[i]))
    red = rescale(b6A, 0, 100, 0, 255)
    grn = rescale(b3A, 0, 100, 0, 255)
    blu = rescale(b2A, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Night-time Microphysics RGB
def ABINightMicrophysicsRGB(b7T, b13T, b15T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_NtMicroRGB_Final_20191206-1.pdf
    # red = band15 - band13; -6.7C to 2.6C rescaled to 0 to 255
    # grn = band13 - band7; -3.1C to 5.2C rescaled to 0 to 255
    # blu = band13; 243.55K to 292.65K rescaled to 0 to 255
    red = rescale(b15T-b13T, -6.7, 2.6, 0, 255)
    grn = rescale(b13T-b7T, -3.1, 5.2, 0, 255)
    blu = rescale(b13T, 243.55, 292.65, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Simple Water Vapor RGB
def ABISimpleWaterVaporRGB(b8T, b10T, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/Simple_Water_Vapor_RGB-1.pdf
    # red = band13 inverted; 278.96K to 202.29K rescaled to 0 to 255
    # grn = band8; 242.67K to 214.66K rescaled to 0 to 255
    # blu = band10; 261.03K to 245.12K rescaled to 0 to 255
    red = rescale(b13T, 278.96, 202.29, 0, 255)
    grn = rescale(b8T, 242.67, 214.66, 0, 255)
    blu = rescale(b10T, 261.03, 245.12, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Day Snow Fog RGB
def ABIDaySnowFogRGB(b3A, b5A, b7T, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_DaySnowFogRGB_final_v2.pdf
    # red = band3; 0% to 100% rescaled to 0 to 255; gamma 1.7
    # grn = band5; 0% to 70% rescaled to 0 to 255; gamma 1.7
    # blu = band7 - band13; 0C to 30C rescaled to 0 to 255; gamma 1.7
    red = 255*(rescale(b3A, 0, 100, 0, 1)**(1/1.7))
    grn = 255*(rescale(b5A, 0, 70, 0, 1)**(1/1.7))
    blu = 255*(rescale(b7T-b13T, 0, 30, 0, 1)**(1/1.7))
    return combineRGB(red, grn, blu)

# ABI Day Cloud Convection RGB
def ABIDayCloudConvectionRGB(b2A, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_DayCloudConvectionRGB_final-1.pdf
    # red = band2; 0% to 100% rescaled to 0 to 255; gamma 1.7
    # grn = band2; 0% to 100% rescaled to 0 to 255; gamma 1.7
    # blu = band13; 323K to 203K rescaled to 0 to 255; gamma 1.0
    red = 255*(rescale(b2A, 0, 100, 0, 1)**(1/1.7))
    grn = 255*(rescale(b2A, 0, 100, 0, 1)**(1/1.7))
    blu = rescale(b13T, 323, 203, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Fire Temperature RGB
def ABIFireTemperatureRGB(b5A, b6A, b7T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/Fire_Temperature_RGB-1.pdf
    # red = band7; 0C to 60C rescaled to 0 to 255; gamma 0.4
    # grn = band6; 0% to 100% rescaled to 0 to 255; gamma 1.0
    # blu = band5; 0% to 75% rescaled to 0 to 255; gamma 1.0
    time_steps = b7T.getLength()
    for i in range(time_steps):
       b7T.setSample(i, resampleGrid(b7T[i], b5A[i]))
    red = 255*(rescale(b7T, 273.15, 333.15, 0, 1)**(1/.4))
    grn = rescale(b6A, 0, 100, 0, 255)
    blu = rescale(b5A, 0, 75, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Dust RGB
def ABIDustRGB(b11T, b13T, b14T, b15T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/Dust_RGB_Quick_Guide-1.pdf
    # red = band15 - band13; -6.7C to 2.6C rescaled to 0 to 255; gamma 1.0
    # grn = band14 - band11; -0.5C to 20.0C rescaled to 0 to 255; gamma 2.5
    # blu = band13; -11.95C to 15.55C rescaled to 0 to 255; gamma 1.0
    red = rescale(b15T-b13T, -6.7, 2.6, 0, 255)
    grn = 255*(rescale(b14T-b11T, -0.5, 20, 0, 1)**(1/2.5))
    blu = rescale(b13T, 261.2, 288.7, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Differential Water Vapor RGB
def ABIDifferentialWaterVaporRGB(b8T, b10T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_DifferentialWaterVaporRGB_final-1.pdf
    # red = band10 - band8 inverted; 30C to -3C rescaled to 0 to 255; gamma 0.2587
    # grn = band10 inverted; 5C to -60C rescaled to 0 to 255; gamma 0.4
    # blu = band8 inverted; -29.25C to -64.65C rescaled to 0 to 255; gamma 0.4
    red = 255*(rescale(b10T-b8T, 30, -3, 0, 1)**(1/.2587))
    grn = 255*(rescale(b10T, 278.15, 213.15, 0, 1)**(1/.4))
    blu = 255*(rescale(b8T, 243.9, 208.5, 0, 1)**(1/.4))
    return combineRGB(red, grn, blu)

# ABI Day Convection RGB
def ABIDayConvectionRGB(b2A, b5A, b7T, b8T, b10T, b13T):
    # https://rammb2.cira.colostate.edu/wp-content/uploads/2020/01/QuickGuide_GOESR_DayConvectionRGB_final-1.pdf
    # red = band8 - band10; -35C to 5C rescaled to 0 to 255; gamma 1.0
    # grn = band7 - band13; -5C to 60C rescaled to 0 to 255; gamma 1.0
    # blu = band5 - band2; -0.75% to 0.25% rescaled to 0 to 255; gamma 1.0
    time_steps = b8T.getLength()
    for i in range(time_steps):
       b8T.setSample(i, resampleGrid(b8T[i], b2A[i]))
    for i in range(time_steps):
       b10T.setSample(i, resampleGrid(b10T[i], b2A[i]))
    red = rescale(b8T-b10T, -35, 5, 0, 255)
    grn = rescale(b7T-b13T, -5, 60, 0, 255)
    blu = rescale(b5A-b2A, -0.75, 0.25, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Cloud Type RGB
def ABICloudTypeRGB(b4A, b2A, b5A):
    # red = band 4; 0% to 10% rescaled to 0 to 255; gamma 2.5
    # grn = band 2; 0% to 50% rescaled to 0 to 255; gamma 1.4
    # blu = band 5; 0% to 50% rescaled to 0 to 255; gamma 1.5
    time_steps = b4A.getLength()
    for i in range(time_steps):
       b4A.setSample(i, resampleGrid(b4A[i], b2A[i]))
    red = 255*(rescale(b4A, 0, 10, 0, 1)**(1/2.5))
    grn = 255*(rescale(b2A, 0, 50, 0, 1)**(1/1.4))
    blu = 255*(rescale(b5A, 0, 50, 0, 1)**(1/1.5))
    return combineRGB(red, grn, blu)

# ABI Day Rocket Plume RGB
def ABIDayRocketRGB(b2A, b7T, b8T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/QuickGuide_Template_GOESRBanner_Plume_day.pdf
    # red = band7; 273K to 338K rescaled to 0 to 255
    # grn = band8; 233K to 253K rescaled to 0 to 255
    # blu = band2; 0% to 80% rescaled to 0 to 255
    time_steps = b7T.getLength()
    for i in range(time_steps):
       b7T.setSample(i, resampleGrid(b7T[i], b2A[i]))
    red = rescale(b7T, 273, 338, 0, 255)
    grn = rescale(b8T, 233, 253, 0, 255)
    blu = rescale(b2A, 0, 80, 0, 255)
    return combineRGB(red, grn, blu)

# ABI Night Rocket Plume RGB
def ABINightRocketRGB(b7T, b8T, b10T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/QuickGuide_Template_GOESRBanner_Plume_night.pdf
    # red = band7; 273K to 338K rescaled to 0 to 255
    # grn = band8; 220K to 280K rescaled to 0 to 255
    # blu = band10; 230K to 290K rescaled to 0 to 255
    red = rescale(b7T, 273, 338, 0, 255)
    grn = rescale(b8T, 220, 280, 0, 255)
    blu = rescale(b10T, 230, 290, 0, 255)
    return combineRGB(red, grn, blu)

# Split Ozone Channel Difference
def ABIOzoneDifference(b12T, b13T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_SplitOzoneDiff.pdf
    # band12 temperature - band13 temperature
    return sub(b12T, b13T)

# Split Water Vapor Channel Difference
def ABISplitWaterVaporDifference(b8T, b10T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_SplitWV_BTDiffv2.pdf
    # band8 temperature - band10 temperature
    return sub(b8T, b10T)

# Split Snow Channel Difference
def ABISplitSnowDifference(b5R, b2R):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_SplitSnowv2.pdf
    # band5 reflectance - band2 reflectance
    hr_b5R = resampleGrid(b5R, b2R)
    return sub(hr_b5R, b2R)*100

# Split Cloud Phase Channel Difference
def ABISplitCloudPhaseDifference(b14T, b11T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_G16_CloudPhaseBTD.pdf
    # band14 temperature - band11 temperature
    return sub(b14T, b11T)

# Split Window Channel Difference
def ABISplitWindowDifference(b15T, b13T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_SplitWV_BTDiffv2.pdf
    # band15 temperature - band13 temperature
    return sub(b15T, b13T)

# Night Fog Difference
def ABINightFogDifference(b13T, b7T):
    # http://cimss.ssec.wisc.edu/goes/OCLOFactSheetPDFs/ABIQuickGuide_NightFogBTD.pdf
    # band13 temperature - band7 temperature
    return sub(b13T, b7T)

# The two functions below are designed to work with ABI L1b and L2 CMIP files loaded
# as gridded data sources to give temperature as a calibration for L1b and radiance
# as a calibration for L2 CMIP data.

def abiRadToTemp(data):
    # Function to convert ABI L1b netCDF file radiance to temperature
    # Valid only for bands 7 through 16
    # Formula to calculate temberature from the NOAA ATBD:
    # https://www.star.nesdis.noaa.gov/goesr/docs/ATBD/Imagery.pdf
    # fk1/2 and bc1/2 values are extracted from the data then used in the function
    fk1 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_fk1').read()))
    fk2 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_fk2').read()))
    bc1 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_bc1').read()))
    bc2 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_bc2').read()))
    T = ( fk2 / (log((fk1 /data) + 1)) - bc1 ) / bc2
    return T

def abiTempToRad(data):
    # Function to convert ABI L2 CMIP netCDF file temperatue to radiance
    # Valid only for bands 7 through 16
    # Formula to calculate radiance from the NOAA ATBD:
    # https://www.star.nesdis.noaa.gov/goesr/docs/ATBD/Imagery.pdf
    # fk1/2 and bc1/2 values are extracted from the data then used in the function
    fk1 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_fk1').read()))
    fk2 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_fk2').read()))
    bc1 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_bc1').read()))
    bc2 = float(str(data[0].geoGrid.dataset.getDataVariable('planck_bc2').read()))
    R = fk1 / (exp (fk2 / (bc1 + (bc2 * data))) -1)
    return R