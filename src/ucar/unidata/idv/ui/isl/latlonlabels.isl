<?xml version="1.0" encoding="ISO-8859-1"?>
<isl debug="true" loop="1" offscreen="true" sleep="60.0minutes">
  <bundle clear="true" file="${islpath}/test.xidv" wait="true"/>
  <image file="${islpath}/test.png">
<!-- 
Note: none of these attributes are required and the "xxx" attributes are just commented out

lonvalues/latvalues - Comma separated list of lonvalues and latvalues.

lonlabels/latlabels - An optional list of  labels to use instead of the values

format - A decimal format for formatting the lat/lons if you don't
specify the lonlabels/latlabels.

labelbackground -  if defined, will be the background  color of the labels.

top/bottom/left/right - This is the matte-ing of the image.
If a value is undefined or 0 then the label is shown on the inside of the map image.
If non-zero then the label is shown on the outside of the map image.

background  - background color of the matted border

showleft/showright/showtop/showbottom - controls what labels are shown

drawlonlines/drawlatlines - draw lines across the map

linewidth/linecolor - line attributes

dashes - comma separated list of line segment lengths. format is:
opaque length1, transparent length1,opaque length2, transparent length2,...
defaults to solid line

lineoffsetleft/lineoffsetright/... - offsets when drawing lat/lon lines
defaults to 0

-->

    <latlonlabels 
       lonvalues="-160,-140,-120,-100,-80,-60,-40,-20,0,20,40,60,80,100,120,140,160" 
       xxxlonlabels="a,b,c,d" 
       latvalues="-80,-60,-40,-20,0,20,40,60,80" 
       xxxlatlabels="a,b,c,d" 

       format="##0"
       xxxlabelbackground="white"

       background="gray"
       top="30"
       bottom="0"
       left="30"
       right="0"

       showleft="true"
       showright="true"
       showtop="true"
       showbottom="true"

       drawlonlines="true" 
       drawlatlines="true"
       linewidth="1"
       linecolor="green"
       dashes="2,10"

       lineoffsetleft="0"
       lineoffsetright="0"
       lineoffsettop="0"
       lineoffsetbottom="0"

       />

  </image>
</isl>
