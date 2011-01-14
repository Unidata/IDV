<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="false" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <pause seconds="5"/>

<!-- Specify a rotation, translation and scale. These values are shown in the Aspect Ratio tab of the View Manager properties dialog  -->
  <viewpoint  rotx="75" roty="62" rotz="-3.3" scale="0.399" transx="0.0" transy="0.0" transz="0.0"/>
  <pause seconds="5"/>

<!-- You can also set the aspect ratio  -->
  <viewpoint  aspectx="2" aspecty="5" aspectz="10"/>
  <pause seconds="5"/>

<!-- You can also specify a tilt and azimuth. This is the same as you can do interactively in the viewpoint dialog -->
  <viewpoint  tilt="45" azimuth="180"/>
  <pause seconds="5"/>

</isl>



