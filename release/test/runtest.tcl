#!/bin/sh
# the next line restarts using tclsh \
exec tclsh "$0" "$@"

############################
# User specified Variables #
############################

set jvmMemory "2048m"
set jvmPermGen "256m"

######################################
# Should not need to edit below here #
######################################
# set startTime
set startTime [clock format [clock seconds] -format {%Y-%m-%d %H:%M:%S %Z}] 

# parse command line args

set testName ""
set userDir ""
set nodefault "" 
set noplugins ""
set java ""
set idvJar ""
set testHome ""
set offscreen ""

set tmpArchives [list]

foreach arg $argv {
    switch -glob -- $arg {
        -help {
            puts ""
            puts "runtest.tcl usage:"
            puts ""
            puts "-idvJar=path \[default : nightly build - /upc/share/idv/lib/idv.jar\]i"
            puts "-javaHome=path : path to java \[default : java\]" 
            puts "-tests=path : path to test archive \[default : ./archive\]"
            puts "-userDir=path : path to IDV user resources directory \[default : ./userdir\]"
            puts "-testName=name : name of directory that will hold results \[default : results\]"
            puts "-nodefault : flag to tell IDV to not load the default bundle before loading the test \[default : flag not set\]"
            puts "-noplugin : flag to tell IDV to not load plugins located in the userDir \[default : flag not set\]"
            puts "-noplugin : flag to tell IDV to not load plugins located in the userDir \[default : flag not set\]"
            puts "-offscreen : flag to tell IDV to run in offscreen mode \[default : flag not set\]"
            puts "test : one or more test names, space separated, no dash. Any argument without a dash is treated as a test name"
            puts ""
            exit
        }
        -idvJar=* {
            set idvJarTmp [split $arg "="]
            set idvJar [lindex $idvJarTmp 1]
        }
        -javaHome=* {
            set javaHomeTmp [split $arg "="]
            set java [lindex $javaHomeTmp 1]
        }
        -testName=* {
            set testNameTmp [split $arg "="]
            set testName [lindex $testNameTmp 1]
        }
        -tests=* {
            set testsTmp [split $arg "="]
            set testHome [lindex $testsTmp 1]
        }
        -userDir=* {
            set userdirTmp [split $arg "="]
            set userDir [lindex $userdirTmp 1]
        }
        -nodefault {
            set nodefault "-nodefault"  
        }
        -noplugins {
            set noplugins "-noplugins"
        }
        -offscreen {
            set offscreen "-Doffscreen='true'"
        }
        default {
          puts $arg
          set tmpArchives [concat $tmpArchives [glob [file join $testHome $arg]]]
        }
    }
}

if {$userDir == ""} {
    set userDir [file join [pwd] "userdir"]
}

if {$testHome == ""} {
    set testHome [file join [pwd] "archives"]
}

if {![file exists $testHome]} {
    puts "Test archive, ${testHome}, does not exists!"
    exit
} 


if {$java == ""} {
    set java "java"
}

if {$idvJar == ""} {
    set idvJar "/upc/share/idv/lib/idv.jar"
}

if {$testName == ""} {
    set testName "results"
}

set topLevelResultsDir [file join [pwd] $testName]
set resultsRightHtml [file join $topLevelResultsDir resultsRight.html]
set resultsLeftHtml [file join $topLevelResultsDir resultsLeft.html]
set resultsHtml [file join $topLevelResultsDir results.html]

puts "Welcome to the IDV Test Suite"
puts ""
puts "Test Environment information:"
puts "    Java : $java"
puts "    Tests Location : $testHome"
puts "    IDV User Resources Directory : $userDir"
puts "    Results Directory : $topLevelResultsDir"
puts ""

##################
# Tcl procedures #
##################

proc readFile {file} {
    set fp [open $file r]
    set contents [read $fp]
    close $fp
    return $contents
}

proc writeFile {file content} {
    set fp [open $file w]
    puts $fp $content
    close $fp
}

proc writeLeft {c} {
    append ::leftHtml "$c\n"
}

proc writeRight {c} {
    append ::rightHtml "$c\n"
}

proc processBundle {archiveName bundle archiveDir resultsDir} {
    global java
    global userDir 
    global idvJar
    global jvmPermGen
    global jvmMemory
    global nodefault
    global noplugins
    global offscreen

    set logFile [file join $resultsDir $archiveName.log]
    puts $logFile
    
    write "<div style=\"padding-left:10;background-color: lightgray;\"><b><a name=${archiveName}>$archiveName</a></b><br>\n";
    set extra [list]
    lappend extra -jar ${idvJar}
    
    ##If it is a jnlp file put a link to it.
    # need to make this a relative link to work...should point to jnlp in results dir for the archive
    if {[regexp {.*\.jnlp$} $bundle]} {
       # write "<a href=$bundle>$bundle</a><br>"
    }
    set descFile   [file join $archiveDir "$archiveName.txt"]
    if {[file exists $descFile]} {
        file copy $descFile [file join ${resultsDir} "$archiveName.txt"]
        write  [readFile $descFile]
        write "<p>"
    } 
    write "</div>"
    write "<div style=\"margin:10\">"

    set inError 0

    lappend ::archives $archiveName
    if {$::doJava} {
        set command "$java -Xmx${jvmMemory} -XX:MaxPermSize=${jvmPermGen} ${offscreen} -Djava.net.preferIPv4Stack=true -Dswing.metalTheme=steel $extra -userpath $userDir $nodefault $noplugins -test $archiveName $resultsDir $bundle 2> $logFile"
        puts $command

        set result  [catch {eval exec $command} err]

        if {[string trim $err]!=""} {
            puts "error: $err"
        }
        if {$result} {
            lappend ::errors [list $archiveName $err]
            write "<div><font  color=red>An error has occurred:</font></div> $err<p>" 
            set inError 1
        }
    } 
    set logContents [readFile $logFile]
    set contents ""
    set imageFiles [list]
    set imageFiles [ glob -nocomplain [file join $resultsDir *.gif] [file join $resultsDir *.jpg] [file join $resultsDir *.png]]
    set didone 0
    foreach line [split $logContents "\n" ] {
        if {[regexp {^Warning:\s*Cannot\s* convert} $line match file]} {
            ##Skip this
        } elseif {[regexp {log4j} $line match file]} {
            ##Skip this too
        } elseif {[regexp {^Writing\s*image:(.*)$} $line match file]} {
            #Skip this too, too
            #lappend imageFiles $file
        } else {
            append contents "$line\n"
        }
    }

    if {[string trim $contents] != ""} {
        write "<b>Output:</b> <div style=\"background-color:red;\"><p align='left'>[string trim $contents]</p></div>"
    }
    
    if {$inError} {
        write "</div>"
        return;
    }

    set testDir   [pwd]

    foreach origImage [glob -nocomplain [file join $archiveDir $archiveName*.jpg]] {
        set originalImages([file tail $origImage])  $origImage
    }

    foreach image $imageFiles {
        # get location of original image
        set originalImageTmp [file join $archiveDir [file tail $image]]
        catch {unset originalImagesTmp([file tail $originalImageTmp])}
        # get original image name and extension
        set parts [split $image .]
        set name [lindex $parts 0]
        set ext [lindex $parts 1]
        # create new name for original file to be stored in results dir
        set tmpNewName "${name}_orig.${ext}"
        set originalImage [file join $testDir $tmpNewName] 
        set testImage [file join $testDir $image]
        # copy original image to results directory
        file copy -force $originalImageTmp $originalImage
        # create relative path to test and original (copy) images
        set testDirLen [llength [file split $testDir]]
        set testDirLen [expr {$testDirLen + 1}]
        set testParts [file split $testImage]
        set originalParts [file split $originalImage]
        # make paths relative
        set testImageRel "."
        set originalImageRel "."
        for { set i $testDirLen} { $i <= [llength $testParts] } { incr i } {
            set testImageRel [file join $testImageRel [lindex $testParts $i]]
        }
        
        for { set i $testDirLen} { $i <= [llength $originalParts] } { incr i } {
            set originalImageRel [file join $originalImageRel [lindex $originalParts $i]]
        }
        # test if images exists
        set originalImageExist [file exists $originalImage]
        set testImageExists [file exists $testImage]    
        # write the html for the image
        writeImage $archiveName $originalImageRel $testImageRel $originalImageExist $testImageExists
    }

    set true 1

    foreach image [array names originalImages] {
        writeImage $archiveName $originalImages($image) ""  $true $true
    }
    write "</div>"
}

proc imageTag {image} {
    return "<a href=$image><img border=0 width=100% src=$image></a>"
}

proc writeImage {archiveName originalImage testImage origImgExists testImgExists} { 
    set tail [file tail $originalImage]
    write "<a name=$tail>"
    write "<center><b>[file tail $testImage]</b></center><table width=100%>"
    set img1 [imageTag  $originalImage]
    set img2 ""
    if {$origImgExists && $testImgExists} {
        set diffFP [open  "|diff $originalImage $testImage"]
        set diff [string trim [read $diffFP]]
        catch {close $diffFP}
        #if {!$diff == ""} #add open brace when uncommenting! 
        set false 0
        if {$false} {
            # images cannot be diff'd to see if a change has occured, as the IDV does not 
            # make reproducable images down to the pixel, so for now this is set to false
            lappend ::imageErrors(DIFF) "<a href=resultsRight.html#$tail target=display>[file tail $originalImage]</a>"
        } else {
            lappend ::imageErrors(NOERRORS) "<a href=resultsRight.html#$tail target=display>[file tail $originalImage]</a>"
        }       
        set img2 [imageTag $testImage]
    } elseif {$origImgExists} {
        lappend ::imageErrors(MISSINGTEST) "<a href=resultsRight.html#[file tail $originalImage] target=display>[file tail $originalImage]</a>"
        set img2 "Missing: $testImage"
    } else {
        lappend ::imageErrors(MISSINGORIGINAL) "<a href=resultsRight.html#$tail target=display>[file tail $originalImage]</a>"
        set img2 [imageTag $testImage]
    }
    write "<tr valign=top><td width=50%>$img1</td><td width=50%>$img2</table><p>"
}

proc write {s} {
    append  ::html "$s\n"
}

proc getImageErrors {diff missingtest missingoriginal noerrors} {
    set html ""
    if {[llength $diff]} {
        foreach error $diff {
            append html "$error<br>"
        }
    }

    if {[llength $missingtest]} {
        foreach error $missingtest {
            append html "$error<br>"
        }
    }

    if {[llength $missingoriginal]} {
        foreach error $missingtest {
            append html "$missingoriginal<br>"
        }
    }
    
    if {[llength $noerrors]} {
        foreach success $noerrors {
            append html "$success<br>"
        }
    }

    return $html
}

proc getErrors {errors} {
    set html ""
    if {[llength $errors]} {
        append html "<hr><span style=background-color:red><b>There were errors:</b></span><p>"
        foreach error $errors {
            foreach {archive msg} $error break;
            append html "<a href=resultsRight.html#${archive} target=display>$archive<br></a><span style=background-color:red>$msg</span><p>" 
        }
    }
    return $html
}


###################
# Main Tcl script #
###################

if {[llength $tmpArchives]  == 0} {
    set tmpArchives [concat [glob -nocomplain  [file join $testHome *]]]
}

set tmpArchives [lsort $tmpArchives]

set archiveDirs [list]
foreach archiveDir $tmpArchives {
    if {[regexp {_results$} $archiveDir]} {continue;}
    if {[regexp {_bak$} $archiveDir]} {continue;}
    if {![regexp {^/} $archiveDir]} {
        set archiveDir [file join $testHome $archiveDir]
    }
    if {![file isdirectory $archiveDir]} {continue;}
    lappend archiveDirs $archiveDir
}


set ::rightHtml ""
set ::leftHtml ""
set ::doJava 1
set ::allHtml ""
set ::allErrors [list]
set ::archives [list]

append allHtml "<html><title>IDV Test Results</title>"
append allHtml "<style media=\"screen\" type=\"text/css\">
div {
    white-space: pre;           /* CSS 2.0 */
    white-space: pre-wrap;      /* CSS 2.1 */
    white-space: pre-line;      /* CSS 3.0 */
    white-space: -pre-wrap;     /* Opera 4-6 */
    white-space: -o-pre-wrap;   /* Opera 7 */
    white-space: -moz-pre-wrap; /* Mozilla */
    white-space: -hp-pre-wrap;  /* HP Printers */
    word-wrap: break-word;      /* IE 5+ */
}
</style>"
append allHtml "<body style=\"margin:0;\">\n"

foreach archiveDir $archiveDirs {
    set ::html ""
    set errors [list]
    set ::imageErrors(DIFF) [list]
    set ::imageErrors(MISSINGTEST) [list]
    set ::imageErrors(MISSINGORIGINAL) [list]
    set ::imageErrors(NOERRORS) [list]

    set archiveName [file tail $archiveDir]
    set bundleFile [file join $archiveDir $archiveName.isl]
    if {![file exists $bundleFile]} {
        set bundleFile [file join $archiveDir $archiveName.jnlp]
        if {![file exists $bundleFile]} {
            set bundleFile [file join $archiveDir $archiveName.xidv]
        }
    }
    if {![file exists $bundleFile]} {
        puts "No bundle: $bundleFile"
        continue;
    }

    set resultsDir [file join $topLevelResultsDir "${archiveName}_results"]
    
    if {$::doJava} {
        file delete -force $resultsDir
        file mkdir $resultsDir
    }
    file copy -force $bundleFile $resultsDir

    processBundle $archiveName $bundleFile $archiveDir $resultsDir

    set color ""
    if {![llength $::imageErrors(DIFF)] && 
        ![llength $::imageErrors(MISSINGTEST)] && 
        ![llength $::imageErrors(MISSINGORIGINAL)]} {
        set color "style=background-color:red'"
    }
    writeLeft "<li> <b><span ${color}><a href=resultsRight.html#${archiveName} target=display>${archiveName}</a></span></b> <div style=\"margin-left:10\">"
    writeLeft  [getErrors $errors]
    writeLeft  [getImageErrors $::imageErrors(DIFF) $::imageErrors(MISSINGTEST) $::imageErrors(MISSINGORIGINAL) $::imageErrors(NOERRORS)]
    writeLeft "</div>"
    writeLeft "<hr>"

    set allErrors [concat $::allErrors $errors]

    append allHtml $::html
    set fp [open $resultsHtml w]
    puts $fp [getErrors $errors]
    puts $fp [getImageErrors $imageErrors(DIFF) $imageErrors(MISSINGTEST) $imageErrors(MISSINGORIGINAL) $::imageErrors(NOERRORS)]
    puts $fp $::html
    close $fp
    puts ""
}

set endTime [clock format [clock seconds] -format {%Y-%m-%d %H:%M:%S %Z}] 
append allHtml "Tests Started:  ${startTime}<BR>"
append allHtml "Tests Finished: ${endTime}<BR>"
append allHtml "</body><html>"

writeRight "<p>"
writeRight $allHtml

set frame {<frameset cols="200,*" > 
  <frame name=left src="resultsLeft.html">
  <frame name=display src="resultsRight.html">
</frameset>}

writeFile "$resultsLeftHtml" $::leftHtml
writeFile "$resultsRightHtml" $::rightHtml
writeFile $resultsHtml $frame

puts "Tests Started:  ${startTime}"
puts "Tests Finished: ${endTime}\n"
puts "IDV Test Suite Complete!"
