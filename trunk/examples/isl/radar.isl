<isl debug="true" offscreen="true">

    <append name="html"><![CDATA[
<html><title>IDV Radar Images</title>
<body>
<table width="100%"><tr>
]]>
       </append>

<procedure name="doradar" label="">

    <append name="html"><![CDATA[
<h3>${label}</h3>
<table width="100%"><tr>
]]>
       </append>

    <property       name="imagecount"       value="0"       global="true"/>

   <foreach radarid="${radarids}" radarname="${radarnames}">
     <makeradar/>
   </foreach>
    <append name="html"><![CDATA[
</table>
]]>
       </append>

</procedure>


<procedure name="makeradar">
       <bundle file="${islpath}/radar.xidv"/>
       <center display="radardisplay" useprojection="false"/>
       <display type="rangerings">
           <property name="showInDisplayList" value="false"/>
           <property name="initStationLocation" value="${radarid}"/>
       </display>
       <image file="${islpath}/${radarid}.png">
            <resize width="300"/>
<!--            <matte space="5" background="blue"/>-->
<!--            <transparent color="black"/> -->
       </image>
       <movie file="${islpath}/${radarid}.gif,${islpath}/${radarid}.kmz" imagesuffix="png">
            <transparent color="black"/>
       </movie>


       <exec command="scp ${islpath}/${radarid}.png conan:/content/software/idv/examples/radar"/>
       <exec command="scp ${islpath}/${radarid}.gif conan:/content/software/idv/examples/radar"/>
       <exec command="scp ${islpath}/${radarid}.kmz conan:/content/software/idv/examples/radar"/>

       <increment name="imagecount"/>
       <if expr="${imagecount}&gt;2">
           <append name="html"><![CDATA[
</tr>
<tr><td>&nbsp;<p></td></tr>
<tr valign="top">
]]></append>
      <replace
         name="imagecount"
         value="1"/>
</if>


       <append name="html"><![CDATA[
<td>
<table><tr valign="top"><td width="75%">
<img src="${radarid}.png" border="0"><br>${radarname} (${radarid})
<br>(<i><span style="font-size:8pt;">${MMMMM} ${d}, ${yyyy} ${HH}:${mm} ${Z}</span></i>)
</td><td align="left">
<a href="${radarid}.gif">Animated GIF</a><br>
<a href="${radarid}.kmz">Google Earth</a>
</td><td>&nbsp;&nbsp;&nbsp;</td>
</tr></table>
</td>
]]>
       </append>

</procedure>




    <doradar label="Northeast"  radarids="CBW,        BOX,   OKX,          BUF,    DIX,         PBZ  "
                              radarnames="Portland ME,Boston,New York City,Buffalo,Philadelphia,Pittsburgh"/>


    <doradar label="Midwest"  radarids="CLE,      LOT,    MPX,        LSX,      DMX,       EAX,        OAX"
                            radarnames="Cleveland,Chicago,Minneapolis,St. Louis,Des Moines,Kansas City,Omaha"/>


    <doradar label="South"  radarids="LWX,      JGX,     JAX,         MLB,      AMX,  LIX,        HGX,    LOT,    MPX,        EAX"
                          radarnames="Baltimore,Atlanta, Jacksonville,Melbourne,Miami,New Orleans,Houston,Chicago,Minneapolis,Kansas City"/>


    <doradar label="West" radarids="FTG,   ABX,        IWA,    MTX,           CBX,  ATX,    RTX,        MUX,          VTX"
                    radarnames="Denver,Albuquerque,Phoenix,Salt Lake City,Boise,Seattle,Portland OR,San Francisco,Los Angeles"/>




    <append name="html"><![CDATA[</body></html>]]></append>

    <output file="${islpath}/radar.html">
        <output text="${html}"/>
    </output>

   <exec command="scp ${islpath}/radar.html conan:/content/software/idv/examples/radar/index.html"/>


</isl>