<?xml version="1.0" encoding="ISO-8859-1"?>
<resourcebundle name="Default">

<!--The different user interfaces available   -->
  <resources name="idv.resource.skin">
    <resource
       skinid="idv.skin2d.twoview.map"
       label="Map View&gt;2D&gt;Two Panes"
       location="/twoviewskin2d.xml">
      <property
         name="left_view_class"
         value="ucar.unidata.idv.MapViewManager"/>
      <property
         name="right_view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin2d.threeview.map"
       label="Map View&gt;2D&gt;Three Panes"
       location="/threeviewskin2d.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>

    <resource
       skinid="idv.skin2d.fourview.map"
       label="Map View&gt;2D&gt;Four Panes"
       location="/fourviewskin2d.xml">
      <property
         name="view_class"
         value="ucar.unidata.idv.MapViewManager"/>
    </resource>


  </resources>

</resourcebundle>
