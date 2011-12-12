<?xml version="1.0" encoding="UTF-8"?>

<resourcebundle  name="Default"> 

  <!--The different user interfaces available  -->
  <resources name="idv.resource.skin" removeprevious="true">

    <resource
       location="%IDVPATH%/skins/twoviewskin.xml"
       label="Transect and Map">
     <property name="left_view_class" value="ucar.unidata.idv.TransectViewManager"/>
     <property name="right_view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       location="%IDVPATH%/skins/skin.xml"
       label="One Transect">
     <property name="view_class" value="ucar.unidata.idv.TransectViewManager"/>
    </resource>


    <resource
       location="%IDVPATH%/skins/skin.xml"
       label="One Map">
     <property name="view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>



    <resource
       location="%IDVPATH%/skins/twoviewskin.xml"
       label="Misc&gt;Two Maps">
     <property name="left_view_class" value="ucar.unidata.idv.MapViewManager"/>
     <property name="right_view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       location="%IDVPATH%/skins/threeviewskin.xml"
       label="Misc&gt;Three Maps">
     <property name="view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>
    <resource
       location="%IDVPATH%/skins/fourviewskin.xml"
       label="Misc&gt;Four Maps">
     <property name="view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       location="%IDVPATH%/skins/twoviewskin.xml"
       label="Misc&gt;Two Transects">
     <property name="left_view_class" value="ucar.unidata.idv.TransectViewManager"/>
     <property name="right_view_class" value="ucar.unidata.idv.TransectViewManager"/>
    </resource>


    <resource
       location="%IDVPATH%/skins/threeviewskin.xml"
       label="Misc&gt;Three Transects">
     <property name="view_class" value="ucar.unidata.idv.TransectViewManager"/>
    </resource>
    <resource
       location="%IDVPATH%/skins/fourviewskin.xml"
       label="Misc&gt;Four Transects"> 
    <property name="view_class" value="ucar.unidata.idv.TransectViewManager"/>
    </resource>



    <resource
       location="%IDVPATH%/skins/oneandtwoskin.xml"
       label="Misc&gt;1 Transect and 2 Maps">
     <property name="left_view_class" value="ucar.unidata.idv.TransectViewManager"/>
     <property name="right_upper_view_class" value="ucar.unidata.idv.MapViewManager"/>
     <property name="right_lower_view_class" value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       location="%IDVPATH%/skins/oneandtwoskin.xml"
       label="Misc&gt;2 Transects and  1 Map">
     <property name="left_view_class" value="ucar.unidata.idv.MapViewManager"/>
     <property name="right_upper_view_class" value="ucar.unidata.idv.TransectViewManager"/>
     <property name="right_lower_view_class" value="ucar.unidata.idv.TransectViewManager"/>
    </resource>





  </resources>


  <!-- Defines the image set xml  -->
  <resources name="idv.resource.imagesets">
    <resource location="http://www.unidata.ucar.edu/software/idv/projects/trex/products/trexproducts.xml"/>
  </resources>


</resourcebundle>
