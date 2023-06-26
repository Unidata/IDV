# A collection of functions to create displays of different MSG RGB products

# These are standard SEVIRI RGBs designed to work with data from
# the MSG series (Meteosat-8 through Meteosat-11) and were provided
# by EUMETSAT (HansPeter Roesli).  These functions were last revised
# by EUMETSAT on 12/15/2011 and were originally available through the
# Miscellaneous > RGB-Eumetsat plugin.  The function names and structure
# have been modified to be consistent with the functions for ABI, AHI, and JPSS.

# Further information about each of these MSG RGBs can be found using the
# MSG Interprtation Guide, specifically under "RGB Composite Guides":
# https://www.eumetsat.int/website/home/Data/Training/TrainingLibrary/DAT_2044069.html

# MSG Airmass RGB
def MSGAirmassRGB(b5T, b6T, b8T, b9T):
    # red = band5 - band6; -25K to 0K rescalled to 0 to 255
    # grn = band8 - band9; -40K to 5K rescalled to 0 to 255
    # blu = band5 inverted; 208K to 243K rescalled to 0 to 255
    red = rescale(b5T-b6T, -25, 0, 0, 255)
    grn = rescale(b8T-b9T, -40, 5, 0, 255)
    blu = rescale(b5T, 243, 208, 0, 255)
    return combineRGB(red, grn, blu)

def MSGAirmassTropicsRGB(b5T, b8T, b9T):
    # red = band5 - band9; -25K to 5K rescalled to 0 to 255
    # grn = band8 - band9; -30K to 25K rescalled to 0 to 255
    # blu = band5 inverted; 243K to 190K rescalled to 0 to 255
    red = rescale(b5T-b9T, -25, 5, 0, 255)
    grn = rescale(b8T-b9T, -30, 25, 0, 255)
    blu = rescale(b5T, 243, 190, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Tropical Airmass RGB
def MSGTropicalAirmassRGB(b5T, b6T, b8T, b9T):
    # red = band5 - band6; -25K to 5K rescalled to 0 to 255
    # grn = band8 - band9; -30K to 25K rescalled to 0 to 255
    # blu = band5 inverted; 190K to 243K rescalled to 0 to 255
    red = rescale(b5T-b6T, -25, 5, 0, 255)
    grn = rescale(b8T-b9T, -30, 25, 0, 255)
    blu = rescale(b5T, 243, 190, 0, 255)
    return combineRGB(red, grn, blu)

# MSG 24-hour cloud microphysics RGB
def MSG24HrMicrophysicsRGB(b7T, b9T, b10T):
    # red = band10 - band9; -4K to 2K rescalled to 0 to 255
    # grn = band9 - band7; 0K to 6K rescalled to 0 to 255; gamma 1.2
    # blu = band9; 248K to 303K rescalled to 0 to 255
    red = rescale(b10T-b9T, -4, 2, 0, 255)
    grn = 255*(rescale(b9T-b7T, 0, 6, 0, 1)**0.8333)
    blu = rescale(b9T, 248, 303, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Dust RGB
def MSGDustRGB(b7T, b9T, b10T):
    # red = band10 - band9; -4K to 2K rescalled to 0 to 255
    # grn = band9 - band7; 0K to 15K rescalled to 0 to 255; gamma 2.5
    # blu = band9; 261K to 289K rescalled to 0 to 255
    red = rescale(b10T-b9T, -4, 2, 0, 255)
    grn = 255*(rescale(b9T-b7T, 0, 15, 0, 1)**0.4)
    blu = rescale(b9T, 261, 289, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Volcanic Ash / SO2 RGB
def MSGSo2RGB(b7T, b9T, b10T):
    # red = band10 - band9; -4K to 2K rescalled to 0 to 255
    # grn = band9 - band7; -4K to 5K rescalled to 0 to 255
    # blu = band9; 243K to 303K rescalled to 0 to 255
    red = rescale(b10T-b9T, -4, 2, 0, 255)
    grn = rescale(b9T-b7T, -4, 5, 0, 255)
    blu = rescale(b9T, 243, 303, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Cloud Microphysics RGB
# Manual rescalling and gamma correction can be applied to display
def MSGCloudMicrophysicsRGB(b7T, b9T, b10T):
    # red = band10 - band9; units of K
    # grn = band9 - band7; units of K
    # blu = band9; units of K
    red = b10T-b9T
    grn = b9T-b7T
    blu = b9T
    return mycombineRGB(red, grn, blu)

# MSG Natural Color RGB
def MSGNaturalColorRGB(b1R, b2R, b3R, minrefl, maxrefl):
    # red = band3 reflectance; rescale minrefl and maxrefl to 0 to 255
    # grn = band2 reflectance; rescale minrefl and maxrefl to 0 to 255
    # blu = band1 reflectance; rescale minrefl and maxrefl to 0 to 255
    # minrefl = lower limit of reflectivity range (units %)
    # maxrefl = upper limit of reflectivity range (units %)
    minVal = float(minrefl)
    maxVal = float(maxrefl)
    red = rescale(b3R, minVal, maxVal, 0, 255)
    grn = rescale(b2R, minVal, maxVal, 0, 255)
    blu = rescale(b1R, minVal, maxVal, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Generic Natural Color RGB
# Manual rescallang and gamma correction can be applied to the display
def MSGGenericNaturalColorRGB(b1R, b2R, b3R):
    # red = band3; reflectivity
    # grn = band2; reflectivity
    # blu = band1; reflectivity
    red = b3R
    grn = b2R
    blu = b1R
    return mycombineRGB(red, grn, blu)

# MSG Severe Convection RGB
def MSGSevereConvectionRGB(b1R, b3R, b4T, b5T, b6T, b9T):
    # red = band5 - band6; rescale -35K to 5K to 0 to 255
    # grn = band4 - band9; rescale -5K to 60K to 0 to 255; gamma 0.5
    # blu = band3 - band1; rescale -75% to 25% to 0 to 255 (units reflectance)
    red = rescale(b5T-b6T, -35, 5, 0, 255)
    grn = 255*(rescale(b4T-b9T, -5, 60, 0, 1)**2)
    blu = rescale(b3R-b1R, -75, 25, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Severe Convection RGB Tuned for Tropics
def MSGSevereConvectionTropicsRGB(b1R, b3R, b4T, b5T, b6T, b9T):
    # red = band5 - band6; rescale -35K to 5K to 0 to 255
    # grn = band4 - band9; rescale -5K to 75K to 0 to 255; gamma .33
    # blu = band3 - band1; rescale -75% to 25% to 0 to 255 (units reflectance)
    red = rescale(b5T-b6T, -35, 5, 0, 255)
    grn = 255*(rescale(b4T-b9T, -5, 75, 0, 1)**3.0303)
    blu = rescale(b3R-b1R, -75, 25, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Generic Severe Convection RGB
# Manual rescallang and gamma correction can be applied to the display
def MSGGenericSevereConvectionRGB(b1R, b3R, b4T, b5T, b6T, b9T):
    # red = band5 - band6 (Temperature; units K)
    # grn = band4 - band9 (Temperature; units K)
    # blu = band3 - band1 (Reflectance; units %)
    red = b5T-b6T
    grn = b4T-b9T
    blu = b3R-b1R
    return mycombineRGB(red, grn, blu)

# MSG Night Cloud Microphysics RGB
def MSGNightMicrophysicsRGB(b4T, b9T, b10T):
    # red = band10 - band9; -4K to 2K rescalled to 0 to 255
    # grn = band9 - band4; 0K to 10K rescalled to 0 to 255
    # blu = band10; 243K to 293K rescalled to 0 to 255
    red = rescale(b10T-b9T, -4, 2, 0, 255)
    grn = rescale(b9T-b4T, 0, 10, 0, 255)
    blu = rescale(b9T, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# MSG Night Cloud Microphysics RGB Tuned for Tropics
def MSGNightMicrophysicsTropicsRGB(b4T, b9T, b10T):
    # red = band10 - band9; -4K to 2K rescalled to 0 to 255
    # grn = band9 - band4; 0K to 5K rescalled to 0 to 255
    # blu = band10; 273K to 300K rescalled to 0 to 255
    red = rescale(b10T-b9T, -4, 2, 0, 255)
    grn = rescale(b9T-b4T, 0, 5, 0, 255)
    blu = rescale(b9T, 273, 300, 0, 255)
    return combineRGB(red, grn, blu)

# MSG HRV Cloud RGB
def MSGHrvCloudRGB(hrv, b9T):
    # red = hrv; 0% to 100% rescalled to 0 to 255
    # grn = hrv; 0% to 100% rescalled to 0 to 255
    # blu = fd band9; 323K to 203K rescalled to 0 to 255
    red = rescale(hrv, 0, 100, 0, 255)
    grn = rescale(hrv, 0, 100, 0, 255)
    blu = rescale(b9T, 323, 203, 0, 255)
    return combineRGB(red, grn, blu)

# MSG HRV Fog RGB
def MSGHrvFogRGB(hrv, b3R):
    # red = fd band3; 0% to 70% rescalled to 0 to 255
    # grn = hrv; 0% to 100% rescalled to 0 to 255
    # blu = hrv; 0% to 100% rescalled to 0 to 255
    hr_b3R = resampleGrid(b3R, hrv)
    red = rescale(hr_b3R, 0, 70, 0, 255)
    grn = rescale(hrv, 0, 100, 0, 255)
    blu = rescale(hrv, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)