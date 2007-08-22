<isl debug="true" offscreen="false">
     <bundle file="${islpath}/isl.xidv"/>
     <pause/>

     <!-- Make an image -->
     <image file="${islpath}/clipped.png">
          <!-- Clip it at the lat/lon box -->
          <clip north="60" south="20" east="-60" west="-140"/>

          <!-- resize it -->
          <resize width="400"/>

          <!-- Make a thumbnail -->
          <thumbnail file="${islpath}/clipped_thumb.png" width="25%"/>
     </image>

     <!-- Make another image -->
     <image file="${islpath}/matted.png">
          <!-- add a logo overlay -->
          <overlay image="http://www.unidata.ucar.edu/software/idv/logo.gif"
                   place="LL,10,-10"
                   anchor="LL"/>

          <!-- Matte the image -->
          <matte background="red" bottom="100" top="100"/>

          <!-- Overlay some text -->
          <overlay text="Workshop Example" place="LM,0,-10" anchor="LM" color="blue" fontsize="24"/>

     </image>
</isl>
