<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="false" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <pause seconds="5"/>
  <echo message="setting ct"/>
  <displayproperties display="test">
     <property name="colorTableName" value="Red"/>
  </displayproperties>
  <pause seconds="5"/>
  <image file="${islpath}/test.png"/>
</isl>
