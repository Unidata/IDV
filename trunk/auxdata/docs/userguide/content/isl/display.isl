<isl>

<!-- Create   a datasource. Note: you can also have a template attribute
     that points to a bundle xidv file that was saved from a data source:
     bundle="${islpath}/datasource.xidv"
-->

  <datasource 
     url="dods://motherlode.ucar.edu:8080/thredds/dodsC/casestudies/idvtest/grids/small_ruc_grid.nc"
     id="datasource1">
<!-- Set the name on the data source  -->
    <property
       name="name"
       value="Example data source"/>
<!-- Create a display of RH. Here we load in the display from a template.  -->
<!--
    <display
       param="RH" template="${islpath}/template.xidv">
      <property
         name="id"
         value="display1"/>
      <property
         name="displayCategory"
         value="Category 1"/>
      <property
         name="legendLabelTemplate"
         value="%datasourcename% - %shortname%"/>
    </display>
-->


<!-- Create a display of T. Here we create the display from the type -->
    <display
	type="planviewcontour"
      param="T">
      <property
         name="displayCategory"
         value="Category 1"/>
<!--
        The contour info can be set with: interval;base;min;max
              <property name="contourInfoParams" value="10;40;-30;100"/>
         Or it can have names in it:
-->
      <property name="contourInfoParams" value="interval=10;base=40;min=-30;max=100;dashed=true;labels=false"/>

      <property
         name="legendLabelTemplate"
         value="%datasourcename% - %shortname%"/>

<!-- This sets the level to be 500 hectopascals -->
<!-- Note: this can also be of the form #<index>, eg. #4 will select the 5th level (this is zero based) -->

      <property
         name="dataSelectionLevel"
         value="500[hectopascals]"/>


    </display>
  </datasource>


<!-- Set the projection to the data projection  of the display we created above  -->
<!--
  <center
     display="display1"
     useprojection="true"/>
-->

</isl>
