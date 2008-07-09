<?xml version="1.0" encoding="ISO-8859-1"?>
<group>
  <procedure
     name="generate_start"
     target="idvproducts"
     title="IDV Image Products"
     description=""
     scpdest=""
     cpdest=""
     generate_image="1"
     filepath="${islpath}"
     libpath="/ucar/unidata/idv/isl"
     generate_kmz="1"
     generate_html="1"
     generate_mov="1"
     generate_animatedgif="1">


    <property
       name="filepath"
       value="${filepath}"
       global="true"/>


    <property
       name="libpath"
       value="${libpath}"
       global="true"/>
    <property
       name="wwwroot"
       value="${wwwroot}"
       global="true"/>
    <property
       name="generate_kmz"
       value="${generate_kmz}"
       global="true"/>
    <property
       name="generate_mov"
       value="${generate_mov}"
       global="true"/>
    <property
       name="generate_image"
       value="${generate_image}"
       global="true"/>
    <property
       name="generate_animatedgif"
       value="${generate_animatedgif}"
       global="true"/>
    <property
       name="generate_html"
       value="${generate_html}"
       global="true"/>
    <property
       name="scpdest"
       value="${scpdest}"
       global="true"/>
    <property
       name="cpdest"
       value="${cpdest}"
       global="true"/>
    <property
       name="target"
       value="${target}"
       global="true"/>
    <property
       name="title"
       value="${title}"
       global="true"/>
    <property
       name="description"
       value="${description}"
       global="true"/>
    <property
       name="html"
       value=""
       global="true"/>
    <property
       name="kml"
       value=""
       global="true"/>
    <property
       name="imagecount"
       value="0"
       global="true"/>
    <append name="html">
<![CDATA[
<html>
<title>${title}</title>
<body>
<h3>${title}</h3>
${description}
<i>Generated: ${MMMMM} ${d}, ${yyyy} ${HH}:${mm} ${Z}</i><br>
<hr>
<table width="100%"><tr  valign="top">
]]>    </append>
  </procedure>

<!-- 
    Define the procedure to load in the bundle, generate the images, etc.  
    The transparency attribute is one of the default arguments used
  -->

  <procedure
     name="generate"
     thebundle=""
     label=""
     transparency="none">

    <if expr="'${thebundle}'==''">
       <then>
          <property name="bundle" value="${islpath}/${id}.jnlp"/>
       </then>
       <else>
          <property name="bundle" value="${thebundle}"/>
       </else>
    </if>

    <property name="jnlplink"><![CDATA[<a href="${bundle}">IDV Bundle</a> <br>]]></property>

    <increment name="imagecount"/>
    <if expr="${imagecount}&gt;2">
      <append name="html">
<![CDATA[
</tr>
<tr><td>&nbsp;<p></td></tr>
<tr valign="top">
]]>      </append>
      <replace
         name="imagecount"
         value="1"/>
    </if>

<!-- Load in the bundle    -->
    <bundle file="${bundle}" color="${transparency}"/>

    <property
       name="movie_files"
       value=""/>
    <property
       name="links"
       value=""/>
    <if expr="${generate_kmz}">
      <append
         name="movie_files"
         value=",${filepath}/${id}.kmz"/>
      <append name="links"><![CDATA[<a href="${id}.kmz">Google Earth</a><br>]]></append>
    </if>
    <if expr="${generate_mov}">
      <append
         name="movie_files"
         value=",${filepath}/${id}.mov"/>
      <append name="links"><![CDATA[<a href="${id}.mov">Quicktime</a> <br>]]></append>
    </if>
    <if expr="${generate_animatedgif}">
      <append
         name="movie_files"
         value=",${filepath}/${id}.gif"/>
      <append name="links"><![CDATA[<a href="${id}.gif">Animated GIF</a> <br>]]></append>
    </if>

<!-- Generate the movie and the thumbnail   -->
<!--         kml_desc="&lt;a href=&quot;http://www.unidata.ucar.edu/cgi-bin/idv/getbundle.jnlp?bundleurl=${wwwroot}/${bundle}&quot;&gt;Run in the IDV&lt;/a&gt; (Needs Java Webstart)"
-->
    <if expr="'${movie_files}'!=''">
      <movie
         file="${movie_files}"
         imageprefix="image_%count%"
         imagesuffix="png"
         kml_desc="&lt;a href=&quot;${wwwroot}/${bundle}&quot;&gt;Run in the IDV&lt;/a&gt; (Needs Java Webstart)"
         kml_name="${label}"
         kml_visibility="0"
         kml_open="0">
        <transparent color="${transparency}"/>
      </movie>
    </if>
    <if expr="${generate_mov}">
      <generate_copy file="${filepath}/${id}.mov"/>
    </if>
    <if expr="${generate_animatedgif}">
      <generate_copy file="${filepath}/${id}.gif"/>
    </if>
    <if expr="${generate_kmz}">
      <generate_copy file="${bundle}"/>
      <generate_copy file="${filepath}/${id}.kmz"/>
      <append
         name="kml"
         fromfile="${libpath}/networklink.kml"/>
    </if>
    <if expr="${generate_image}">
      <image file="${filepath}/${id}.png">
        <transparent color="${transparency}"/>
      </image>
      <generate_copy file="${filepath}/${id}.png"/>
    </if>
    <if expr="${generate_html}">
      <image file="${filepath}/${id}_thumb.png">
        <resize width="300"/>
        <transparent color="${transparency}"/>
      </image>
      <generate_copy file="${filepath}/${id}_thumb.png"/>
      <append name="html">
<![CDATA[
<td><b>${label}</b><br><table><tr  valign="top"><td><img src="${id}_thumb.png">
</td> <td>
See it in:<br>
${links}
${jnlplink}
</td></tr></table>
${paramtext}
</td>
]]>      </append>
    </if>
  </procedure>
  <procedure name="generate_copy">
    <if expr="'${scpdest}'!=''">
      <exec command="scp ${file} ${scpdest}"/>
    </if>
    <if expr="'${cpdest}'!=''">
      <exec command="cp ${file} ${cpdest}"/>
    </if>
  </procedure>
  <procedure name="generate_end">
    <if expr="${generate_kmz}">
      <output file="${filepath}/${target}.kml">
        <output fromfile="${libpath}/header.kml"/>
        <output text="${kml}"/>
        <output fromfile="${libpath}/footer.kml"/>
      </output>
      <generate_copy file="${filepath}/${target}.kml"/>
    </if>

    <if expr="${generate_html}">
      <output file="${filepath}/${target}.html">
        <output text="${html}"/>
      </output>
      <generate_copy file="${filepath}/${target}.html"/>
    </if>
  </procedure>
</group>
