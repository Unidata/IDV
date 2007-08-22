<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true">

<!-- Create   a datasource. Note: you can also have a template attribute
     that points to a bundle xidv file that was saved from a data source:
     bundle="${islpath}/datasource.xidv"
 -->
  <datasource
     id="datasource1"
     url="dods://motherlode.ucar.edu:8080/thredds/dodsC/casestudies/idvtest/grids/small_ruc_grid.nc"/>

<!--
We could list out all of the param names using:
  <foreach param="interp.fields('datasource1','U')">
     <echo message="param: ${param}"/>
  </foreach>
-->


  <foreach param="T,RH">
    <display
       datasource="datasource1"
       param="${param}"
       type="planviewcolor">
      <property
         name="id"
         value="display1"/>
      <property
         name="dataSelectionLevel"
         value="500[hectopascals]"/>
    </display>


<!-- Set the projection to the data projection  of the display we created above   -->
    <center
       display="display1"
       useprojection="true"/>

<!-- Write the image -->
    <movie file="test${param}.gif">
	<matte bottom="50"  background="white"/>
	<overlay text="Parameter: ${param} @ ${anim:yyyy}-${anim:mm}-${anim:dd} ${anim:HH}:${anim:mm}"
		 place="LM,0,-10"
		 anchor="LM"
                 fontsize="16"
 		 color="black"/>
    </movie>

<!-- Remove this display -->
    <removedisplays display="display1"/>
  </foreach>
</isl>
