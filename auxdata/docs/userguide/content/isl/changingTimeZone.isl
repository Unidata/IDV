<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <displayproperties display="class:ucar.unidata.idv.control.ContourPlanViewControl">
            <property name="DisplayListTimeZone"  value="MST"/>
  </displayproperties>
  <pause/>
  <image file="${islpath}/test.png"/>
  <displayproperties display="class:ucar.unidata.idv.control.ContourPlanViewControl">
            <property name="DisplayListTimeZone"  value="EST"/>
  </displayproperties>
  <pause/>
  <image file="${islpath}/test1.png"/>
</isl>
