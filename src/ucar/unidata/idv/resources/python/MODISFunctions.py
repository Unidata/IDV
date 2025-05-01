# MODIS Airmass RGB
def MODISAirmassRGB(b27T, b28T, b30T, b31T):
    # red = band27 - band28; -25K to 0K rescalled to 0 to 255
    # grn = band30 - band31; -40K to 5K rescalled to 0 to 255
    # blu = band27; 243K to 208K rescalled to 0 to 255
    red = rescale(b27T-b28T, -25, 0, 0, 255)
    grn = rescale(b30T-b31T, -40, 5, 0, 255)
    blu = rescale(b27T, 243, 208, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS Dust RGB
def MODISDustRGB(b29T, b31T, b32T):
    # red = band32 - band31; -4 to 2K rescalled to 0 to 255
    # grn = band31 - band29; 0K to 15K rescalled to 0 to 255; gamma 2.5
    # blu = band31; 261K to 289K rescalled to 0 to 255
    red = rescale(b32T-b31T, -4, 2, 0, 255)
    grn = 255*(rescale(b31T-b29T, 0, 15, 0, 1)**0.4)
    blu = rescale(b31T, 261, 289, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS Night Microphysics RGB
def MODISNightMicrophysicsRGB(b22T, b31T, b32T):
    # red = band32 - band31; -4K to 2K rescalled to 0 to 255
    # grn = band31 - band21; 0K to 10K rescalled to 0 to 255; gamma 0.4
    # blu = band31; 243K to 293K rescalled to 0 to 255
    red = rescale(b32T-b31T, -4, 2, 0, 255)
    grn = 255*(rescale(b31T-b22T, 0, 10, 0, 1)**2.5)
    blu = rescale(b31T, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS NDVI
def MODISNDVI(b1R, b2R):
    # b1R = Band 1 (0.6465um) Reflectance
    # b2R = Band 2 (0.8567um) Reflectance
    return (b2R-b1R)/(b2R+b1R)

# MODIS NDSI
def MODISNDSI(b4R, b6R):
    # b4R = Band 4 (0.55375um) Reflectance
    # b6R = Band 6 (1.6291um) Reflectance
    return (b4R-b6R)/(b4R+b6R)

# MODIS NDWI
def MODISNDWI(b2R, b4R):
    # b2R = Band 2 (0.8567um) Reflectance
    # b4R = Band 4 (0.55375um) Reflectance
    return (b2R-b4R)/(b2R+b4R)

# MODIS EVI
def MODISEVI(b1R, b2R, b3R):
    # b1R = Band 1 (0.6465um) Reflectance : Red band
    # b2R = Band 2 (0.8567um) Reflectance : NIR
    # b3R = Band 3 (0.4656um) Reflectance : Blue band
    # G = Gain factor (2.5)
    # L = Canopy background adjustment (1)
    # C1, C2 = Coefficients of aerosol resistance term (C1=6, C2=7.5)
    G = 2.5
    L = 1
    C1 = 6
    C2 = 7.5
    return G * ((b2R - b1R)/(b2R + (C1 * b1R) - (C2 * b3R) + L))

# MODIS True Color RGB
def MODISTruColRGB(b1R, b4R, b3R):
    # red = Band 1 Reflectance; 0 to 1 rescalled to 0 to 255
    # grn = Band 4 Reflectance; 0 to 1 rescalled to 0 to 255
    # blu = Band 3 Reflectance; 0 to 1 rescalled to 0 to 255
    red = rescale(b1R, 0, 1, 0, 255)
    grn = rescale(b4R, 0, 1, 0, 255)
    blu = rescale(b3R, 0, 1, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS False Color RGB
def MODISFalseColRGB(b7R, b2R, b1R):
    # red = Band 7 Reflectance; 0 to 1 rescalled to 0 to 255
    # grn = Band 2 Reflectance; 0 to 1 rescalled to 0 to 255
    # blu = Band 1 Reflectance; 0 to 1 rescalled to 0 to 255
    red = rescale(b7R, 0, 1, 0, 255)
    grn = rescale(b2R, 0, 1, 0, 255)
    blu = rescale(b1R, 0, 1, 0, 255)
    return combineRGB(red, grn, blu)

# MODIS Burn Area Index (BAI)
def MODISBAI(B1, B2):
    # B1  = 0.6465um - red Reflectance
    # B2 = 0.8567um  - near IR Reflectance
    return 1/((0.1 -B1)**2 + (0.06 - B2)**2)

# MODIS Normalized Burn Ratio (NDBI)
def MODISNBR(B2, B6):
    # B2 = 0.8567um - near IR Reflectance
    # B6 = 1.61um   - shortwave IR Reflectance
    return (B2-B6) / (B2+B6)

# MODIS Visible Atmospherically Resistant Index (VARI)
def MODISVARI(B1, B3, B4):
    # VARI = Visible Atmospherically Resistant Index
    # Makes vegetation stand out from surrounding areas
    # B1 = red   = 0.6465um reflectance
    # B4 = green = 0.5537um reflectance
    # B3 = blue  = 0.4656um reflectance
    return (B4-B1) / (B4+B1-B3)

# MODIS Normalized Difference Built-up Index (NDBI)
def MODISNDBI(B2, B6):
    # B2 = 0.8567um - near IR Reflectance
    # B6 = 1.61um   - shortwave IR Reflectance
    return (B6-B2) / (B6+B2)