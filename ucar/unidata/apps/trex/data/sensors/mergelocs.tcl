

proc merge {file name args} {
    set fp [open $file w]
    puts stderr "<stationtable category=\"TREX\" name=\"$name\"  href=\"/ucar/unidata/apps/trex/data/sensors/$file\">"
    puts $fp "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    puts $fp "<stationtable name=\"$name\">"
    foreach f $args {
        set tfp [open $f r]
        foreach line [split [read $tfp] "\n"] {
            if {[regexp {<station } $line]} {
                puts $fp $line
            }
        }            
    }
    puts $fp "</stationtable>"
}



merge sensors.xml {Lidar, etc.} asu_doppler_lidar.xml dlr_doppler_lidar.xml houston_flux_tower.xml houston_sodar.xml leeds_flux_towers.xml leeds_sodars.xml ncar_isff.xml ncar_mapr.xml ncar_miss.xml ncar_real_alt.xml ncar_real.xml noaa_doppler_lidar.xml nrl_aerosol_lidar.xml 


merge  soil.xml  Soil soil_inyo.xml soil_moisture.xml 


merge  dri.xml {DRI Sites} dri_aws.xml leeds_aws.xml 

merge misc.xml {Misc. Sites}  gps.xml innsbruck_car.xml mglass.xml ncar_dbs.xml yale_radar.xml yale_video.xml 

