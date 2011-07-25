
##format:
##defineImage url {group>name} [-id <id>] [-desc <desc>] [-group <group>] [-ll {latitude,longitude}] [-dz delta from GMT]
##
##If there are any spaces in an argument enclose it within brackets
##
##If a group is not given as a -group arg then we look at the name
##and extract out the group with  ">"  as a delimiter
##If no group still we use the most recent one defined by the defineGroup call
##
##If no -desc provided we use {From: <hostname>}
##
##We use the -dz to sort of guess when nighttime is so we won't fetch images then
##


defineGroup Rockies
defineImage http://www.eol.ucar.edu/webcam/latest.jpg {UCAR Boulder, CO} -ll {40.01,-105.26} -dz -7
defineImage http://www.esrl.noaa.gov/gsd/webcam/flatiron.jpg {NOAA Boulder, CO} -ll {39.9917,-105.2618} -dz -7

defineImage http://boulderflatironcam.com/bfc/bfc.jpg {Flatirons Boulder, CO} -ll {40.0,-105.0} -dz -7 -heading {270}
defineImage http://63.147.112.178:9595/axis-cgi/jpg/image.cgi?resolution=640x480 {Eldora Mountain, CO} -dz -7 -ll {39.94,-105.58} -heading {240}
defineImage http://www.windcliff.com/webcam/netcam.jpg {Rocky Mountain National Park, CO} -dz -7 -ll {40.426,-105.573}
defineImage http://www.unco.edu/liveview/images/UNC2.jpg {University of NorthernColorado, Greeley, CO} -dz -7 -ll {40.42418,-104.693098}



defineImage http://www.9news.com/9live/images/cams/pic1.jpg {Denver, CO} -dz -7 -ll {39.75,-105}
defineImage http://www.cotrip.org/images/camera?imageURL=60002.jpg {Eisenhower Tunnel, CO} -ll {39.68,-105.9} -dz -7
defineImage http://utepasscams.com/cams/216.jpg {Pikes Peak, CO} -ll {38.84056,-105.04389} -dz -7

defineImage http://ouraynet.com/blowout.jpg  {Ouray, CO} -dz -7 -ll {38.04,-107.76} -heading {225}

defineImage {http://www-das.uwyo.edu/weathercam/weathercam.jpg} {Laramie, WY} -ll {41.316,-105.578} -dz -7
defineImage http://www2.tetoncam.com/tetonl.jpg {Grand Tetons, WY} -ll {43.74111,-110.80167} -dz -7
defineImage {http://vmserver.net/webcams/bozeman/bozeman.jpg} {Bridger Mountains, MT} -ll {45.71,-111.07} -dz -7




defineGroup {National Parks}
defineImage {http://www.nps.gov/webcams-glac/ghcam.jpg} {Glacier NP - Goat Haunt, MT} -ll {48.95786,-113.89206} -dz -7
defineImage {http://www.nps.gov/webcams-glac/smcam.jpg} {Glacier NP - St. Mary, MT} -ll {48.729,-113.442} -dz -7
defineImage {http://www.nps.gov/webcams-glac/mcdcam.jpg} {Glacier NP - Lake McDonald, MT} -ll {48.52936,-113.99487} -dz -7
defineImage {http://www.nps.gov/webcams-glac/tmcam.jpg} {Glacier NP - Two Medicine, MT} -ll {48.49859,-113.34755} -dz -7
defineImage {http://www.nps.gov/webcams-yell/oldfaithvc.jpg} {Old Faithful, WY} -ll {44.45972,-110.83139} -dz -7
defineImage {http://nature.nps.gov/air/WebCams/parks/throcam/thro.jpg}  {Theodore Roosevelt National Park, ND} -ll {46.94889,-103.43306}  -dz -7
defineImage {http://www.nature.nps.gov/air/webcams/parks/bibecam/bibe.jpg}  {Big Bend National Park,TX} -ll {29.25,-103.25}  -dz -6
defineImage {http://www.nps.gov/webcams-olym/current_ridgecam.jpg}  {Olympic National Park} -ll {47.934,-123.371}  -dz -8	
defineImage {http://www.nature.nps.gov/air/webcams/parks/olymcam/olym.jpg} {Lake Crescent,WA}  -ll {48.094531,-123.805311}


defineImage {http://www.nature.nps.gov/air/WebCams/parks/moracam/mora.jpg}  {Mount Rainier National Park} -ll {46.88333,-121.88333}  -dz -8	
defineImage {http://www.nature.nps.gov/air/webcams/parks/nocacam/noca.jpg}  {North Cascades National Park} -ll {48.66667,-121.25028}  -dz -8	
defineImage {http://www.nature.nps.gov/air/WebCams/parks/porecam/pore.jpg}  {Point Reyes National Seashore} -ll {38.06667,-122.88333}  -dz -8	
defineImage {http://www.yosemiteconservancystore.com/DSN/wwwyosemiteassociationorg/Content/Webcam/ahwahnee.jpg}  {Yosemite National Park} -ll {37.85,-119.56667}  -dz -8
defineImage {http://www.nature.nps.gov/air/webcams/parks/jotrcam/jotr.jpg}  {Joshua Tree National Park} -ll {35.12861,-116.03667}  -dz -8	
defineImage {http://www.nature.nps.gov/air/WebCams/parks/nacccam/wash.jpg}  {Washington, D.C.} -ll {38.3,-77.0}  -dz -8	

defineImage {http://www.hazecam.net/images/photos-main/BALTIMORE_1.JPG} {Baltimore,MD}  -ll {39.290555,-76.609604}

defineImage {http://www.hazecam.net/images/photos-main/FROSTBURG.JPG} {Frostburg,MD}  -ll {39.656902,-78.927609}



defineImage {http://www.nature.nps.gov/air/webcams/parks/grsmcam/grsm.jpg}  {Great Smoky Mountains National Park} -ll {35.60056,-83.50889}  -dz -8	
defineImage {http://www.nature.nps.gov/air/webcams/parks/grcacam/grca.jpg}  {Grand Canyon - Yavapai Point} -ll {36.06502,-112.11841}  -dz -8	
defineImage {http://www.nature.nps.gov/air/WebCams/parks/denacam/dena.jpg}  {Denali National Park } -ll {63.46,-150.87162}  -dz -10	

defineImage {http://www.nature.nps.gov/air/webcams/parks/macacam/maca.jpg} {Mammoth Cave National Park}  -ll {37.186699,-86.098999}
defineImage {http://hvo.wr.usgs.gov/cams/KIcam/images/M.jpg} {Hawaii Volcanoes National Park}  -ll {19.428749,-155.253677}






defineGroup {West Coast}
defineImage http://castrocam.net/castrocam.jpg {San Francisco,CA} -ll {37.76298,-122.440395} -dz -8
defineImage http://sv.berkeley.edu/view/images/current_view.jpg {Berkeley,CA} -ll {37.86972,-122.25778} -dz -8
defineImage http://www.met.sjsu.edu/cam_directory/latest.jpg {San Jose State} -ll {37.3044,-121.85} -dz -8
defineImage http://obs.astro.ucla.edu/images/towercam.jpg {Mt. Wilson, CA} -ll {34.22583,-118.05639} -dz -8
defineImage http://abclocal.go.com/three/kabc/webcam/web1-2.jpg?1144856196725 {Los Angeles, CA} -ll {34,-118.24} -dz -8
defineImage {http://hpwren.ucsd.edu/gphoto2-a2/CNMVCSD2/LAST.jpg} {San Diego,CA}  -ll {32.715685,-117.161724}
defineImage {http://208.74.106.27/netcam.jpg} {Whiskey Town,CA}  -ll {44.30315,-118.324272}


###defineImage http://www.lesjackson.com/capture1.jpg {Monterey, CA} -ll {36.59,-121.882} -dz -8


defineImage http://www.monolake.org/livedata/camtwo.jpg {Mono Lake} -ll {38.01667,-119.00833} -dz -8
defineImage http://www.gbuapcd.org/dustcam/keelerv1.jpg {Owens Lake} -ll {36.501,-117.902} -dz -8





defineImage http://www.fs.fed.us/gpnf/volcanocams/msh/images/mshvolcanocam.jpg {Mount St. Helens, WA} -ll {46.272,-122.152} -dz -8
defineImage http://webcam.uoregon.edu/jpg/image.jpg {Eugene, OR} -ll {44.04,-123.07} -dz -8
defineImage http://bimedia.ftp.clickability.com/fishwebftp/KATU/ABANK.JPG {Portland, OR} -ll {45.59,-122.6} -dz -8
defineImage http://centralmediaserver.com/koin/weathercams/koinsw.jpg {Portland, OR} -ll {45.59,-122.6} -dz -8
defineImage http://images.ibsys.com/sea/images/weather/auto/queenannecam_640x480.jpg {Seattle,WA} -ll {47.444,-122.317} -dz -8
defineImage http://134.121.11.63/images/bryan/bryan_00001.jpg {Pullman,WA} -ll {46.73193,-117.165365} -dz -8




defineGroup Alaska


defineImage http://www.borealisbroadband.net/DTN/dtnsemega.jpg {Anchorage, AK} -ll {61.21,-149.92} -dz {-9}
defineImage http://www.avo.alaska.edu/webcam/augtst.jpg {Augustine Volcano, AK} -ll {59.3626,-153.435} -dz {-9}
###defineImage http://akweathercams.faa.gov/wxdata/35-22368.jpg {Denali, AK} -dz -9 -ll {63.43,-149.54}




defineGroup Southwest
defineImage http://www.cs.arizona.edu/camera/view.jpg {Tuscon, AZ} -ll {32.22694,-110.96083} 
defineImage http://www.wrh.noaa.gov/images/fgz/webcam/camera1.jpg {Bellemont, AZ} -ll {35.24,-111.84} 

defineImage http://www.noao.edu/kpno/kpcam/lastim.jpg {Kitt Peak, AZ} -ll {31.9639671, -111.5998359} 

defineImage http://wwc.instacam.com/instacamimg/LTCRC/LTCRC_l.jpg {Las Cruces,  NM} -ll {32.3,-106.7}

defineImage http://www.bradlight.com/cam1.jpg {Carlsbad,NM}  -ll {32.418595,-104.229019}

defineImage http://wwc.instacam.com/instacamimg/KOBTV/KOBTV_l.jpg {Albuquerque,NM}  -ll {35.1029697, -106.6703963}

defineImage http://belo.bimedia.net/WFAA/weather/stills/wfaahuge.jpg {Dallas,TX} -ll {32.775,-96.79667} -dz -6
defineImage http://www.atmo.ttu.edu/includes/Photo_N.jpg {Lubbock,TX} -ll {33.5777778, -101.8547222} -dz -6

defineImage http://www.nps.gov/webcams-glca/hc1.jpg {Halls Crossing, Lake Powell, UT} -ll {37.45694,-110.7122} -dz -7
defineImage http://www.nps.gov/webcams-glca/ww1.jpg {Wahweap, Lake Powell, UT} -ll {37.01278,-111.48944} -dz -7
defineImage http://www.neng.usu.edu/webcam/latest/latest_bldcam2.jpg {Logan, UT} -ll {41.73444,-111.85361} -dz -7
###Cannot find URL for latest Castle Dale image.
###defineImage http://www.ewcd.org/webcams/images/Castle_Dale_N_View/800.jpg {Castle Dale,UT} -ll {39.222,-111.02} -dz -7
defineImage http://155.98.55.200/image.jpg {University of Utah at Salt Lake City, UT} -dz -7 -ll {40.76623,-111.84755}
defineImage http://instacam.com/instacamimg/ZPRNG/ZPRNG_S.jpg {Zion National Park, UT} -dz -7 -ll {37,-113}
defineImage http://www.cnha.org/webcam/image.jpg {Moab, UT} -dz -7 -ll {38.59,-109.55}

defineImage {http://www.wrh.noaa.gov/boi/images/BOI2-CAM.jpg} {Boise,ID}  -ll {43.60698,-116.193409}




defineGroup "Midwest"

defineImage http://165.134.236.34/-wvhttp-01-/GetStillImage?p=-15&t=-5&z=3&b=off&camer_id=1  {Saint Louis Univertiy,MO} -ll {38.637,-90.234} -dz -6
defineImage http://www.soils.wisc.edu/asig/webcam/halfsize.jpg  {Madison,WI} -ll {43.07980,-89.38751} -dz -6


defineImage http://images.ibsys.com/det/images/weather/auto/windsorcam_320x240.jpg {Detroit,MI} -dz -5 -ll {42.2,-83.1}
defineImage http://www.myfoxwfld.com/webcam/sears/sears.jpg {Chicago, IL} -dz -6 -ll {41.8,-87.7}

defineImage {http://www.glerl.noaa.gov/metdata/tol2/tol2-1.jpg} {Toledo, OH}  -ll {41.65381,-83.536259}
defineImage {http://www.glerl.noaa.gov/webcams/images/lmfs1.jpg} {Muskegon,MI}  -ll {43.23424,-86.245929}

#defineImage {ftp://ftp.glerl.noaa.gov/realtime/alpmet/alp2.jpg} {Alpena,MI}  -ll {45.061565,-83.445154}
#defineImage {ftp://ftp.glerl.noaa.gov/realtime/mil/mil1.jpg} {Milwaukee,WI}  -ll {43.04181,-87.906844}



defineImage http://www.starcitywebcam.com/images/cam1l.jpg {Lincoln,NE} -dz -6 -ll {40.7,-96.66}
defineImage http://66.231.15.194/Weather/Files_Forecast/cam4.jpg {Watertown,SD}  -ll {44.900588,-97.105744}






defineGroup "Northeast"
defineImage http://www.hazecam.net/images/photos-main/newark.jpg {Newark,NJ} -dz  -5 -ll {40.71,-74}
defineImage http://www.hazecam.net/images/photos-main/BLUEHILL.JPG {Boston,MA} -dz -5 -ll {42,-71}
defineImage http://www.dotdata.ct.gov/trafficcameras/images/image87.jpg {Hartford,CT} -dz -5 -ll {41.74,-72.65}
defineImage http://www.hazecam.net/images/photos-main/MTWASH.JPG {Mount Washington, NH} -dz -5 -ll {44,-71}
defineImage http://www.hazecam.net/images/photos-main/BURLINGTON.JPG {Burlington, VT} -dz -5 -ll {44,-73}
defineImage http://apollo.lsc.vsc.edu/webcam/LSCimage.jpg {Lyndon State College, VT} -ll {44.53528,-72.02611} -dz -5

defineImage http://wwc.instacam.com/instacamimg/mllru/MLLRU_l.jpg {Millersville University of Pennsylvania} -ll {40,-76.35} -dz -5
defineImage http://www.instacam.com/instacamimg/CALIF/CALIF_s.jpg {California University of Pennylvania} -ll {40.066,-79.892} -dz -5


defineImage  http://www.wchstv.com/newsroom/wvtcam3.jpg {Charleston,WV}  -ll {38.350195,-81.638989}


defineImage http://www.gettysburgaddress.com/GIFS/battle.jpg {Gettysburg,PA} -ll {39.82111,-77.22361} -dz -5
defineImage http://www.nature.nps.gov/air/WebCams/parks/acadcam/acad.jpg {Acadia National Park, ME} -dz -5 -ll {44.378,-68.2588}
defineImage http://vortex.plymouth.edu/webcam/2/latest.jpeg {Plymouth State University, NH} -dz -5 -ll {43.8,-71.7}




defineGroup {Southeast} 
defineImage http://images.ibsys.com/atl/images/weather/auto/towercam1_640x480.jpg {Atlanta,GA} -ll {33.74889,-84.38806} -dz -5
defineImage http://files.wtoc.com/camera/misc/misc.jpg {Savannah,GA} -ll {32,-81.1} -dz -5


defineImage http://www.mymiamiview.com/images/webcam.jpg {Miami,FL} -ll {25.7,-80.1} -dz -5
defineImage http://doppler.tbo.com/webcam/towercam.jpg {Tampa,FL} -ll {27.84806,-82.26333} -dz -5
###defineImage http://www.news-journalonline.com/webcam/dbcam.jpg {Daytona Beach,FL} -ll {29.2,-81.0} -dz -5

defineImage http://instacam.com/instacamimg/NPLES/NPLES_S.jpg?14831739 {Naples,FL} -dz -5 -ll {26.145,-81.795}
defineImage http://www.erh.noaa.gov/rah/weathercam/wxcam.image.jpg {Raleigh, NC} -dz -5 -ll {35,-78}



defineImage http://www.spadre.com/southpadrebeachcam.jpg {South Padre Island,TX} -dz -5 -ll {26.1,-97.2}
defineImage http://webcams.galveston.com/docs/commodore2006/commodoremp.jpg {Galveston,TX} -dz -5 -ll {29.29,-94.79}

defineImage http://downburst.geos.ulm.edu:8080/image.jpg {University of Louisiana, Monroe, LA} -dz -6 -ll {32.538,-92.074}

defineImage http://images.ibsys.com/kan/images/weather/auto/kci_640x480.jpg {Kansas City,MO}  -ll {39.10296,-94.583062}



defineImage {http://www.rwic.und.edu/webcam/webcam32.jpg} {Grand Forks,ND}  -ll {47.924085,-97.032034}


defineImage http://www.scottcountyiowa.com/webcams/images/live/112/cam112_00051.jpg {Scott County, IA}  -ll {41.61293,-90.606277}
 





defineImage http://ksbi.bimedia.net/mcc.jpg {Moore,OK} -dz -5 -ll {35.48,-97.54}


defineImage http://webcam.icorp.net/ar1.jpg {Metairie,LA} -dz -5 -ll {29.98,-90.15}






defineGroup {Antarctica}
defineImage http://www.esrl.noaa.gov/gmd/webdata/spo/webcam/cmdlfullsize.jpg {South Pole}   -ll {-90, -105}
defineGroup {Europe}
defineImage http://www.cardada.ch/webcam/Cardada_mittel.jpg {Locarno, Switzerland}  -dz +1 -ll {46.2, 8.8}
defineImage http://www.knmi.nl/webcam/images/ispy.jpg {KNMI, Netherlands}  -dz +1 -ll {52,5}



defineGroup {Asia}
defineImage http://www.discoverhongkong.com/eng/interactive/webcam/images/ig_webc_vict1.jpg {Hong Kong, China}  -dz +8 -ll {22.3, 114.18}


