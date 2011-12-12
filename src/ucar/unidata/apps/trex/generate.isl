<?xml version="1.0" encoding="UTF-8"?>

<isl debug="true" offscreen="false">
  <property name="quality" value="1.0"/>
  <group>
    <bundle file="${islpath}/test.xidv"/>
    <group onerror="ignore">    
    <echo message="centering"/>
    <center view="class:MapViewManager" display="class:TrackControl"/>
    <echo message="ok"/>
    </group>    
    <property name="dttm" value="${yyyy}${MM}${dd}${HH}${mm}"/>
    <property name="file1" value="research.idv.${dttm}.test1.jpg"/>
    <property name="file2" value="research.idv.${dttm}.test2.jpg"/>
    <echo message="capturing"/>
    <image file="map.png"  quality="1.0" view="class:ucar.unidata.idv.MapViewManager">
       <resize width="250"/>
<!--       <transparent color="black"/> -->
    </image>

    <image file="${islpath}/${file1}"  quality="${quality}" view="class:ucar.unidata.idv.TransectViewManager">
       <matte left="250" color="white"/>
       <overlay image="/ucar/unidata/apps/trex/idvlogo.gif" place="ll" anchor="ll"/>
       <overlay image="map.png" place="ul" anchor="ul"/>
    </image>
<!--
    <image file="${islpath}/${file2}"  quality="${quality}" view="class:ucar.unidata.idv.MapViewManager">
       <overlay image="/ucar/unidata/apps/trex/idvlogo.gif" place="ll" anchor="ll"/>
    </image>
-->

    <echo message="done capturing"/>
<!--
    <echo message="ftping"/>
    <ftp server="ftp.atd.ucar.edu" user="anonymous" password="idvuser@ucar.edu" file="${islpath}/${file1}" destination="/pub/temp/field/incoming/other/trex_${file1}"/>
    <ftp server="ftp.atd.ucar.edu" user="anonymous" password="idvuser@ucar.edu" file="${islpath}/${file2}" destination="/pub/temp/field/incoming/other/trex_${file2}"/>

    <ftp server="ftp.joss.ucar.edu" user="anonymous" password="idvuser@ucar.edu" file="${islpath}/${file1}" destination="/pub/incoming/catalog/trex/${file1}"/>
    <ftp server="ftp.joss.ucar.edu" user="anonymous" password="idvuser@ucar.edu" file="${islpath}/${file2}" destination="/pub/incoming/catalog/trex/${file2}"/>

    <echo message="done ftping"/>
-->
  </group>
</isl>
