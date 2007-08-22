
set ::gempak "laraine:/home/gempak/GEMPAK5.9.4/gempak/maps/ascii/asc"
set jar jar

proc makeMap {file name args} {
#    puts "   Map: $file"
    global xml mapFiles pluginName dir 
    array set A {-category Maps -color 0x00CC00}
    if {$::dfltCategory!=""} {set A(-category) $::dfltCategory}
    if {$::dfltColor!=""} {set A(-color) $::dfltColor}
    array set A $args
    lappend mapFiles  $file
    append xml  "\n<map\n\tcategory=\"$A(-category)\" \n"
    append xml "\tsource=\"/$dir/[file tail $file]\"\n"
    append xml "\tdescription=\"$name\"\n\tvisible=\"false\"\n"
    append xml "\tcolor=\"$A(-color)\"\n\tlinewidth=\"1.0\"\n\tlinestyle=\"0\"\n"
    append xml "/>\n"

#    file copy $file [file join $dir [file tail $file]]
    exec scp $file [file join $dir [file tail $file]]

}


proc startPlugin {name desc  args} {
    array set A {-category Maps -color 0x00CC00}
    array set A $args
    global xml mapFiles dir pluginName  pluginDesc 
    set ::dfltCategory $A(-category)
    set ::dfltColor $A(-color)
#    puts "Plugin: $name"

    set pluginName $name
    set pluginDesc $desc

    set mapFiles [list]
    set xml "<maps>\n"
    set dir ${pluginName}_dir
    file delete -force $dir
    file mkdir $dir
}




proc endPlugin {} {
    global dir pluginName   pluginDesc
    global xml mapFiles jar
    append xml "</maps>"
    set fp [open [file join $dir ${pluginName}_maps.xml] w]
    puts $fp $xml
    close $fp
#    puts "    making $pluginName.jar"
    exec ${jar} -cvf $pluginName.jar $dir

    if {$::dfltCategory == ""} {
        set ::dfltCategory Maps
    }
    set size [file size $pluginName.jar]
    
    puts "<plugin name=\"$pluginDesc\" description=\"Map\" size=\"$size\" url=\"http://www.unidata.ucar.edu/software/idv/plugins/maps/$pluginName.jar\" category=\"Maps\"/>"
}








startPlugin usroads "U.S. Highways and Roads" -category Roads
makeMap ${gempak}loshus.nws "State Highways" -color cyan 
makeMap ${gempak}louhus.nws "US Highways" -color cyan 
makeMap files/usroads.shp "U.S. Roads" -color cyan 
endPlugin


startPlugin lakes "U.S. Rivers and Lakes" -category Water
makeMap ${gempak}islands.cia "Islands" -color blue 
makeMap ${gempak}lakes.cia "Low Resoltion Lakes" -color blue 
makeMap files/lakes.zip "High Resolution Lakes" -color blue 
makeMap ${gempak}lorvwo.cia "Rivers"
makeMap ${gempak}hirvus.usg "Hi-res US Rivers"
endPlugin


startPlugin nwsmarinezones "Marine Zones" -category  Marine
makeMap files/coastalmarinezones.zip "Coastal Marine Zones" -color blue -category "Marine Zones"
makeMap files/offshoremarinezones.zip "Offshore Marine Zones" -color blue -category "Marine Zones"
makeMap files/highseaszones.zip "High Seas Zones" -color blue -category "Marine Zones"
endPlugin
  

startPlugin gempak_basic "Gempak Weather" -category {Weather}
makeMap ${gempak}bwx1224.ncp "Basic Weather 12 and 24hr"
makeMap ${gempak}bwx3648.ncp "Basic Weather 36 and 48hr"
makeMap ${gempak}mefbbw.ncp "Basic Weather GIF"
makeMap ${gempak}mefbmr.ncp "Medium Range Weather"
makeMap ${gempak}hicwa.nws "County Warning Areas"
#startPlugin nwsareamaps
#makeMap files/countywarningareas.zip "NWS County Warning Areas" -color blue -category Weather
#endPlugin

endPlugin 



startPlugin gempak_basins "River Forecast Basins" -category  {River Forecast Basins}
makeMap ${gempak}rfc.nws "River Forecast Centers"
makeMap ${gempak}tprbab.rfc "AR-Red Basin RFC Basins"
makeMap ${gempak}tprblm.rfc "Lower MS RFC Basins"
makeMap ${gempak}tprbma.rfc "Mid Atlantic RFC Basins"
makeMap ${gempak}tprbmb.rfc "MO Basin RFC Basins"
makeMap ${gempak}tprbnc.rfc "North Central RFC Basins"
makeMap ${gempak}tprbne.rfc "Northeast RFC Basins"
makeMap ${gempak}tprboh.rfc "Ohio RFC Basins"
makeMap ${gempak}tprbse.rfc "Southeast RFC Basins"
makeMap ${gempak}tprbwg.rfc "West Gulf RFC Basins"
endPlugin 


startPlugin gempak_maps "Climate Zones" -category {Climate Zones}
#makeMap ${gempak}histus.nws "US states, NWS"
#makeMap ${gempak}hicnus.nws "US counties, NWS"
makeMap ${gempak}hiznus.nws "US zones, NWS"
makeMap ${gempak}tpczus.cpc "US CPC Climate Zones"
makeMap ${gempak}tppzus.cpc "US Palmer Climate Zones"
#makeMap ${gempak}hicowo.cia "World Coasts"
#makeMap ${gempak}hicywo.cia "Hi-res World Countries"
endPlugin 



#makeMap ${gempak}mxpowo.ncp "Mix-res Pol Bndry,World"

startPlugin gempak_forecast "Offshore Forecast Areas" -category {Offshore Forecast Areas}
makeMap ${gempak}mehsuo.ncp "High Seas Forecast Areas"
makeMap ${gempak}mereuo.ncp "Regional Sea Fcst Areas"
makeMap ${gempak}hiosuo.nws "Offshore Forecast Areas"
makeMap ${gempak}himouo.nws "Offshore Forecast OPC"
makeMap ${gempak}mefbna.ncp "North America Fcst Bnds"
endPlugin 


startPlugin gempak_intl  "Pacific and Atlantic Products" -category "Pacific and Atlantic Products"
makeMap ${gempak}mefbpe.ncp "East Pacific Products"
makeMap ${gempak}mefbpn.ncp "North Pacific Products"
makeMap ${gempak}mefban.ncp "North Atlantic Products"
makeMap ${gempak}mefbps.ncp "Pacific Surface Products"
makeMap ${gempak}mefbp1.ncp "Pacific SFC Anal Part 1"
makeMap ${gempak}mefbp2.ncp "Pacific SFC Anal Part 2"
makeMap ${gempak}mefba1.ncp "Atlantic SFC Anal Part 1"
makeMap ${gempak}mefba2.ncp "Atlantic SFC Anal Part 2"
endPlugin 

if {0} {
makeMap ${gempak}mefbqv.ncp "QPF Verification"
makeMap ${gempak}hifiwo.ncp "Flight Information Regn"
makeMap ${gempak}tpcsus.ncp "Convective Sigmet Bounds"
makeMap ${gempak}tpfbwr.ncp "Western Region Boundary"
makeMap ${gempak}tptana.ncp "TPC Surface Analysis"
makeMap ${gempak}tptbna.ncp "TPC Surface Boundary"
makeMap ${gempak}tpthna.ncp "TPC High Seas"
makeMap ${gempak}tptsna.ncp "TPC SigMets"
makeMap ${gempak}tptvna.ncp "TPC Aviation"
makeMap ${gempak}dcvaac.ncp "DC VAAC Boundary"
makeMap ${gempak}hpcsfc.ncp "HPC SFC Anl Boundary"
makeMap ${gempak}hikfwa.ncp "1000 Fathom Line"
}




startPlugin europemaps "European Maps" -category Europe -color 0x00CC00 
makeMap files/OUTLEURO Europe 
makeMap files/austria.shp Austria 
makeMap files/belgium.shp Belgium 
makeMap files/czech.shp Czech 
makeMap files/denmark.shp Denmark 
makeMap files/finland.shp Finland 
makeMap files/france.shp France 
makeMap files/germany.shp Germany 
makeMap files/hungary.shp Hungary 
makeMap files/italy.shp Italy 
makeMap files/norway.shp Norway 
makeMap files/portugal.shp Portugal 
makeMap files/provinces.shp Provinces 
makeMap files/spain.shp Spain 
makeMap files/sweden.shp Sweden 
makeMap files/switzerland.shp Switzerland 
makeMap files/uk.shp UK 
endPlugin


startPlugin chinamaps "China Maps" -category China -color 0x00CC00 
makeMap files/chinacounty.shp "China County Lines"
makeMap files/chinalake.shp  "China Lakes"
makeMap files/chinaprovince.shp "China Province Boundary" 
makeMap files/chinariver.shp "China River" 
endPlugin

#startPlugin  testmaps
#makeMap asclotzus.nws "Time Zones" -color yellow -category "TEST"
#makeMap asclakes.cia "Lakes" -color blue -category "TEST"
#endPlugin 


