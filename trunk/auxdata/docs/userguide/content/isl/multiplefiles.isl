<isl offscreen="false">
   <fileset  pattern=".*\.nc">
     <echo message="File: ${file}"/>
     <bundle file="${islpath}/test.xidv">
<!-- This sets the data file in the bundle to the ${file} -->
	<setfiles datasource=".*" file="${file}"/>
     </bundle>
     <pause seconds="20"/>
   </fileset>
</isl>