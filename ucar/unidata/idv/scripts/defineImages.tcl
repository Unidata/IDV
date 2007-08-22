
##format:
##defineImage url {group>name} [-id <id>] [-desc <desc>] [-group <group>]
##
##If there are any spaces in an argument enclose it withing brackets
##
##If a group is not given as a -group arg then we look at the name
##and extract out the group with  ">"  as a delimiter
##If no group still we use the most recent one defined by the defineGroup call
##
##If no -desc provided we use {From: <hostname>}




defineGroup Rockies
defineImage http://9news.com/9live/images/cams/pic2.jpg {Boulder, CO} -ll {40.0,-105.27} -dz -7

defineImage http://9news.com/9live/images/cams/pic1.jpg {Denver, CO} -dz -7 -ll {39.75,-105}

defineImage http://www.cotrip.org/rWeather/camera/60002.jpg {Eisenhower Tunnel, CO} -ll {39.68,-105.9} -dz -7

defineImage http://www.pikespeakcam.com/images/cam.jpg {Pikes Peak, CO} -ll {38.84056,-105.04389} -dz -7


defineImage http://www.srv.net/%7Ejpat/tetonl.jpg {Grand Tetons, WY} -ll {43.74111,-110.80167} -dz -7



defineGroup {West Coast}
defineImage http://sv.berkeley.edu/view/images/current_view.jpg {Berkeley,CA} -ll {37.86972,-122.25778} -dz -8
defineImage http://www.astro.ucla.edu/~obs/images/towercam.jpg {Mt. Wilson, CA} -ll {34.22583,-118.05639} -dz -8
defineImage http://www.fs.fed.us/gpnf/volcanocams/msh/images/mshvolcanocam.jpg {Mount St. Helens, WA} -ll {46.52306,-122.81194} -dz -8


defineGroup Alaska
defineImage http://akweathercams.faa.gov/wxdata/7-61294.jpg {Anchorage, AK} -ll {61.21,-149.92} -dz {-9}
defineImage http://akweathercams.faa.gov/wxdata/35-62245.jpg {Denali, AK} -dz -9 -ll {63.43,-149.54}





defineGroup Southwest
defineImage http://www.cs.arizona.edu/camera/view.jpg {Tuscon, AZ} -ll {32.22694,-110.96083}
defineImage http://www.wfaa.com/s/dws/img/standing/cams/wfaahuge.jpg {Dallas,TX} -ll {32.775,-96.79667} -dz -6
defineImage http://kviiwebftp.ftp.clickability.com/TowerB.jpg {Amarillo,TX} -ll {35.18417,-101.69111} -dz -6

defineImage http://www.met.utah.edu/jhorel/images/cameras/wbb/current.jpg {Logan, UT} -ll {41.73444,-111.85361} -dz -7
defineImage http://www.ewcd.org/webcams/images/Castle_Dale_N_View/800.jpg {Castle Dale,UT} -ll {39.222,-111.02} -dz -7
defineImage http://www.wrh.noaa.gov/images/slc/camera/latest/slc.north.latest.jpg {Salt Lake City, UT} -dz -7 -ll {40,-111}
defineImage http://instacam.com/instacamimg/ZPRNG/ZPRNG_S.jpg {Zion National Park, UT} -dz -7 -ll {37,-113}



defineGroup "Midwest"
defineImage http://www.madison.com/img/weather/camera.img  {Madison,WI} -ll {43.07980,-89.38751} -dz -6
defineImage http://web.wxyz.com/towercam.JPG {Southfield, MI} -dz -5  -ll {42,-83}
defineImage http://www.glerl.noaa.gov/metdata/mkg/lmfs3.jpg {Muskegeon, MI} -dz -5 -ll {43,-86}
defineImage http://www.glerl.noaa.gov/metdata/chi/chicago.jpg {Chicago, IL} -dz -6 -ll {41,-87}
defineImage http://www.glerl.noaa.gov/metdata/tol2/tol2-1.jpg {Toledo, OH} -dz -5 -ll {41,-83}





defineGroup "Northeast"
defineImage http://www.hazecam.net/images/photos-main/newark.jpg {Newark,NJ} -dz  -5 -ll {40.71,-74}
defineImage http://www.hazecam.net/images/photos-main/BLUEHILL.JPG {Boston,MA} -dz -5 -ll {42,-71}
defineImage http://www.hazecam.net/images/photos-main/HARTFORD.JPG {Hartford,CT} -dz -5 -ll {41,-72}
defineImage http://www.hazecam.net/images/photos-main/MTWASH.JPG {Mount Washington, NH} -dz -5 -ll {44,-71}
defineImage http://www.hazecam.net/images/photos-main/BURLINGTON.JPG {Burlington, VT} -dz -5 -ll {44,-73}
defineImage http://apollo.lsc.vsc.edu/webcam/LSCimage.jpg {Lyndon State College, VT} -ll {44.53528,-72.02611} -dz -5
defineImage http://www.gettysburgaddress.com/GIFS/battle.jpg {Gettysburg,PA} -ll {39.82111,-77.22361} -dz -5
defineImage http://www.hazecam.net/images/photos-main/ACADIA.JPG {Acadia Nation Park, ME} -dz -5 -ll {44,-68}





defineGroup {Southeast} 
defineImage http://www.wtvt.com/wx2/cam.jpg {Tampa,FL} -ll {27.84806,-82.26333} -dz -5
defineImage http://www.erh.noaa.gov/rah/weathercam/wxcam.image.jpg {Raleigh, NC} -dz -5 -ll {35,-78}





defineGroup {World}
defineImage http://www.cmdl.noaa.gov/obop/spo/images/cmdlfullsize.jpg {South Pole}  
