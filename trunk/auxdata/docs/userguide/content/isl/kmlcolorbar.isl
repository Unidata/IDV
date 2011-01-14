<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/testtwoview.xidv" wait="true"/>
  <image file="${islpath}/test.kmz">
  <kmlcolorbar  width="400" height="20"
        showlines="true" tickmarks="4"  fontsize="12" background="white" color="black"
        file="${islpath}/testcolorbar.png" space="20" suffix=" %unit%"
        kml.name="Color bar"
        kml.overlayXY.x="0" kml.overlayXY.y="1" kml.overlayXY.xunits="fraction" kml.overlayXY.yunits="fraction"
        kml.screenXY.x="10" kml.screenXY.y="1" kml.screenXY.xunits="pixels" kml.screenXY.yunits="fraction"/>
  </image>
  
</isl>
