
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
defineImage http://www.unco.edu/liveview/images/UNC2.jpg {University of Northern Colorado, Greeley, CO} -dz -7 -ll {40.42418,-104.693098}



defineImage http://archive.9news.com/9live/images/cams/pic1.jpg {Denver, CO} -dz -7 -ll {39.75,-105}
defineImage http://www.cotrip.org/dimages/camera?imageURL=remote/TOCCCTV070M21500-EJT-TOUR-W.jpg {Eisenhower Tunnel, CO} -ll {39.68,-105.9} -dz -7
defineImage http://www.cograilway.com/Summit/summitcam.jpg {Pikes Peak, CO} -ll {38.84056,-105.04389} -dz -7

defineImage http://ouraynet.com/blowout.jpg  {Ouray, CO} -dz -7 -ll {38.04,-107.76} -heading {225}

defineImage {http://www.cabincam.biz/laramie/laramie.jpg} {Laramie, WY} -ll {41.316,-105.578} -dz -7
defineImage http://www2.tetoncam.com/tetonl.jpg {Grand Tetons, WY} -ll {43.74111,-110.80167} -dz -7
defineImage {http://bridgerbowl.com/assets/webcams/cam4.jpg} {Bridger Bowl, MT} -ll {45.819727,-110.917782} -dz -7




defineGroup {National Parks}
defineImage {http://www.nps.gov/webcams-glac/ghcam.jpg} {Glacier NP - Goat Haunt, MT} -ll {48.95786,-113.89206} -dz -7
defineImage {http://www.nps.gov/webcams-glac/smcam.jpg} {Glacier NP - St. Mary, MT} -ll {48.729,-113.442} -dz -7
defineImage {http://www.nps.gov/webcams-glac/mcdcam1.jpg} {Glacier NP - Lake McDonald, MT} -ll {48.52936,-113.99487} -dz -7
defineImage {http://www.nps.gov/webcams-glac/tmcam.jpg} {Glacier NP - Two Medicine, MT} -ll {48.49859,-113.34755} -dz -7
defineImage {http://www.nps.gov/webcams-yell/oldfaithvc.jpg} {Old Faithful, WY} -ll {44.45972,-110.83139} -dz -7
defineImage {http://nature.nps.gov/air/WebCams/parks/throcam/thro.jpg}  {Theodore Roosevelt National Park, ND} -ll {46.94889,-103.43306}  -dz -7
defineImage {http://www.nature.nps.gov/air/webcams/parks/bibecam/bibe.jpg}  {Big Bend National Park, TX} -ll {29.25,-103.25}  -dz -6
defineImage {http://www.nps.gov/webcams-olym/current_ridgecam.jpg}  {Olympic National Park, WA} -ll {47.934,-123.371}  -dz -8
defineImage {http://www.nature.nps.gov/air/webcams/parks/olymcam/olym.jpg} {Lake Crescent, WA}  -ll {48.094531,-123.805311}


defineImage {http://www.nature.nps.gov/air/WebCams/parks/moracam/mora.jpg}  {Mount Rainier National Park, WA} -ll {46.88333,-121.88333}  -dz -8
defineImage {http://www.nature.nps.gov/air/webcams/parks/nocacam/noca.jpg}  {North Cascades National Park, WA} -ll {48.66667,-121.25028}  -dz -8
defineImage {http://www.nature.nps.gov/air/WebCams/parks/porecam/pore.jpg}  {Point Reyes National Seashore, CA} -ll {38.06667,-122.88333}  -dz -8
defineImage {http://pixelcaster.com/yosemite/webcams/turtleback.jpg}  {Yosemite National Park, CA} -ll {37.85,-119.56667}  -dz -8
defineImage {http://www.nature.nps.gov/air/webcams/parks/jotrcam/jotr.jpg}  {Joshua Tree National Park, CA} -ll {35.12861,-116.03667}  -dz -8
defineImage {http://www.nature.nps.gov/air/WebCams/parks/nacccam/wash.jpg}  {Washington, D.C.} -ll {38.3,-77.0}  -dz -8

defineImage {http://images.webcams.travel/webcam/1341936012.jpg} {Baltimore,MD}  -ll {39.285848,-76.61311}  -dz -8

defineImage {http://wwc.instacam.com/instacamimg/FRSTB/FRSTB_l.jpg} {Frostburg,MD}  -ll {39.658142,-78.928357}  -dz -8



defineImage {http://www.nature.nps.gov/air/webcams/parks/grsmcam/grsm.jpg}  {Great Smoky Mountains National Park, NC} -ll {35.60056,-83.50889}  -dz -8
defineImage {http://www.nature.nps.gov/air/webcams/parks/grcacam/grca.jpg}  {Grand Canyon - Yavapai Point, AZ} -ll {36.06502,-112.11841}  -dz -8
defineImage {http://www.nature.nps.gov/air/WebCams/parks/denacam/dena.jpg}  {Denali National Park, AK} -ll {63.46,-150.87162}  -dz -10

defineImage {http://www.nature.nps.gov/air/webcams/parks/macacam/maca.jpg} {Mammoth Cave National Park, KY}  -ll {37.186699,-86.098999}
defineImage {http://hvo.wr.usgs.gov/cams/KIcam/images/M.jpg} {Hawaii Volcanoes National Park, HI}  -ll {19.428749,-155.253677}






defineGroup {West Coast}
defineImage http://cdn.abclocal.go.com/three/kgo/webcam/baybridge.jpg {San Francisco,CA} -ll {37.8181, -122.3467} -dz -8
defineImage http://livecams.ocscsailing.com/camera1.php?1418665898.158 {Berkeley,CA} -ll {37.8677, -122.3125} -dz -8

defineImage http://www.met.sjsu.edu/cam_directory/latest.jpg {San Jose State, CA} -ll {37.3044,-121.85} -dz -8
defineImage http://obs.astro.ucla.edu/images/towercam.jpg {Mt. Wilson, CA} -ll {34.22583,-118.05639} -dz -8
defineImage http://abclocal.go.com/three/kabc/webcam/web1-2.jpg?1144856196725 {Los Angeles, CA} -ll {34,-118.24} -dz -8
defineImage {http://hpwren.ucsd.edu/gphoto2-a2/CNMVCSD2/LAST.jpg} {San Diego, CA}  -ll {32.715685,-117.161724}
defineImage {http://images.webcams.travel/webcam/1393001576-Weather-Bass-Mtn-McColl.jpg} {McColl, CA}  -ll {40.7329276 -122.3672311}



defineImage http://www.monolake.org/livedata/camtwo.jpg {Mono Lake, CA} -ll {38.01667,-119.00833} -dz -8
defineImage http://www.gbuapcd.org/dustcam/keelerv1.jpg {Owens Lake, CA} -ll {36.501,-117.902} -dz -8





defineImage http://www.fs.fed.us/gpnf/volcanocams/msh/images01/volcanocamhd.jpg {Mount St. Helens, WA} -ll {46.272,-122.152} -dz -8
defineImage http://www.instacam.com/instacamimg/EUGNE/EUGNE_l.jpg {Eugene, OR} -ll {44.001778 -123.115917} -dz -8
defineImage http://archive.kgw.com/weather/images/core/webcams/rosecity.jpg {Portland, OR} -ll {45.516905,-122.659244} -dz -8
defineImage http://archive.king5.com/weather/images/core/webcam/queenanne.jpg {Seattle, WA} -ll {47.444,-122.317} -dz -8
defineImage http://134.121.11.63/images/bryan/bryan_00001.jpg {Pullman, WA} -ll {46.73193,-117.165365} -dz -8




defineGroup Alaska


defineImage http://www.borealisbroadband.net/DTN/dtnsemega.jpg {Anchorage, AK} -ll {61.21,-149.92} -dz {-9}
defineImage http://www.avo.alaska.edu/webcam/augtst.jpg {Augustine Volcano, AK} -ll {59.3626,-153.435} -dz {-9}




defineGroup Southwest
defineImage http://www.cs.arizona.edu/camera/view.jpg {Tuscon, AZ} -ll {32.22694,-110.96083}
defineImage http://www.wrh.noaa.gov/images/fgz/webcam/camera1.jpg {Bellemont, AZ} -ll {35.24,-111.84}

defineImage http://www.noao.edu/kpno/kpcam/lastim.jpg {Kitt Peak, AZ} -ll {31.9639671, -111.5998359}

defineImage http://www.eastmesaweather.com/cams/secam.jpg {Las Cruces, NM} -ll {32.3,-106.7}
defineImage http://www.cloudcroftwebcam.com/camera2.jpg {Cloudcroft, NM} -ll {32.957313,-105.742485}

defineImage http://wwc.instacam.com/instacamimg/KOBTV/KOBTV_l.jpg {Albuquerque, NM}  -ll {35.1029697, -106.6703963}

defineImage http://www.wxnation.com/livecam/tcam.jpg {Fort Worth, TX} -ll {32.725409, -97.3208496} -dz -6
defineImage http://www.atmo.ttu.edu/includes/Photo_N.jpg {Lubbock, TX} -ll {33.5777778, -101.8547222} -dz -6

defineImage http://www.nps.gov/webcams-glca/hc1.jpg {Halls Crossing, Lake Powell, UT} -ll {37.45694,-110.7122} -dz -7
defineImage http://www.nps.gov/webcams-glca/ww2.jpg {Wahweap, Lake Powell, UT} -ll {37.01278,-111.48944} -dz -7
defineImage http://www.neng.usu.edu/webcam/latest/latest_bldcam2.jpg {Logan, UT} -ll {41.73444,-111.85361} -dz -7
defineImage http://timecam.tv/upload_image_tc/metwest/MeteoWest.jpg {University of Utah at Salt Lake City, UT} -dz -7 -ll {40.76623,-111.84755}
defineImage http://www.nps.gov/webcams-zion/camera.jpg {Zion National Park, UT} -dz -7 -ll {37,-113}
defineImage http://www.redcliffslodge.com/webcam/show-image.php {Moab, UT} -dz -7 -ll {38.573316,-109.549839}
defineImage {http://www.wrh.noaa.gov/boi/images/BOI2-CAM.jpg} {Boise, ID}  -ll {43.60698,-116.193409}




defineGroup "Midwest"

defineImage http://128.252.163.102/SnapshotJPEG  {Saint Louis Univertiy, MO} -ll {38.637,-90.234} -dz -6
defineImage http://agwx.soils.wisc.edu/soils-agwx-assets/uwex_agwx/images/webcam/fullsize.jpg {Madison, WI} -ll {43.07980,-89.38751} -dz -6


defineImage http://images.webcams.travel/webcam/1249044867.jpg {Windsor, Canada} -dz -5 -ll {42.2833, -83.0}
defineImage http://cdn.abclocal.go.com/three/wls/webcam/Loopscape.jpg {Chicago, IL} -dz -6 -ll {41.8,-87.7}

defineImage http://www.starcitywebcam.com/images/cam1l.jpg {Lincoln, NE} -dz -6 -ll {40.7,-96.66}
defineImage http://wwc.instacam.com/instacamimg/KOTAT/KOTAT_l.jpg {Deadwood, SD}  -ll {44.3767, -103.7292}






defineGroup "Northeast"
defineImage http://www.hazecam.net/images/main/newark.jpg {Newark, NJ} -dz  -5 -ll {40.71,-74}
defineImage http://www.hazecam.net/images/photos-main/BLUEHILL.JPG {Boston, MA} -dz -5 -ll {42,-71}
defineImage http://wwc.instacam.com/instacamimg/WSKRG/WSKRG_l.jpg {Hartford, CT} -dz -5 -ll {41.74,-72.65}
defineImage http://hazecam.net/images/main/mtwash.jpg {Mount Washington, NH} -dz -5 -ll {44,-71}
defineImage http://hazecam.net/images/main/burlington_right.jpg {Burlington, VT} -dz -5 -ll {44,-73}
defineImage http://meteorology.lyndonstate.edu/webcam/LSCimage2.jpg {Lyndon State College, VT} -ll {44.53528,-72.02611} -dz -5

defineImage http://wwc.instacam.com/instacamimg/MLLRU/MLLRU_l.jpg {Millersville University of Pennsylvania, PA} -ll {40,-76.35} -dz -5
defineImage http://www.instacam.com/instacamimg/CALIF/CALIF_s.jpg {California University of Pennylvania, PA} -ll {40.066,-79.892} -dz -5


defineImage  http://wwc.instacam.com/instacamimg/CHRGW/CHRGW_l.jpg {Charleston, WV}  -ll {38.350195,-81.638989}


defineImage http://images.webcamgalore.com/webcam-Mechanicsburg-Pennsylvania-22678-08.jpg {Mechanicsburg, PA} -ll {40.2122, -77.0061} -dz -5
defineImage http://www.nature.nps.gov/air/WebCams/parks/acadcam/acad.jpg {Acadia National Park, ME} -dz -5 -ll {44.378,-68.2588}
defineImage http://vortex.plymouth.edu/webcam/1/latest.jpeg {Plymouth State University, NH} -dz -5 -ll {43.8,-71.7}




defineGroup {Southeast}
defineImage http://wwc.instacam.com/instacamimg/ATLGM/ATLGM_l.jpg {Atlanta, GA} -ll {33.74889,-84.38806} -dz -5
defineImage http://images.webcamgalore.com/webcam-Savannah-Georgia-24978-11.jpg {Savannah, GA} -ll {32,-81.1} -dz -5


defineImage http://images.webcams.travel/original/1232600976-Weather-Wannman-Cam,-Miami's-Biscayne-Bay-North-Bay-Village.jpg  {Miami, FL} -ll {25.5658, -80.2164} -dz -5
defineImage http://images.webcamgalore.com/14269-current-webcam-Tampa-Florida.jpg {Tampa, FL} -ll {27.84806,-82.26333} -dz -5

defineImage http://wwc.instacam.com/instacamimg/NPLHH/NPLHH_l.jpg {Naples, FL} -dz -5 -ll {26.145,-81.795}

defineImage http://video-monitoring.com/beachcams/boca/pics/s4/sep2416g/o061326e.jpg {Boca Raton, FL} -dz -5 -ll {26.3683, -80.1289}

defineImage http://images.webcamgalore.com/22141-current-webcam-Raleigh-North-Carolina.jpg {Raleigh, NC} -dz -5 -ll {35,-78}

defineImage http://www.spadre.com/southpadrebeachcam.jpg {South Padre Island, TX} -dz -5 -ll {26.1,-97.2}
defineImage http://webcams.galveston.com/docs/casadelmar2006/image.jpg {Galveston, TX} -dz -5 -ll {29.29,-94.79}

defineImage http://images.webcamgalore.com/webcam-New-Orleans-Louisiana-15268-11.jpg {La Nouvelle-Orleans, LA} -dz -6 -ll {29.951066,-90.071532}
defineImage http://www.kxul.com/cgi-bin/campuscam.cgi?large {Monroe, LA} -dz -6 -ll {32.509311,-92.119301}

defineImage http://dnr.mo.gov/env/esp/aqm/images/KCCamera.jpg {Kansas City, MO}  -ll {39.10296,-94.583062}



defineImage {http://images.webcams.travel/original/1349806811-Weather-North-Dakota-Grand-Forks-City-Hall-Grand-Forks.jpg} {Grand Forks, ND}  -ll {47.924085,-97.032034}


defineImage http://www.scottcountyiowa.us/webcams/cams/admin-south/recent/image00200.jpg {Scott County, IA}  -ll {41.61293,-90.606277}


defineImage http://i.imgur.com/XV8DGFRl.jpg  {Stillwater, OK} -dz -5 -ll {36.115607,-97.058368}

defineImage http://webcam.icorp.net/ar1.jpg {Metairie, LA} -dz -5 -ll {29.98,-90.15}

defineGroup {Antarctica}
defineImage http://www.esrl.noaa.gov/gmd/webdata/spo/webcam/cmdlfullsize.jpg {South Pole}   -ll {-90, -105}
defineGroup {Europe}
defineImage http://www.ascona-locarno.com/docroot/site-ascona-locarno/img/element/webcams/LC.jpg {Locarno, Switzerland}  -dz +1 -ll {46.2, 8.8}
defineImage http://images.webcamgalore.com/webcamimages/webcam-000700.jpg {Scheveningen, Netherlands}  -dz +1 -ll {52.1081, 4.2731}

defineGroup {Asia}
defineImage http://www.weather.gov.hk/wxinfo/aws/hko_mica/cp1/latest_CP1.jpg {Hong Kong, China}  -dz +8 -ll {22.3, 114.18}
