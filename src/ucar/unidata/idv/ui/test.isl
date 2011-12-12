<?xml version="1.0" encoding="UTF-8"?>



<isl debug="true" offscreen="true" xxxonerror="ignore">


  <echo message="test message"/>
  <stop/>



<!--
  <bundle file="test.xidv"/>
-->

  <movie   file="foo.mov" imagedir="moviedir" imagetemplate="image_%count%_%yyyyMMddHHmm%"/>
  <stop/>



  <image file="test.jpg"/>





  <bundle file="test.xidv">
    <setfiles datasource="class:TrackDataSource" file="/upc/share/metapps/test/data/track/wmi_lear1.nc"/>
  </bundle>
  <pause seconds="60"/>
  <image file="test.jpg"/>
  <echo message="ftping"/>
  <stop/>



  <echo message="${G}, ${yy}, ${yyyy}, ${MM}, ${HH}, ${mm}, ${ss}, ${EEE},${Z}"/>
  <stop/>


<!--
  <movie   file="foo.mov">
      <fileset dir="." pattern="test.*\.png$"/>
  </movie>
-->




<pause seconds="10"/>
<echo message="centering"/>
<center display="class:ucar.unidata.idv.control.ColorPlanViewControl"/>
<pause seconds="60"/>




<output
     file="output.html">
<image file="fourth_${viewindex}.jpg">
<output>
<![CDATA[
<img src="fourth_${viewindex}.jpg">fourth_${viewindex}.jpg<br>
]]>
</output>
</image>
</output>
<stop/>


<group loop="2">
   <echo message="${loopindex}"/>
   <bundle file="testtimes.xidv" times="${loopindex}"/>
   <pause/>
   <image file="test${loopindex}.png">
        <overlay
           image="logo.gif"
           place="lr,-5,-5"
           anchor="lr"/>
    </image>
</group>



<stop/>



<wait seconds="1">
   <fileset file="tmp1.png"/>
   <fileset file="tmp2.png"/>
</wait>

<echo message="after"/>
<stop/>

<group loop="3">
   <ask message="continue" property="bar" default="true"/>
   <if expr="${bar}==0">
       <break/>
   </if>
</group>
<stop/>



<echo message="centering"/>
<center north="40" south="35" west="-90" east="-85"/>
<pause seconds="20"/>
<stop/>



<group loop="3">
    <foreach value1="40,30,20" value2="-100,-90,-80">
        <center lat="${value1}" lon="${value2}"/>
        <pause seconds="5"/>
    </foreach>
    <center/>
    <pause seconds="5"/>
    <center north="40" south="35" west="-90" east="-85"/>
    <pause seconds="5"/>
</group>

<stop/>



<mkdir file="moviedir"/>

<output
     template="file:template.html"
     template:contents="file:entry.html"
     file="output.html">
    <movie imagedir="moviedir">

       <thumbnail file="moviedir/thumb${imageindex}.png" width="20%"/>
       <output  thumbfile="moviedir/thumb${imageindex}.png" caption="Some caption"/>
   </movie>
</output>


<stop/>

<output
     template="file:template.html"
     template:contents="file:entry.html"
     file="output.html">
    <image file="test.png">
        <clip file="testclip.png" top="0%" bottom="100%" left="10%" right="50%" copy="true"/>
        <thumbnail file="testclip_thumb.png" width="25%"/>
    </image>
    <output imagefile="testclip.png" thumbfile="testclip_thumb.png" caption="Some caption"/>
</output>



<procedure name="makeImage">
    <image file="${filename}">
        <thumbnail width="250"/>
    </image>
</procedure>




<if>
<![CDATA[1<5]]>
<then>
   <echo message="the then part"/>
</then>
</if>




<display type="rangerings"/>
<pause/>
<call name="makeImage" filename="image1.png"/>

<mkdir file="tmp"/>


<copy dir="tmp">
   <fileset dir="." pattern="\.png$"/>
</copy>

<delete>
    <fileset dir="tmp" pattern="\.png$"/>
</delete>







<stop/>

<group loop="10">
    <bundle file="test.xidv" width="jython:(${loopindex}+1)*50" height="500"/>
    <pause/>
    <image file="/home/jeffmc/images/test${loopindex}.gif"/>
    <echo message="Generated image ${loopindex} on ${EEE} at  ${HH}:${mm}"/>
</group>    
<stop/>


<!--
  <pause every="12"/>
  <pause every="1.5"/>
  <pause every=".25"/>
  <pause every="6.0"/>
-->









<join columns="10">
   <fileset dir="tmp" pattern="\.gif$"/>
</join>


  <property name="foo" value="2"/>

<!--
  <movie   file="animated.gif" >
      <fileset file="test1.png"/>
      <fileset file="test{$foo}.png"/>
  </movie>
-->

<stop/>     



<bundle file="test.xidv"/>
<pause/>


<movie 
      file="somefile.gif" 
      appendtime="true"
      imageprefix="image"
        imagesuffix="png">
        <matte bottom="150"/>
        <overlay
           image="logo.gif"
           place="lr,-5,-5"
           anchor="lr"/>
  </movie>


  <stop/>


  <image file="test.png">
     <transparent color="black"/>
  </image>
  <stop/>

  <image file="test.png">
    <split file="split${cnt}.png" rows="3" columns="3">
        <matte space="5" background="red"/>
    </split>
    <thumbnail width="200">
        <matte space="5" background="blue"/>
    </thumbnail>
    <clip top="1" bottom="-1" left="-1" right="1"/>
    <matte left="100" bottom="150"/>
    <colorbar
       width="25"
       height="300"
       orientation="left"
       showlines="true"
       tickmarks="5"
       place="ul,100,30"
       anchor="ul"
       fontsize="18"
       color="green"/>
    <colorbar
       width="25"
       height="300"
       orientation="right"
       showlines="true"
       tickmarks="5"
       place="ul,0,10"
       anchor="ul"
       fontsize="18"
       color="blue"/>
    <colorbar
       width="300"
       height="25"
       orientation="top"
       showlines="true"
       tickmarks="5"
       fontsize="8"
       place="lm,10,-40"
       color="red"/>
    <colorbar
       width="300"
       height="25"
       orientation="bottom"
       showlines="true"
       tickmarks="5"
       fontsize="18"
       fontface="monospaced"
       place="lm,-10,-40"
       anchor="lr"
       color="blue"/>
    <matte space="2" background="white"/>
    <matte space="1" background="black"/>
  </image>


  <stop/>
  <output
     template="file:template.html"
     image:entry="foo"
     entry="file:entry.html"
     file="output.html">
    <group
       loop="3"
       xsleep="10"
       debug="true">
      <image file="test_${loopindex}_${year}_${month}_${monthname}_${dayname}_${dayofweek}_${day}_${hour}_${minute}_${second}.png">
        <overlay
           image="logo.gif"
           place="lr,-5,-5"
           anchor="lr"/>
        <overlay
           text="Generated with the IDV-${loopindex} anim:${animationtime}"
           place="lm"
           anchor="lm"
           color="black"
           background="white"/>
        <thumbnail width="200"/>
      </image>
    </group>
  </output>
  <echo message="DONE"/>
<!--
  <datasource url="foo" id="datasource" />
    <reload/>
 -->
<!--
  <center/>
 -->
<!--

 -->
  <pause seconds="10"/>
  <center
     north="40"
     south="30"
     east="-90"
     west="-100"/>
  <pause seconds="20"/>
  <center
     lat="10"
     lon="-107"/>
  <pause seconds="10"/>
  <center
     north="39"
     south="41"
     east="-106"
     west="-107"/>
  <pause seconds="10"/>
<!--
  <image 
        file="test.png" />
  <stop/>
 -->
<!--
  <movie 
        file="somefile.mov" 
        appendtime="true"
        imagedir="/home/jeffmc/test/group"
        imageprefix="image"
        imagesuffix="png"/>
 -->
</isl>
