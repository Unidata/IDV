<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/radar.xidv"/>
  <pause/>
  <displayproperties display="class:ucar.unidata.idv.control.ColorRhiControl">
      <property name="EndPoint"  value="45.0"/>
  </displayproperties>
  <pause/>
  <image file="${islpath}/radar.png" what="rhi" display="class:ucar.unidata.idv.control.ColorRhiControl"/>
</isl>
