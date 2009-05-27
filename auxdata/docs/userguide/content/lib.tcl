

namespace eval ug {}
namespace eval ht {}

#Turn on tcl evaluation


gen::setDoTclEvaluation 1

#Add the special handler for the controls section
gen::addHandler {controls/.*\.html} ug::handleControls


#Don't include the overview section when listing children
gen::setDoChildOverview 0

##We use this for referencing install anywhere file names
gen::defineMacro {<%idv.version%>} {2.7}
gen::defineMacro {<%idv.under.version%>} {2_7}
gen::defineMacro {<%release.date%>} [clock format [clock seconds] -format {%B %d, %Y}]


##Assuming we are in the auxdata/docs/userguide/content directory
##read in the version.properties file in
##../../../../ucar/unidata/idv/resources/build.properties file
catch {
    set fp [open [file join .. .. .. .. ucar unidata idv resources build.properties] r]
    set contents [read $fp]
    close $fp
    set ok 1
##Parse the property file
    foreach key {major minor revision} {
        set $key ""
        if {![regexp "idv.version.$key\s*=\s*(\[^\n\]*)"  $contents match $key]} {
            puts "Failed to read version: $key"
            set ok 0
            break
        } else {
            set $key [string trim [set $key]]
        }
    }
    if {$ok} {
        gen::defineMacro {<%idv.version%>} "$major.${minor}$revision"
        gen::defineMacro {<%idv.under.version%>} "$major_${minor}$revision"
    }
} err



proc ug::jnlp {jnlp {tooltip {}}} {
    if {$::forJavaHelp} {
        global currentFile 
        set path [file dirname $currentFile]
        if {$tooltip == ""} {
            set tooltip "Click to load the example bundle"
        }
        return "
<OBJECT CLASSID=\"java:ucar.unidata.ui.HelpActionLabel\">
<param name=\"helpIcon\" value=\"/auxdata/docs/userguide/processed/images/webstart.jpg\">
<param name=\"helpTooltip\" value=\"$tooltip\">
<param name=\"helpAction\" value=\"/auxdata/docs/userguide/processed/$path/$jnlp\">
</OBJECT> $tooltip
"
    } else {
        if {$tooltip == ""} {
            set tooltip "Click to launch via webstart"
        }
        return "<a href=\"$jnlp\"><img src=\"%dotpath%/images/webstart.jpg\" border=\"0\" alt=\"$tooltip\"></a> $tooltip"
    }
}

proc ug::menuDesc {name {desc ""}} {
    set desc [subst $desc]
    return "<li> [ug::menu $name]<div class=\"menudesc\">$desc</div>"
}






set ::ugfiles [list]

gen::setAllFileName "userguide.html"


gen::setDoImageLinks 0


proc gen::getAllPagesTitle {} {
     return "IDV User Guide"
}



set ::forJavaHelp 0


proc gen::hook::parseArgs {argv arg i} {
    switch -- $arg {
        -forjavahelp {
            set ::forJavaHelp 1
            puts "Building for javahelp\n**********************\n"
            gen::setPageTemplateName JavaHelpTemplate.html
        }

        default {
            puts "Unknown argument: $arg"
            gen::usage
        }
    }
    return $i
}


proc gen::hook::definePage {file} {
    set body [gen::getBody $file]

    set names [list]
    set tmpBody $body
    while {[regexp {<a\s*name\s*=\s*"(.*?)">(.*)$} $tmpBody match name tmpBody]} {
        lappend names $name
#        puts "define: $file name=$name"
    }

    set tmpBody $body
    while {[regexp {<meta\s*?name\s*=\s*"jhid"\s*value="([^"]+)"(.*)$} $tmpBody match name tmpBody]} {
##"
         lappend names $name
##         puts "define: $file meta id=$name"
##         puts $body
    }

    set ::jhids($file) $names
    lappend ::ugfiles $file
}


#This gets called from generate.tcl when the processing is complete
#We generate the javahelp toc.xml and map.xml files here
proc gen::hook::end {} {
    set xml {
<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE map
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp Map Version 1.0//EN"
         "http://java.sun.com/products/javahelp/map_1_0.dtd">

<map version="1.0">

}

    set contentPrefix ""

    foreach file $::ugfiles {
##        puts "File:$file"
        set id [ug::getJavaDocId $file]
        set tag "<mapID target=\"$id\" "
        append xml   "[pad $tag 60] url=\"$contentPrefix$file\" />\n"
        foreach jhid $::jhids($file) {
##            puts "\t$jhid"
            if {[string first "idv." $jhid] == 0} {
                set tag "<mapID target=\"$jhid\" "
                append xml "[pad $tag 60] url=\"$contentPrefix$file\" />\n"
            } else {
                set tag "<mapID target=\"$id.$jhid\" "
                append xml "[pad $tag 60] url=\"$contentPrefix$file\#$jhid\" />\n"
            }
        }

    }

    append xml "\n</map>\n"
    set mapXmlFile [file join [gen::getTargetDir] Map.xml]
    gen::writeFile $mapXmlFile $xml

    set tocXml {
<?xml version='1.0' encoding='ISO-8859-1'  ?>
<!DOCTYPE toc
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp TOC Version 1.0//EN"
         "http://java.sun.com/products/javahelp/toc_1_0.dtd">

<toc version="1.0">

}

append tocXml [ug::makeTocXml [gen::getTopFile]]


append tocXml "\n</toc>\n"
set tocXmlFile [file join [gen::getTargetDir] TOC.xml]
gen::writeFile $tocXmlFile $tocXml



}



proc ug::step {html {img {}} {extra {}}} {
    if {$img!=""} {
        set img "<div style=\"margin-left:10;\"><table><tr valign=\"top\"><td><img src=\"$img\"></td><td>$extra</td></tr></table></div>"
    }

    return  "<li> $html $img<p>"
}

proc ug::prefGroup {args} {
    return "</div>&nbsp;<p><div class=\"pagesubtitle\">[join $args { }]</div>"
}



proc ug::prefSubGroup {args} {
    return "</div><div class=\"preferencegrouptitle\">[join $args { }]</div><div class=\"preferencegroup\">"
}

proc ug::preference {label {jhid {}}} {
    if {$jhid==""} {
        set jhid [string tolower $label]
        regsub -all {\s} $jhid {} jhid
    }
    return "<meta name=\"jhid\" value=\"$jhid\">\n<a name=\"$jhid\"></a><p><b>$label</b>."
}

proc ug::makeTocXml {file {pad ""}} {
    set id [ug::getJavaDocId $file]
    set xml $pad
    set children  [gen::getChildren $file]
    if {[llength $children]} {
        append xml "<tocitem text=\"[gen::getTitle $file]\" target=\"$id\">\n"
        foreach child [gen::getChildren $file] {
            append xml [ug::makeTocXml $child "$pad  "]
        }
        append xml $pad "</tocitem>\n"
    } else {
        append xml "<tocitem text=\"[gen::getTitle $file]\" target=\"$id\"/>\n"
    }
}



proc ug::getJavaDocId {file} {
    set suffix ""
    set idx [string first \# $file]
    if {$idx>=0} {
        incr idx
        set suffix ".[string tolower [string range $file  $idx end]]"
    }
    set root [string tolower [file root $file]]
    regsub -all / $root . root
    regsub {.index$} $root {} root
    if {[string first "idv." $root] ==0} {
        return $root
    }
    return "idv.$root$suffix"
}

proc pad {s l}  {
    while {[string length $s]<$l} {
        append s " "
    }
    set s
}


##Handle the html in controls special
proc ug::handleControls  {path content} {
    if {[catch {ug::handleControlsInner $path $content} err]} {
        puts "Error handling $path\n $err"
#        puts "Content:$content"
        exit
        return $content
    }
    return $err
}


proc ug::handleControlsInner  {path content} {
    if {[regexp {<html} $content]} {return $content}
    set title [subst -novariables  [lindex $content 0] ]
    set overview [subst -novariables  [lindex $content 1]]
    set args [list]
    foreach pair [lrange $content 2 end] {
        if {[regexp {^\[} $pair]} {
            foreach {prop desc} [subst -novariables $pair] {
                lappend args [list $prop $desc]
            }
        } else {
            foreach {prop desc} $pair break
            set desc [subst -novariables $desc]
            lappend args [list $prop $desc]
        }
    }
    ug::controlHtml  $path $title $overview  $args
}





proc ug::mmenu {args} {
    set html [eval ug::menu $args]
    ##Don't do this for now.
    if {0 && $::forJavaHelp} {
        set arg [join $args :]
        set extra "
<OBJECT CLASSID=\"java:ucar.unidata.ui.HelpActionLabel\">
<param name=\"helpText\" value=\"(try it) \">
<param name=\"helpAction\" value=\"jython:idv.getIdvUIManager().showMenu('$arg');\">
</OBJECT> 
"
        append html " $extra"
    }
    set html

}


proc ug::menu {args} {
    set sep "-&gt;"
    return "<code class=\"menu\">[join $args $sep]</code>"
}

proc ug::button {args} {
    set sep " "
    return "<code class=\"menu\">[join $args $sep]</code>"
}

proc ug::addSource {} {
    return "[ug::button Add Source] button"
}

proc ug::chooser {args} {
    set sep " "
    return "<b>[join $args $sep]</b>"
}

proc ug::tab {args} {
    set sep " "
    return "<code class=\"button\">[join $args $sep]</code>"
}


proc ug::label {args} {
    set sep " "
    return "<code class=\"button\">[join $args $sep]</code>"
}


proc ht::command {args} {
    return "<code class=\"command\">[join $args { }]</code>"
}


proc ug::colortable {{level ../}} {
    list {Color Table} "
   The control has a color bar showing the active color table
   and the associated high and low data values in the units the
   display is made in. As the mouse pointer is moved over the
   color bar, the value at a particular color is shown. Click on 
   the color bar to start the 
   <a  href=\"${level}tools/ColorTableEditor.html\">Color Table Editor</a>. 
   Or click on the button that displays
   the name of the color table to show a popup menu that allows you
   to change the range,  select other color tables, etc."
}


proc ug::levels {} {
    set l [list]
    lappend l Levels 
    lappend l "Click on the [ht::command Levels] box to see a pull down menu of the
native grid levels. Click on one level value to reset the 
plan to that level. The plan level in use is shown
in the data's native altitude units. 
The [ht::command Levels] box is editable. Click in the box, enter
a value and hit return to set your own value.
<p>
Click on the [ht::command Cycle] check box to animate  
           vertically through all available levels.
" 
set l
}

proc ug::resample {widget} {
list {Resampling} "
Adjust the [ht::command $widget] slider to change the
resolution of the image. A larger number makes a lower resolution
(coarser) display.
"
}

proc ug::skip {widget} {
list {Skip interval} "
To avoid cluttering the display you can 
define the number of grid points that are skipped
with the [ht::command $widget] combo box.
A skip interval of 0 mean show all vectors,  1 means skip every
other vector,  2 means show every third vector, etc.
This is also an editable box, enter  a new skip interval 
and press return.
"
}

proc ug::vertical {display} {
list {Vertical Position} "This slider allows you to set the vertical
position of the $display."
}



proc ug::shade {display} {
list {Shade Colors} "
The color-shaded display has two modes. It comes up
with interpolated  colors: every pixel is colored to give a smooth gradation of color.
The alternative is coloring an area of pixels corresponding to a
single data grid cell with one color. Click off the <b>Shade Colors</b>
box in the control window to see this display. 
"

}

proc ug::selector {display} {
    list {Plan Selector} "
Another way to adjust the height of a $display through a 3D field is
to drag the selector point (a colored rectangle) 
near the lower left corner of the plot to a
new height. The 3D display has to be rotated to see the display 
somewhat from the side to do this.  The color of this selector point
can be changed through the [ug::menu Edit {Selector Color}] menu.
"
}

proc ug::color {{what {position line and its end points}} {title {Probe Color}}} {
    list $title "Use the [ug::menu Edit {Color}] menu  to  select a color for the  $what"
}

proc ug::station {} {
    list {Station} "Shows the station location id, latitude and longitude."
}

proc ug::lineWidth {} {
    list {Line Width} "Set the line width in the display. Click on the <img src=\"%dotpath%/ui/images/Slider16.gif\"> to change the value."
}

proc ug::location {{what {cross section line}}} { 
    list {Location} "Shows the starting and ending points of the $what."
}


proc ug::animation {} {
    list {Time Animation Control} {
        <img src="images/AnimationControls.gif" ><br>
        controls looping of displays when more than one
        data time is loaded. See more in 
        <a href="../ui/index.html#animation">Time Animation Control</a>.}
}

proc ug::autoscale {} {
    list {Autoscale Y-Axis} {
        When selected, the display will automatically scale the Y Axis to the
data range along the selector line.}
}

proc ug::contour {} {
    list {Contour} {
To set  contour information use the <b>Contour: Set </b>
button,  which brings up the  
<a href="../tools/ContourDialog.html">Contour Properties Editor</a>.
}
}

proc ug::datarange {} {
    list {Visible Range} { 
The [ug::button Visible Range] allows you to set the range 
that determines what parts of the data is actually shown.  Check
the box to enable the widget and use the [ug::button Change] button
to set the data range.
}
}

proc ug::texture {control} {
    list {Texture Quality} { 
The [ug::button Texture Quality] allows you to set the quality of
the displayed texture of $control.  A higher
quality will take longer to render and use more memory.  Move
the slider to select the quality you want.
}
}

proc ug::displaymode {control} {
    list {Mode} {
The [ug::button Mode] selector allows you to set the way
the texture is displayed.  You can select Solid, Mesh,
or points.  This is useful for looking at the structure
of the underlying data used to create the $control.
}
}


proc ht::convertSpaces {c} {
    regsub -all {\n\s*[\n\s]+\n} $c {<p>} c
    set c
}

proc ht::section {t} {
    return "<div class=\"pagesubtitle\">$t</div>"
}

proc ug::controlHtml {path title overview properties} {
    set controlFile [string tolower [file tail [file rootname $path]]]
    set html "
<html><head>
  <title>$title</title>
  <link rel=\"stylesheet\" type=\"text/css\" href=\"/idv.css\">
</head>
<body>
<xxxmeta xxxname=\"jhid\" value=\"idv.controls.$controlFile\">
<div class=\"pagesubtitle\">Overview</div>
[ht::convertSpaces $overview]

"

    if {[llength $properties]>0} {
       append html "[ht::section Properties]<ul>"
    }

    foreach  propTuple $properties {
        foreach {name content} $propTuple break
        append html "<li><b>$name</b><p> [ht::convertSpaces $content] <p>\n"
    }

    if {[llength $properties]>0} {
       append html "</ul>"
    }

    append html "</body></html>"

}


proc ug::head {title} {
    return "<html><head>\n<title>$title</title>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"idv.css\" title=\"Style\">\n</head>\n<body>\n"
}

proc ug::foot {} {
return "</body></html>"
}

proc ug::xml {args} {
  set xml [ht::pre [join $args " "]]
#  foreach t [array names ::taghome] {
#     regsub -all "lt;$t" $xml "lt;<a[ug::tagref $t]" xml
#     regsub -all "/$t" $xml "/<a[ug::tagref $t]" xml
#  }
  return "<blockquote>$xml</blockquote>"
}


proc ug::import {file} {
   set fp [open $file r ]
   set c [read $fp]
   return $c
}



