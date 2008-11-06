#
##A library of routines specifically for the IDV workshop
##


##Define the index file we are using if we aren't using the default (main.index) filename
gen::setIndexFile   main.index

##Where is the userguide located at
##TODO: Set this to point to the right place
set ::workshopDocroot http://www.unidata.ucar.edu/software/idv/release/nightly
set ::userguideRoot $::workshopDocroot/docs/userguide

##The page title
set ::workshopTitle {Unidata IDV Workshop}

##The  home directory of the workshop account. We use this var in the macro and in the ht::save routine
set ::workshopHome /home/idv

##The installation directory of the  IDV
set ::idvInstall /home/idv/

##Where on the filesystem do we find the data files.
gen::defineMacro {<%workshop.datadir%>} {/data/idv}

##We use this for referencing download file names
gen::defineMacro {<%idv.version%>} {2.6}

##The installed directory of the source
gen::defineMacro {<%workshop.installdir%>} "$::workshopHome/idv"

##The  home directory of the workshop account
gen::defineMacro {<%workshop.homedir%>} $::workshopHome

##The  home directory of the workshop account
gen::defineMacro {<%workshop.idvinstall%>} $::idvInstall

##The sitepath to use - where to find resources, etc.
gen::defineMacro {<%workshop.sitepath%>} ${::workshopDocroot}/data
gen::defineMacro {<%idv.website%>} ${::workshopDocroot}

##The installed directory of the source
gen::defineMacro {<%workshop.installdir%>} "$::workshopHome/idv"

##Where on the filesystem do we find the data files.
set ::workshopexampledir ${::workshopHome}/idv/ucar/unidata/apps/example
gen::defineMacro {<%workshop.exampledir%>} $::workshopexampledir

##Add the page title macro
gen::defineMacro {<%workshop.title%>} "$::workshopTitle for version <%idv.version%>"

##We use this for referencing download file names
gen::defineMacro {<%dev.version%>} {<%idv.version%>}


##Assuming we are in the auxdata/docs/userguide/content directory
##read in the version.properties file in
##../../../../ucar/unidata/idv/resources/version.properties file
catch {
    set fp [open [file join .. .. .. .. ucar unidata idv resources version.properties] r]
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
#       gen::defineMacro {<%idv.version%>} "$major.${minor}$revision"
       gen::defineMacro {<%dev.version%>} "$major.${minor}$revision"
    }
} err


set ::forDevWorkshop 0
set ::forRegionalWorkshop 0

proc gen::hook::parseArgs {argv arg i} {
    if {$arg == "-fordev"} {
        set ::forDevWorkshop 1
        gen::setIndexFile   dev.index
        set ::workshopTitle {Unidata IDV Developer's Workshop}
        gen::define flag_developerworkshop 
        gen::defineMacro {<%workshop.title%>} "$::workshopTitle for version <%idv.version%>"
        return $i
    }  
    if {$arg == "-forreg"} {
        set ::forRegionalWorkshop 1
        gen::setIndexFile   regional.index
        set ::workshopTitle {Regional Unidata IDV Workshop}
        gen::define flag_regionalworkshop 
        gen::defineMacro {<%idv.version%>} {2.6}
        set ::workshopDocroot http://www.unidata.ucar.edu/software/idv
        set ::workshopHome {c:\\\\data\\\\idv}
        gen::defineMacro {<%workshop.installdir%>} "$::workshopHome"
        gen::defineMacro {<%workshop.homedir%>} $::workshopHome
        gen::defineMacro {<%workshop.idvinstall%>} {c:\\\\Program Files\\\\}
        gen::defineMacro {<%workshop.datadir%>} {c:\\\\workshopdata}
        gen::defineMacro {<%workshop.sitepath%>} ${::workshopDocroot}/data
        gen::defineMacro {<%idv.website%>} ${::workshopDocroot}
gen::defineMacro {<%workshop.title%>} "$::workshopTitle for version <%idv.version%>"

        return $i
    }
    puts "Unknown argument: $arg"
    gen::usage
    set i
}



###############################################################
## You probably don't need to change anything below here
###############################################################

##When we generate the all.html file what is the filename we use
gen::setAllFileName "workshop.html"


##Turn on tcl evaluation
gen::setDoTclEvaluation 1


gen::defineTranslation fr Français
gen::defineTranslation es Español
gen::defineTranslation pt Portugese
gen::defineTranslation ko Korean
gen::setUrlRoot  $::workshopDocroot/docs/workshop
gen::setDoTranslateLinks 0


namespace eval ht {
    namespace eval ug {}
}



proc gen::getAllPagesTitle {} {
     return $::workshopTitle
}







gen::setDoJSNav 1
gen::setDoJSBorder 1
gen::setDoJSBG 1
gen::js::setBorders Left Top
gen::js::setBGColor #eeeeee




##Overwrite the processExtra
proc gen::processExtra {from  content} {
    set userguide "/index.html"
    regexp {<meta\s*?name\s*?=\s*?"userguide"\s*?content\s*?=\s*?\"(.*?)"} $content match userguide
    regsub -all {%userguide%} $content $userguide content
    regsub -all {%urlroot%} $content $::workshopDocroot content
    return $content;
}


proc ht::ug::top {url} {
    return "<meta name=\"userguide\" content=\"$url\">"
}

proc ht::ug::url {url} {
    return "$::userguideRoot$url"
}


proc gen::extraFormatHtml {path c} {

    while {[regexp {<code\s*?class\s*?=\s*?"(code|command|dialog|filename)"\s*?>(.*?)</code>} $c match what contents]} {
        set contents [string trim $contents]
        regsub  {<code\s*?class\s*?=\s*?"(code|command|dialog|filename)"\s*?>(.*?)</code>} $c "\[ht::$what $contents\]" c
    }


    set orig $c

    while {[regexp {<code\s*?class\s*?=\s*?"menu"\s*?>(.*?)</code>} $c match menu]} {
        set menu [string trim $menu]
        regsub -all -- {-&gt;} $menu > menu
        set menuHtml ""
        foreach m [split $menu >] {
            append menuHtml " \{$m\} "
        }
        regsub {<code\s*?class\s*?=\s*?"menu"\s*?>(.*?)</code>} $c "\[ht::menu $menuHtml\]" c
    }
    return $c
}



proc ht::save {file} {
    return "Make sure you are in the directory [ht::filename $::workshopHome]. 
            Enter the name [ht::filename $file]. Click [ht::button Save]."
}


proc ht::createBasicDisplay {args} {
    array set A [list -param temperature -tab {3D grid} -display {Contour Plan View}]
    array set A $args
    return "Create a [ht::display $A(-display)] display with a [ht::param $A(-param)] parameter. 
           <popup label=\"\">
            To create a [ht::display $A(-display)]: 
            <ul>
              <li> In the [ht::dialog Field Selector] tab of the [ht::command Dashboard] select the desired data source under 
                   the [ht::command Data Sources] list.
              <li> In the  [ht::qt Fields] panel in the [ht::dialog Field Selector], expand the [ht::qt 3D grid] tab.  
             </li>
             <li> Select the  [ht::param $A(-param)] field. 
             </li>
             <li> In the [ht::command Displays] list, select [ht::display $A(-display)]
                  and press the [ht::button Create Display] button. 
             </li> 
             </ul>
           </popup>"
} 

proc ht::openDataSourceChooser {} {
    return "Open the [ht::dialog Data Source Chooser]. <popup label=\"\">
            Open the [ht::dialog Data Source Chooser] by either:<ul>
            <li> Select the [ht::command Data Chooser] tab in the 
                 [ht::dialog Dashboard]
            <li> Choose one of the menus under the 
                 [ht::menu Data {New Data Source}] menu.
            </popup>"
            
}

proc ht::editDataSourceProperties {{arg {the dataset}}} {
    return "Open the [ht::dialog Data Source Properties] editor for $arg.
            <popup label=\"\">
            Open the [ht::dialog Data Source Properties] editor by either:
            <ul>
            <li> Double click on the specific data source in the
                 [ht::command Data Sources] panel in the 
                 [ht::tab Field Selector].
            <li> Right click on the specific data source in the
                 [ht::command Data Source] panel in the 
                 [ht::tab Field Selector] and choose the
                 [ht::menu Properties] menu from the popup menu.
            </ul>
            </popup>"
            
}


proc ht::loadBasicGrid {} {
    if {$::forDevWorkshop} {
        return "Load in  some grid data source"
    }
        


    return "Load in  the [ht::filename Sample RUC Data] data source.
           <popup label=\"\">
           To load in the [ht::filename Sample RUC Data] data source:
        <ul>
          <li> Open the [ht::dialog Catalog Chooser] by either:
          </li>
             <ul> 
                 <li> Choose the [ht::menu Data {New Data Source} {From a Catalog}] menu item.
                 <li> Click the [ht::command Catalogs] tab in the [ht::command {Data Chooser}].
                 </li>
             </ul>
         <li> Select the [ht::menu {Miscellaneous Data}  {Sample RUC Data}] item.
         </li>
         <li> Select the [ht::button Add Source] button.
       </ul>
       </popup>
    "
}


proc ht::cmdline {a} {
    return "<i><blockquote>$a</blockquote></i>"
}

proc ht::createStationPlot {} {
    return "Create a [ht::display Point Data] display.
           <popup label=\"\">
              To create a [ht::display Point Data] display:
<ul>
<li> Select the 
[ht::filename Surface (METAR) Data] in the [ht::command Data Sources] list
in the [ht::dialog Field Selector].
</li>
<li>Select  [ht::param Point Data] in the [ht::command Fields] list.</li>
<li>Select  [ht::display Point Data Plot] in the [ht::command Displays] list.</li>
</ul>
</popup>
"


}

proc ht::loadPointData {} {
    return "Load in some surface observation data.
           <popup label=\"\">
           To load in surface observation data:
        <ul>
          <li> Open the [ht::dialog Surface Point Data Chooser] by either:
          </li>
             <ul> 
                 <li> Select the [ht::command Point] tab in the [ht::command Data Chooser].
                 </li>
                 <li> Or, choose the [ht::menu Data {New Data Source} {Surface Observations}] menu item from the main menu bar.
                 </li>
             </ul>
         <li> Click on the [ht::button Connect] button.
         </li>
         <li> Make sure the [ht::command Data Type] is [ht::param Surface (METAR) Data].</li>
         <li> Choose the latest time (the last in the list) in the
             [ht::command Available Times] list.</li>
          <li>Click the [ht::button Add Source] button.
       </ul>
       </popup>
    "
}




proc ht::loadEtaGrid {} {
    return "Load in  the [ht::filename ETA 1998-06-29 00:00] data source.
           <popup label=\"\">
           To load in the [ht::filename ETA 1998-06-29 00:00] data source:
        <ul>
          <li> Open the [ht::dialog Catalog Chooser] by either:
          </li>
             <ul> 
                 <li> Choose the [ht::menu Data {New Data Source} {From a Catalog}] menu item.
                 <li> Click the [ht::command Catalogs] tab in the [ht::command {Data Chooser}].
                 </li>
             </ul>
         <li> Select the [ht::menu {Case Studies}  {Data for Comet Case Study 039} {NCEP Model Data} {ETA 1998-06-29 00:00 GMT}] item.
         </li>
         <li> Select the [ht::button Add Source] button.
       </ul>
       </popup>
    "
}

proc ht::cutAndPasteTip {} {
    return "(Cut and Paste tip
           <popup label=\"\">
           You can cut and paste between your browser and the IDV or
           a terminal window to save some typing.
        <ul class=\"step\">
          <li> In the browser, highlight the text you want to copy to
          the IDV by clicking and dragging your mouse over it.
          </li>
          <li> Press Ctrl-C to copy to the system clipboard</li>

          <li> To paste to the IDV:

            <ul class=\"substep\">

              <li>Click the mouse in the IDV at place you want to
                  paste the text.</li>

              <li>Press Ctrl-V to paste into the IDV.</li>
            </ul>
          </li>

          <li> To paste to a terminal window or another application:

            <ul class=\"substep\">

              <li>Click the mouse in the terminal window/application at the
                  place you want to paste the text.</li>

              <li>Use your terminal/application's operation for pasting (e.g.,
                  for X-Windows: click middle mouse button; for
                  many Windows applications: press Ctrl-V)</li>
            </ul>
          </li>
       </ul>
       </popup>)
    "
}

proc ht::startIDV {} {
    return "Start the IDV 
            [ht::popup /installandstart/basics/ShortStart.html {} -useimg 1]
            "
}

proc ht::clearDisplays {} {
    return "If other displays are in the main IDV window, select the 
            [ht::menu Edit {Remove All Displays}] menu item or the
            <img src=\"%dotpath%images/Erase16.gif\"> icon in the toolbar to clear them out."
}

proc ht::clearDisplaysAndData {} {
    return "If other displays and data are already loaded, select the 
            [ht::menu Edit {Remove All Displays and Data}] menu item 
            or the <img src=\"%dotpath%images/Cut16.gif\"> icon in the toolbar to clear them out."
}

proc ht::refresh {} {
    return "<popup label=\"\">To start fresh choose the [ht::menu Edit {Remove All Displays and Data}] menu item or the scissors icon in the toolbar.</popup>"
}

proc ht::zoom {} {
    return "[ht::popup /installandstart/basics/MouseAndKeyboard.html#zoom {} -useimg 1]"

}

proc ht::listSelect {} {
    return "[ht::popup /installandstart/basics/MouseAndKeyboard.html#listselect {} -useimg 1]"

}

proc ht::visibility {} {
    return "[ht::popup /installandstart/basics/MouseAndKeyboard.html#vistoggle {} -useimg 1]"

}

proc ht::img  {url {extra ""}} {
    return "<img src=\"$url\" $extra>"
}

proc ht::cdExample  {} {
    set foo [ht::cmdline "cd $::workshopexampledir" ]
    return "Change to the example directory. $foo"
}

proc ht::mapSelector {} {
     return "[ht::popup /installandstart/basics/MapSelector.html {} -useimg 1]"
}

proc ht::mapChange {} {
    return "Change the background maps using the [ht::dialog {Default Background Maps} Control]. 
           <popup label=\"\">
             Open the [ht::dialog {Default Background Maps}] display control by
             either:
                <ul>
                <li>Selecting the control's tab in the 
                    [ht::tab Display Controls] tab in the 
                    [ht::dialog Dashboard]</li>
                <li> Left clicking on the control's label</li>
                <li> Right clicking on the label and selecting the 
                   [ht::menu {Control Window}]</li> menu item.</li>
                </ul>
           </popup>"
}


proc ht::backgroundImage {} {
return "Use the [ht::menu Displays {Maps and Backgrounds} {Add Background Image}]
      menu to load in the background image control.  The default view
      is the Blue Marble image from NASA."
}



proc ht::param {args} {
    return "<code class=\"param\">[join $args { }]</code>"
}

proc ht::display {args} {
    return "<code class=\"display\">[join $args { }]</code>"
}

proc ht::chooser {args} {
    return "<code class=\"dialog\">[join $args { }]</code>"
}

proc ht::symbol {args} {
    return "<code class=\"param\">[join $args { }]</code>"
}

proc ht::dialog {args} {
    return "<code class=\"dialog\">[join $args { }]</code>"
}

proc ht::codeBlock {args} {
    return "<p><code>[join $args { }]</code><p>"
}


proc ht::ct {args} {
    return "<i>[join $args { }]</i>"
}

proc ht::filename {args} {
    return "<code class=\"filename\">[join $args { }]</code>"
}

proc ht::command {args} {
    return "<code class=\"command\">[join $args { }]</code>"
}

proc ht::dataset {args} {
    return "<code class=\"dataset\">[join $args { }]</code>"
}

proc ht::datasource {args} {
    return "<code class=\"dataset\">[join $args { }]</code>"
}

proc ht::tab {args} {
    return "<code class=\"command\">[join $args { }]</code>"
}

proc ht::cimg {img args} {
    array set A [list -attrs "" -popup 0]
    array set A $args
    if {$A(-popup)} {
        set inner [ht::popup $img "<img src=\"$img\" $A(-attrs) border=\"0\">"]
    }  else {
        set inner "<img src=\"$img\" $A(-attrs)>"
    }
    return "<div class=\"cimg\">$inner<br></div>"
}



proc ht::button {args} {
    return "<code class=\"command\">[join $args { }]</code>"
}


proc menuDesc {name {desc ""}} {
    set desc [subst $desc]
    return "<li> [ht::menu $name]<div class=\"menudesc\">$desc</div>"
}


set ::didIndex 0
proc  gen::isl {f} {
    set dir [file dirname $::currentFile]
    set from [file join $dir $f]
    set to [file join [gen::getTargetDir] $from]
    catch {file mkdir [file dirname $to]}
    set c [gen::getFile  $from]
#    puts "$from file exists: [file exists $from]" 
#    puts "copy: $from to: $to"
    file copy -force $from $to
    set c [ht::pre $c]
    regsub -all {(&lt;!--.*?--&gt;)} $c {<span class="xmlcomment">\1</span>} c
    regsub -all {(&lt;!\[CDATA\[.*?\]\]&gt;)} $c {<span class="xmlcdata">\1</span>} c
    regsub -all {([^\s]*)=&quot;} $c {<span class="xmlattr">\1</span>="} c
#    puts $c
#    exit
    return "$c <a target=\"_isl\" href=\"[file tail $f]\">[file tail $f]</a>"
}





proc  gen::xmlFile {f} {
    set dir [file dirname $::currentFile]
    set from [file join $dir $f]
    set to [file join [gen::getTargetDir] $from]
    catch {file mkdir [file dirname $to]}
    set c [gen::getFile  $from]
#    puts "$from file exists: [file exists $from]" 
#    puts "copy: $from to: $to"
    file copy -force $from $to
    set c [ht::pre $c]
    regsub -all {(&lt;!--.*?--&gt;)} $c {<span class="xmlcomment">\1</span>} c
    regsub -all {(&lt;!\[CDATA\[.*?\]\]&gt;)} $c {<span class="xmlcdata">\1</span>} c
    regsub -all {([^\s]*)=&quot;} $c {<span class="xmlattr">\1</span>="} c
#    puts $c
#    exit
    return "$c <a target=\"_isl\" href=\"[file tail $f]\">[file tail $f]</a>"
}



proc gen::islTag {tag} {
    if {!$::didIndex} {
        set ::didIndex 1
        catch   {
            set fp [open [file join ..  .. userguide content isl tags.index] r]
            array set ::islindex [read $fp]
        } err
        puts $err
    }

    set file "Summary.html#$tag"
    if {[info exists ::islindex($tag)]} {
        set file "$::islindex($tag)#$tag"
    }
##TODO: Fix the url
    return "<a target=\"ISLPAGE\" href=\"$::workshopDocroot/docs/userguide/isl/$file\"><i>$tag</i></a>"
}


namespace eval dg {
}
proc dg::class {c} {
    return "<code>$c</code>"
}

proc dg::method {c} {
    return "<code>$c</code>"
}

proc dg::className {c} {
    set c
}

proc ug::head {title} {
    return "<html><head>\n<title>$title</title>\n<link rel=\"stylesheet\" type=\"text/css\" href=\"idv.css\" title=\"Style\">\n</head>\n<body>\n"
}

proc ug::foot {} {
    return "</body></html>"
}
