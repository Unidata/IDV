
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

proc setCategory {cat} {
    set ::dfltCategory $cat
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


if {0} {



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
  

startPlugin mexico "Mexico States" -category {Misc}
makeMap files/aschistmx.nws "Mexico States"
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



startPlugin europemaps "European Maps" -category Europe -color 0x00CC00 
makeMap files/OUTLEURO Europe   OUTLEURO
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


}



startPlugin africamaps "African Maps" -category {Africa} -color 0x00CC00 
setCategory "Africa A-E"
makeMap africa/Africa.zip  {Africa}
makeMap africa/ALG_outline.zip  {Algeria outline}
makeMap africa/ALG.zip          {Algeria }
makeMap africa/ANG-level_1.zip  {Angola level 1}
makeMap africa/ANG.zip          {Angola }
makeMap africa/BEN-level_1.zip  {Benin level 1}
makeMap africa/BEN.zip          {Benin }
makeMap africa/BOT-level_1.zip  {Botswana level 1}
makeMap africa/BOT.zip          {Botswana }
makeMap africa/BUF-level_1.zip  {Burkina Faso level 1}
makeMap africa/BUF.zip          {Burkina Faso }
makeMap africa/BUR-level_1.zip  {Burundi level 1}
makeMap africa/BUR.zip          {Burundi }
makeMap africa/CAM-level_1.zip  {Cameroon level 1}
makeMap africa/CAM.zip          {Cameroon }
makeMap africa/CAP_outline.zip  {Cape Verde outline}
makeMap africa/CAP.zip          {Cape Verde }
makeMap africa/CAR-level_1.zip  {Central African Republic level 1}
makeMap africa/CAR.zip          {Central African Republic }
makeMap africa/CDI-level_1.zip  {Ivory Coast level 1}
makeMap africa/CDI.zip          {Ivory Coast }
makeMap africa/CHA_outline.zip  {Chad outline}
makeMap africa/CHA.zip          {Chad }
makeMap africa/CNG-level_1.zip  {Congo level 1}
makeMap africa/CNG.zip          {Congo }
makeMap africa/COM_outline.zip  {Comoros outline}
makeMap africa/COM.zip          {Comoros }
makeMap africa/DJI-level_1.zip  {Djibouti level 1}
makeMap africa/DJI.zip          {Djibouti }
makeMap africa/EGY-level_1.zip  {Egypt level 1}
makeMap africa/EGY.zip          {Egypt }
makeMap africa/EQG-level_1.zip  {Equatorial Guinea level 1}
makeMap africa/EQG.zip          {Equatorial Guinea }
makeMap africa/ERI-level_1.zip  {Eritrea level 1}
makeMap africa/ERI.zip          {Eritrea }
makeMap africa/ETH-level_1.zip  {Ethiopia level 1}
makeMap africa/ETH.zip          {Ethiopia }

setCategory "Africa G-M"
makeMap africa/GAB-level_1.zip  {Gabon level 1}
makeMap africa/GAB.zip          {Gabon }
makeMap africa/GHA-level_1.zip  {Ghana level 1}
makeMap africa/GHA.zip          {Ghana }
makeMap africa/GIN-level_1.zip  {Guinea level 1}
makeMap africa/GIN.zip          {Guinea }
makeMap africa/GUB-level_1.zip  {Guinea Bissau level 1}
makeMap africa/GUB.zip          {Guinea Bissau }
makeMap africa/KEN-level_1.zip  {Kenya level 1}
makeMap africa/KEN.zip          {Kenya }
makeMap africa/LAJ_outline.zip  {Libya outline}
makeMap africa/LAJ.zip          {Libya }
makeMap africa/LES-level_1.zip  {Lesotho level 1}
makeMap africa/LES.zip          {Lesoth }
makeMap africa/LIB-level_1.zip  {Liberia level 1}
makeMap africa/LIB.zip          {Liberia }
makeMap africa/MAA-level_1.zip  {Malawi level 1}
makeMap africa/MAA.zip          {Malawi }
makeMap africa/MAD-level_1.zip  {Madagascar level 1}
makeMap africa/MAD.zip          {Madgascar }
makeMap africa/MAL-level_1.zip  {Mali level 1}
makeMap africa/MAL.zip          {Mali }
makeMap africa/MAU-level_1.zip  {Mauritania level 1}
makeMap africa/MAU.zip          {Mauritania }
makeMap africa/MOR-level_1.zip  {Morocco level 1}
makeMap africa/MOR.zip          {Morocco }
makeMap africa/MOZ-level_1.zip  {Mozambique level 1}
makeMap africa/MOZ.zip          {Mozambique }

setCategory "Africa N-Z"
makeMap africa/NAM-level_1.zip  {Namibia level 1}
makeMap africa/NAM.zip          {Namibia }
makeMap africa/NIG-level_1.zip  {Nigerlevel 1}
makeMap africa/NIG.zip          {Niger }
makeMap africa/NIR-level_1.zip  {Nigeria level 1}
makeMap africa/NIR.zip          {Nigeria }
makeMap africa/RWA-level_1.zip  {Rwanda level 1}
makeMap africa/RWA.zip          {Rwanda }
makeMap africa/SEN-level_1.zip  {Senegal level 1}
makeMap africa/SEN.zip          {Senegal }
makeMap africa/SIL-level_1.zip  {Sierra Leone level 1}
makeMap africa/SIL.zip          {Sierra Leone}
makeMap africa/SOM-level_1.zip  {Somalia level 1}
makeMap africa/SOM.zip          {Somalia }
makeMap africa/SOU-level_1.zip  {South Africa level 1}
makeMap africa/SOU.zip          {South Africa }
makeMap africa/STP-level_1.zip  {Sao Tome level 1}
makeMap africa/STP.zip          {Sao Tome }
makeMap africa/SUD-level_1.zip  {Sudan level 1}
makeMap africa/SUD.zip          {Sudan }
makeMap africa/SWA-level_1.zip  {Swaziland level 1}
makeMap africa/SWA.zip          {Swaziland }
makeMap africa/TAN-level_1.zip  {Tanzania level 1}
makeMap africa/TAN.zip          {Tanzania }
makeMap africa/TOG-level_1.zip  {Togo level 1}
makeMap africa/TOG.zip          {Togo }
makeMap africa/TUN-level_1.zip  {Tunisia level 1}
makeMap africa/TUN.zip          {Tunisia }
makeMap africa/UGA-level_1.zip  {Uganda level 1}
makeMap africa/UGA.zip          {Uganda }
makeMap africa/WES-level_1.zip  {Western Sahara level 1}
makeMap africa/WES.zip          {Western Sahara }
makeMap africa/ZAI-level_1.zip  {Zaire level 1}
makeMap africa/ZAI.zip          {Zaire }
makeMap africa/ZAM-level_1.zip  {Zambia level 1}
makeMap africa/ZAM.zip          {Zambia }
makeMap africa/ZIM-level_1.zip  {Zimbabwe level 1}  
makeMap africa/ZIM.zip          {Zimbabwe }
endPlugin

