#!/opt/bin/tclsh

package require http 


proc logMsg {msg} {
    set fp [open $::logFile a]
    puts $fp $msg
    close $fp
    puts $msg
}


proc initializeState {} {

    if {[info exists ::images]} {
        foreach  id  $::images {
            catch {unset ::image_${id}}
        }
    }
    catch {unset ::idsInGroup}
    set ::defaultGroup Images
    set ::defaultExclude ""
    set ::groups [list]
    set ::images [list]
    source $::imageSource
}


proc defineExclude {from to} {
    set ::defaultExclude [list $from $to]
}

proc defineGroup {group args}  {
    array set A $args
    set ::defaultGroup $group
}


proc defineImage {url name args}  {
    array  set A {-group {} -desc {} -id {} -dz {} -ll {} -heading {0} }
    array set A $args
    if {$A(-group) == ""} {
        set tokens [split $name >]
        if {[llength $tokens]>1} {
            set A(-group) [join [lrange $tokens 0 [expr [llength $tokens]-2]] {} ]
            set name [lrange $tokens end end]
        }
    }
    if {$A(-group) == ""} {
        set A(-group) $::defaultGroup
    }
    if {$A(-id) == ""} {
        set A(-id) [string tolower $name]
        regsub -all {[\.\s-]+} $A(-id) {} A(-id)
    }
    if {$A(-desc) == ""} {
        if {![regexp {(http://[^/]+)/} $url match source]} {
            regexp {(ftp://[^/]+)/} $url match source
        }
        set A(-desc) "From: $source"
    }
    lappend ::images $A(-id)
    set A(-ll) [string trim $A(-ll)]
    array set ::image_$A(-id) [list url $url name $name group $A(-group) desc $A(-desc) dz $A(-dz) heading $A(-heading) latlon $A(-ll)]
    if {![info exists ::idsInGroup($A(-group))]} {
        set ::idsInGroup($A(-group)) [list]
        lappend ::groups $A(-group)
    }
    lappend ::idsInGroup($A(-group)) $A(-id)
}

proc getName {id} {
    set ::image_${id}(name)
}



proc getLat {id} {
    set ll [getLatLon $id]
    if {$ll!=""} {
        foreach {lat lon} [split $ll ,] break
        return $lat
    }
    return ""
}

proc getLon {id} {
    set ll [getLatLon $id]
    if {$ll!=""} {
        foreach {lat lon} [split $ll ,] break
        return $lon
    }
    return ""
}

proc getLatLon {id} {
    set ::image_${id}(latlon)
}

proc getGroup {id} {
    set ::image_${id}(group)
}

proc getUrl {id} {
    set ::image_${id}(url)    
}

proc getDesc {id} {
    set ::image_${id}(desc)    
}

proc getDZ {id} {
    set ::image_${id}(dz)    
}

proc getHeading {id} {
    set ::image_${id}(heading)    
}


proc cleanId {id} {
    set id [string tolower $id]
    regsub -all {[ ]+} $id {} id
    regsub -all {[,]+} $id {_} id
    set id
}

proc getDir {id} {
    set id [cleanId $id]
    set dir [file join $::imageDir $id]
    catch {file mkdir $dir}
    set dir
}


proc getImages {} {
    foreach  id  $::images {
        if {[catch {fetchImage $id } err]} {
            logMsg  "Error fetching image: $id\n$err\n$::errorInfo"
        } else {
        }
    }
}


proc makeIndex {} {
    foreach  id  $::images {
        makeIndexForImageSet $id
    }

    foreach group $::groups {
        append ::html "<li> $group <ul>\n"
        append ::xml "<group name=\"$group\">\n"
        foreach id $::idsInGroup($group) {
            set name [getName $id]
            append ::html "<li> <a href=\"[cleanId $id]/index.html\"/>$name</a>\n"
            set ll [getLatLon $id]
            if {$ll!=""} {
                foreach {lat lon} [split $ll ,] break
                set ll "lat=\"$lat\" lon=\"$lon\""
            }
            append ::xml "<imageset name=\"$name\"  index=\"[cleanId $id]/index.xml\"  $ll/>\n"
        }
        append ::html "</ul>\n"
        append ::xml "</group>\n"
    }
}


proc fetchImage {id} {
    set dir [getDir $id]
    set url [getUrl $id]
    set dz [getDZ $id]

    logMsg "\tFetching [getName $id] $url"
    set tok [::http::geturl $url -timeout 30000]
    set status [::http::status $tok]

    if {$status == "timeout"} {
        logMsg "\tfetch timed out"
        return
    }
    set body [::http::data $tok]
    upvar #0 $tok httpState
    array set meta $httpState(meta)
    if {![info exists meta(Last-Modified)]} {
        logMsg "\t\tNo Last-Modified"
        set date [clock format [clock seconds]]
        ##	catch {unset httpState}
        ##	return
    } else {
        set date $meta(Last-Modified)
    }
    #    logMsg "\tdone"


    catch {unset httpState}
    set dttm [clock scan $date]

    if {$dz != ""} {
        set h [clock format $dttm -format "%H"  -gmt true]
        regsub {^0} $h {} h
        set localTime [expr $h+ $dz]
        if {$localTime<0} {
            set localTime [expr 24+$localTime]
        }
        #	puts "[getName $id] h: $h -- local: $localTime [clock format $dttm -gmt true]" 
        if {$localTime>21 || $localTime < 5} {
            puts "Excluding: $id"
            return
        }
    }


    if {[info exists meta(Content-Type)]} {
        set ct $meta(Content-Type)
        if {![regexp {image/(.*)} $ct match type]} {
            logMsg "\t\tUnknown  content type: $ct" 
            ##	    puts $body
            return
        }
        set ext ".$type"
    } else {
        set ext [file extension $url]
        regsub {\?.*$} $ext {} ext
    }
    set dttmString [clock format $dttm -format "%Y%m%d%H%M%Z" -gmt true]
    set filename  [file join $dir image_${dttmString}$ext ]
    if {![file exists $filename]} {
        writeFile $filename  $body
    }
}


proc makeIndexForImageSet {id} {
    set dir [getDir $id]
    set name [getName $id]
    set group [getGroup $id]
    set format "yyyyMMddHHmmz"
    set imageXml {<?xml version="1.0" encoding="UTF-8"?>}
    
    set imageKml ""

    set imageHtml "<html><body>\n<h3>$name</h3>\n<ul>\n"
    append imageXml "\n<images base=\"$::imageRoot/[cleanId $id]\" name=\"$name\" group=\"$group\" format=\"$format\" desc=\"[getDesc $id]\">\n"
    set archiveDir [file join $dir archive]
    catch {file mkdir $archiveDir}
    set images [glob -nocomplain [file join $dir image_*]] 
    set fileList [list]
    foreach f $images {
        if {![regexp {image_([^\.]+)} $f match dttmString]} {continue}
        if {[regexp {thumb} $f match dttmString]} {continue}
        regexp {(\d+)} $dttmString match dttm
        lappend fileList [list   $dttm $dttmString $f]
    }
    set fileList [lsort -index 0  -real -decreasing $fileList]
    set cnt 0
    #Purge
    foreach tuple $fileList {
        incr cnt
        foreach {dttm dttmString file} $tuple break
        set f [file tail $file]
        if {$cnt>200} {
            ##	    puts "Renaming $file to $archiveDir"
            ##	    file rename -force $file $archiveDir
            #	    puts "Deleting old $file"
            file delete -force $file 
            continue;
        }

        set dz [getDZ $id]
        if {$dz != ""} {
            if {[regexp {\d\d\d\d\d\d\d\d(\d\d)} $dttm match h]} {
                regsub {^0} $h {} h
                set localTime [expr $h+ $dz]
                if {$localTime>18 || $localTime < 6} {
                    ##		    puts "Excluding: $id -- $localTime"
                    continue;
                }
            }
        }


        set lat [getLat $id]
        set lon [getLon $id]
        
        set imgPath "[cleanId $id]/$f"
        if {$lat!=""}  {
            if {$cnt == 1} {
                set kml $::kmlTemplate
                regsub -all {%name%} $kml "Latest Image" kml
                regsub -all {%lon%} $kml $lon kml
                regsub -all {%lat%} $kml $lat kml
                regsub -all {%url%} $kml "$::imageRoot/$imgPath" kml
                regsub -all {%extra%} $kml "" latest
                append ::latestKml  "<Folder>\n<name>$name</name>\n<visibility>1</visibility>\n"
                append ::latestKml $latest
                append ::latestKml "<NetworkLink><name>Time loop</name><visibility>0</visibility><flyToView>1</flyToView>\n<Link><href>$::imageRoot/[cleanId $id]/index.kml</href></Link></NetworkLink>\n"
                append ::latestKml  "</Folder>\n";
            } 
            if {$cnt<20} {
                regexp {(\d\d\d\d)(\d\d)(\d\d)(\d\d)(\d\d)} $dttm match yyyy  mm dd hh min  
                set formattedTime  "$yyyy-$mm-${dd}T$hh:${min}:00" 
                set timeStamp "<TimeStamp><when>$formattedTime</when></TimeStamp>"
                set kml $::kmlTemplate
                set heading  [getHeading $id]
                regsub -all {%heading%} $kml "$heading" kml
                regsub -all {%name%} $kml "$formattedTime" kml
                regsub -all {%lon%} $kml $lon kml
                regsub -all {%lat%} $kml $lat kml
                regsub -all {%url%} $kml "$::imageRoot/$imgPath" kml
                regsub -all {%extra%} $kml "$timeStamp" latest
                append imageKml $latest
            }
        } 



        append imageXml "<image time=\"$dttmString\" file=\"$f\"/>\n"
        append imageHtml "<li> <a href=\"$f\">$dttmString</a>\n"
        if {$cnt == 1} {
            catch {
                #	    set thumb [file join [file dirname $file] thumb_[file tail $file]]
                #		set thumbDim 140
                #		exec convert -interlace NONE -geometry ${thumbDim}x${thumbDim} $file $thumb
                #               set imgPath $thumb
            } err

            append ::latestHtml "<a href=\"[cleanId $id]/index.html\"><img border=\"0\" src=\"$imgPath\"></a><br>$group - $name $dttmString<p>\n"
        }
    }

    append imageXml "</images>\n"
    ##    puts "$name - $dir"
    writeFile [file join $dir index.kml] [kml $imageKml $name 0]
    writeFile [file join $dir index.xml] $imageXml
    writeFile [file join $dir index.html ] $imageHtml 0
}


proc writeFile {f c {binary 1}} {
    set fp [open $f w]
    if {$binary} {
        fconfigure $fp -translation binary
    }
    puts $fp $c
    close $fp
}




proc kml {contents name {vis 1}} {
    set kml {<?xml version="1.0" encoding="UTF-8"?>}
    append kml "\n"
    append kml {<kml xmlns="http://earth.google.com/kml/2.2">}
    append kml "\n"
    append kml "<Folder>\n<name>$name</name>\n<visibility>$vis</visibility>\n"
    append kml $contents
    append kml "\n"
    append kml {</Folder>}
    append kml {</kml>}
    return $kml
}


proc process {justIndex} {
    set ::images [list]
    catch {file mkdir $::imageDir}
    set ::html {<html><body>Images:<ul>}

    puts "Trying to open $::kmlTemplateFile"
    puts "file exists = [file exists  $::kmlTemplateFile]"
    set kmlfp  [open $::kmlTemplateFile r]
    puts "opened it"

    set ::kmlTemplate [read $kmlfp]
    close $kmlfp

    set ::latestKml ""
    set ::latestHtml {<html><title>Latest Imagery</title><body>}


    set ::xml {<?xml version="1.0" encoding="UTF-8"?>}
    append ::xml "\n<imagesets base=\"$::imageRoot\" name=\"IDV Webcams\">\n"
    initializeState
    if {!$justIndex} {
        logMsg "Getting images"
        getImages
        logMsg "Done getting images"
    }
    makeIndex
    append ::latestHtml {</body></html>}
    append ::html {</body></html>}
    writeFile [file join $::imageDir index.html] $::html
    writeFile [file join $::imageDir latest.html] $::latestHtml
    puts "writing kml: [file join $::imageDir latest.kml]"

    writeFile [file join $::imageDir latest.kml] [kml $::latestKml "Latest Webcam Images" 1]
    append ::xml "</imagesets>"
    writeFile [file join $::imageDir index.xml] $::xml
}



proc msg {msg} {
    if {$::verbose} {
        puts $msg
    }
}



set dirRoot /opt/bin/webcams/
set logRoot /data/logs/
set imgRoot /georesources/webcams/
#set dirRoot ""


set ::imageSource ${dirRoot}defineImages.tcl
set ::logFile     ${logRoot}getImages.log.out
set ::imageDir ${imgRoot}images
set ::kmlTemplateFile     ${imgRoot}photooverlay.kml
set ::imageRoot http://www.unidata.ucar.edu/georesources/webcams/images/

set ::imagePath ${imgRoot}images


set ::verbose 0
set ::justIndex 0
set ::wait 15
set ::total -1

for {set i 0} {$i < [llength $argv]} {incr i} {
    set arg [lindex $argv $i]
    switch -- $arg {
        -total {incr i; set ::total [lindex $argv $i]}
        -verbose {set ::verbose 1}
        -justindex {set ::justIndex 1}
        -wait {incr i; set ::wait [lindex $argv $i]}
        -imagedir {incr i; set ::imageDir [lindex $argv $i]}
        -imagepath {incr i; set ::imagePath [lindex $argv $i]}
        -imagesource {incr i; set ::imageSource [lindex $argv $i]}
        -imageroot {incr i; set ::imageRoot [lindex $argv $i]}
        default {
            puts "Usage getImages \n\t\[-justindex\] \n\t\[-wait <minutes>\] \n\t\[-imagedir <where to put images>\] \n\t\[-imagesource <what defines the images to load>\] \n\t\[-imageroot <URI image root>\] \n\t\[-total <total number of times>\]"
            exit
        }
    }
}


set cnt 0
while {1} {
    logMsg "fetching at [clock format [clock seconds]]"
    process $::justIndex
    logMsg "done fetching at [clock format [clock seconds]]"
    if {$justIndex} {
        break
    }
    incr cnt
    if {$total>0 && $cnt>=$total} {
        puts "Fetched $cnt images"
        exit
    }
    if {!$::wait} {exit}
    set doNext 0
    ##Wait 1 minutes
    after [expr $wait*60*1000] {set doNext 1}
    vwait doNext
}





