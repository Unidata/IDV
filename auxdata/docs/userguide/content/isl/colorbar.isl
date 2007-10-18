<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/colorbar.xidv" wait="true"/>
  <image file="${islpath}/colorbar.png">
     <matte space="100" background="gray"/>
     <colorbar display="planview" orientation="top" tickmarks="3" width="400" showlines="true"  anchor="LM" place="UM,0,100" showunit="true"/>
     <colorbar display="planview" orientation="bottom" tickmarks="3" width="400" showlines="true"  anchor="UM" place="LM,0,-100" showunit="true"/>
     <colorbar display="planview" orientation="top" tickmarks="3" width="400" showlines="true"  anchor="LM" place="LM" showunit="true"/>
     <colorbar display="planview" orientation="left" tickmarks="3" width="20" height="400" showlines="true"  anchor="MR" place="ML,100,0" showunit="true"/>
     <colorbar display="planview" orientation="left" tickmarks="3" width="20" height="400" showlines="true"  anchor="MR" place="MR" showunit="true"/>
  </image>
</isl>
