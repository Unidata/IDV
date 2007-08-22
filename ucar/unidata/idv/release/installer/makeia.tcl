

##Where we put the scratch dir and the final installers
set destDir /upc/share/metapps/installers
set rootDir /upc/share/metapps
set appName  {Integrated Data Viewer}
set doWebstart 0

set templateFile ""


set ::links ""

#The version
set version IDV_1.3B2

set skipIA 0
set doCleanup 0


for {set cnt 0} {$cnt < [llength $argv]} {incr cnt} {
    set arg [lindex $argv $cnt]
    switch -- $arg {
        -root {
            set rootDir [lindex $argv [incr cnt]]
        }
        -template {
            set templateFile  [lindex $argv [incr cnt]]
        }
        -appname {
            set appName  [lindex $argv [incr cnt]]
        }
        -version {
            set version [lindex $argv [incr cnt]]
        }
        -dest {
            set destDir [lindex $argv [incr cnt]]
        }
        -webstart {
            set doWebstart 1
        }
        -skipia {
            set skipIA 1
        }
        -cleanup {
            set doCleanup 1
        }
        default {
            puts "Usage: tclsh makeia.tcl"
            puts "\t-root root_dir (where to find the lib and ucar dirs)"
            puts "\t-dest dest_directory (where to put the installers)"
            puts "\t-version version"
            puts "\t-appname  app name"
            puts "\t-webstart <create the webstart release>"
            exit
        }
    }
}



##Check if its relative

if {![regexp {^/} $rootDir]} {
    set pwd [pwd]
    while {[regsub \.\./ $rootDir {} rootDir]} {
	regsub {/[^/]+$} $pwd {} pwd
    }
    set rootDir [file join $pwd $rootDir]
##    puts "root=$rootDir"
##    exit
}




set libDir [file join $rootDir lib]

if {$templateFile==""} {
    set templateFile [file join $rootDir ucar unidata idv release installer template.iap_xml]
}

regsub -all {%VERSION%} $destDir $version destDir
regsub -all {%version%} $destDir [string tolower $version] destDir

puts "    Version: $version"
puts "   App name: $appName"
puts "       Root: $rootDir"
puts "Destination: $destDir"


regsub {\.} $version _ tmp
set ::filePrefix generated_[string tolower $tmp]



proc processTemplate {template  list } {
    foreach {macro value} $list {
        regsub -all "%$macro%" $template $value template
    }
    set template
}


proc createWebstart {} {
    set webstartDir [string tolower webstart]
    puts "Creating the webstart release in: [file join $::destDir $webstartDir]"
    exec   ant -buildfile $::rootDir/build/build.xml jnlp  -verbose -Dwebstart_target_root $::destDir -Dwebstart_target_subdir=$webstartDir -Dwebstart_url_root=http://www.unidata.ucar.edu/content/software/IDV/release/[string tolower $::version]
    puts "Done creating the webstart release"

}


if {$doWebstart} {
    if {[catch {createWebstart} err]} {
        puts "Error: $::errorInfo"
    }
    exit
}



proc fileRead {f} {
    set fp [open $f r]
    set c  [read $fp]
    close $fp
    set c
}

proc fileWrite {f contents} {
    set fp [open $f w]
    puts $fp $contents
    close $fp
}


proc makeTemplate {extraFlags} {
    set scratchDir [file join $::destDir scratch]
##    catch {file delete -force $scratchDir}
    if {![file exists $scratchDir]} {
        file mkdir $scratchDir
    }
    array set flags  [list ia.destdir  $::version  ia.appname $::appName ia.rootdir $::rootDir  ia.libdir $::libDir ia.unix.do true] 
    array set flags $extraFlags

    set template  [fileRead  $::templateFile]

    set outputFile [file join $scratchDir ${::filePrefix}.iap_xml] 
    puts "        IAP: $outputFile"
    fileWrite $outputFile [processTemplate $template  [array get flags]]
}


proc runIA {winPrefix} {
    set scratchDir [file join $::destDir scratch]
    if {[catch {exec /upc/share/InstallAnywhere_5_Now/InstallAnywhere} err]} {
	set inABadWay 0
	foreach line [split $err \n] {
	    set line [string trim $line]
	    if {$line==""} {continue}
	    if {![regexp {redirecting} $line]} {
		if {![regexp {Cannot\s*convert} $line]} {
		    puts "** makeia error: $err"
		    set inABadWay 1
		}
	    }
	}
	if {$inABadWay} {
	    puts "** makeia Errors have occurred. Exiting"
	    exit
	}
    }

    set root [file join $scratchDir ${::filePrefix}_Build_Output/Web_Installers/InstData]
    set prefix [string tolower $::version]
    regsub -all {\.} $prefix _ prefix
    set cnt 0
    set dirs [list Linux bin i586 MacOSX zip {} Java jar {} Solaris bin sparc Windows exe i586]
    set errors ""
    foreach {Os suff hardware} $dirs  {
        set os [string tolower $Os]
        set from [file join $root $Os/VM/installer.$suff]
        if {![file exists $from]} {
            set from [file join $root $Os/installer.$suff]
        }
        if {$hardware!=""} {
            append hardware _
        }
        set to [file join $::destDir ${prefix}_${os}_${hardware}installer.$suff]
        if {![file exists $from]} {
            append errors  "Error: File does not exist:  $from\n "
            continue;
        }
        puts "Copying installer to $to"
        file rename -force  $from $to
        set tail [file tail $to]
        set label $Os
        append ::links "<tr><td>$label</td><td> <a href=\"$tail\">$tail</a></tr>\n\n"
        incr cnt
    }
    if {$cnt==0} {
        puts "** makeia No installers were created or found:\n$errors" 
    } else {
	puts "$cnt installers created."
    }
    return $cnt
}






makeTemplate     [list ia.windows.vm SunJRE1.4.2oglWin32.vm ia.others.do true]
if {![runIA   ogl]} {
    puts "** makeia Exiting"
    exit
}


###puts "Now making the d3d windows installer"
###makeTemplate      [list ia.windows.vm SunJRE1.4.2d3dWin32.vm ia.others.do false ]
###runIA  d3d


puts "Done"




set extra ""
if {$doWebstart} {
    append extra "<a href=\"$webstartDir/IDV/idv.jnlp\">Webstart</a><p>\n"
} 
append extra "<a href=\"docs/userguide/index.html\">User Guide</a><p>"

set links "<table>$::links</table>"
set html "<html><head>\n<title>IDV Release $::version</title>\n</head><body>\n\nIDV Release $::version<p>$extra\n$links\n</body></html>"


fileWrite [file join $destDir index.html] $html


if {$doCleanup} {
    file delete [file join $destDir scratch]
}




