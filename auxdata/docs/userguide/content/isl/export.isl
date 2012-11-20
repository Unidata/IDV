<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <export file="/tmp/test" what="csv, netcdf" display="class:ucar.unidata.idv.control.ObsListControl"/>
</isl>
