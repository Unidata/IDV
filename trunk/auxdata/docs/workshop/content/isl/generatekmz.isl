<?xml version="1.0" encoding="ISO-8859-1"?>
<isl
   loop="1"
   offscreen="false"
   debug="true">

<!-- This is where we put the web content  -->
  <property
     name="wwwdir"
     value="http://www.unidata.ucar.edu/software/idv/examples/kml"/>

<!-- 
    Define the procedure to load in the bundle, generate the images, etc.  
    The transparency attribute is one of the default arguments used
-->
  <procedure
     name="makekmz"
     transparency="none">

<!-- Load in the bundle  -->
    <bundle file="${islpath}/${basename}.jnlp"/>

<!-- wait  for the displays to get loaded -->
    <pause/>

<!-- Generate the movie  -->
    <movie
       file="${islpath}/${basename}.kmz"
       imagesuffix="png"
       kml_name="${kmzname}"
       kml_visibility="0"
       kml_open="0">

<!-- Make the given color transparent -->
      <transparent color="${transparency}"/>
    </movie>

<!-- scp the files to the web site  -->
<!--
    <exec command="scp ${islpath}/${basename}.kmz conan:/content/software/idv/examples/kml"/>
    <exec command="scp ${islpath}/${basename}.jnlp conan:/content/software/idv/examples/kml"/>
-->

<!-- Append to the kml property the contents of the network link  file -->
    <append name="kml" fromfile="networklink.kml"/>
  </procedure>

<!-- Import the list of bundles. This calls the above procedure -->
  <import file="${islpath}/bundles.isl"/>

<!-- Write out the kml -->
  <output file="${islpath}/idvproducts.kml">
<!-- Write out the header -->
    <output fromfile="${islpath}/header.kml"/>

<!-- Write out the contents -->
    <output text="${kml}"/>

<!-- Write out the footer -->
    <output fromfile="${islpath}/footer.kml"/>
   </output>

<!-- scp it -->
<!--
  <exec command="scp ${islpath}/idvproducts.kml conan:/content/software/idv/examples/kml"/>
-->
</isl>
