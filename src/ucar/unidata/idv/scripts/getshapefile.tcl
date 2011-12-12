#!/opt/bin/tclsh

set ::archiveDir /live/georesources/shapefiles


package require http 

proc createStateIndex {layer abbrev} {
    set fips  [getStateFips $abbrev]
    set url "http://arcdata.esri.com/data/tiger2000/tiger_county.cfm?sfips=$fips"
    set tok [::http::geturl $url -query "layer=$layer"]
    set body [::http::data $tok]
    if {![regexp {Below is a list of the counties} $body]} {
	error "Could not read the list of counties for layer: $layer state: $abbrev"
    }
    set pattern {filename_id"\s*VALUE="([^"]+)">&nbsp;([^\n]+)\n(.*)}
    while {[regexp $pattern  $body match value county body]} {
         regsub -all {\s+} $county _ county
	 set county [string tolower $county]
         set counties($county) $value
    }
    array get counties	    
}


proc  getShapeFile {filename layer fips county} {
    set id $::stateIndex($county)
    set url "http://arcdata.esri.com/data/tiger2000/tiger_final.cfm?RequestTimeout=500"
    set tok [::http::geturl $url -query "filename_id=$id"]
    set body [::http::data $tok]
    if {![regexp -nocase {href="([^"]+/out/data/[^"]+)"} $body match link]} {
##	writeFile  /home/jeffmc/tmp/err.out $body
        error  "No link found"
    } 
    if {[catch  {::http::geturl $link} tok]} {
	error "Could not connect to: $link\n$tok"
    }
    set root [file root $filename]
    set tmpdir [file join $::archiveDir tmp[expr rand()]]
    file mkdir $tmpdir
    set tmpfile [file join $tmpdir tmp.zip]
    writeFile  $tmpfile [::http::data $tok]
    exec unzip -d $tmpdir -o $tmpfile
    foreach f [glob [file join $tmpdir $layer*.zip]] {
	file rename -force $f [file join  $filename]
    }
    file delete -force $tmpdir
}




array set ::fipsToName { 01  {Alabama}   29  {Missouri}   02  {Alaska}    30  {Montana}   04  {Arizona}   31  {Nebraska}   05  {Arkansas}   32  {Nevada}    06  {California}   33  {New Hampshire}   08  {Colorado}   34  {New Jersey}   09  {Connecticut}   35  {New Mexico}   10  {Delaware}   36  {New York}   11  {District of Columbia}   37  {North Carolina}   12  {Florida}   38  {North Dakota}   13  {Georgia}   39  {Ohio}    40  {Oklahoma}   41  {Oregon}    15  {Hawaii}    42  {Pennsylvania}   16  {Idaho}    44  {Rhode Island}   17  {Illinois}   45  {South Carolina}   18  {Indiana}   46  {South Dakota}   19  {Iowa}    47  {Tennessee}   20  {Kansas}    48  {Texas}    21  {Kentucky}   49  {Utah}    22  {Louisiana}   50  {Vermont}   23  {Maine}    51  {Virginia}   24  {Maryland}   53  {Washington}   25  {Massachusetts}   54  {West Virginia}   26  {Michigan}   55  {Wisconsin}   27  {Minnesota}   56  {Wyoming}   28  {Mississippi}  }


array set ::fipsToAbbrev {01  AL  29  MO 02  AK 30  MT 04  AZ 31  NE 05  AR 32  NV 06  CA 33  NH 08  CO 34  NJ 09  CT 35  NM 10  DE 36  NY 11  DC 37  NC 12  FL 38  ND 13  GA 39  OH 40  OK 41  OR 15  HI 42  PA 16  ID 44  RI 17  IL 45  SC 18  IN 46  SD 19  IA 47  TN 20  KS 48  TX 21  KY 49  UT 22  LA 50  VT 23  ME 51  VA 24  MD 53  WA 25  MA 54  WV 26  MI 55  WI 27  MN 56  WY 28 MS } 


array set ::abbrevToFips {   AL  01   MO 29   AK 02   MT 30   AZ 04   NE 31   AR 05   NV 32   CA 06   NH 33   CO 08   NJ 34   CT 09   NM 35   DE 10   NY 36   DC 11   NC 37   FL 12   ND 38   GA 13   OH 39   OK 40   OR 41   HI 15   PA 42   ID 16   RI 44   IL 17   SC 45   IN 18   SD 46   IA 19   TN 47   KS 20   TX 48   KY 21   UT 49   LA 22   VT 50   ME 23   VA 51   MD 24   WA 53   MA 25   WV 54   MI 26   WI 55   MN 27   WY 56   MS 28
}




proc readFile {f} {
    set fp [open $f r]
    set c [read $fp]
    close $fp
    set c
}

proc writeFile {f c} {
    set fp [open $f w]
    fconfigure $fp -translation binary
    puts $fp $c
    close $fp
}

proc getStateFips {abbrev} {
    set abbrev [string toupper $abbrev]
    set ::abbrevToFips($abbrev)
}

proc getStateAbbrev {fips} {
    set ::fipsToAbbrev([string toupper $fips)])
}

proc getStateName {fips} {
    set ::fipsToName([string toupper $fips)])
}



proc handleError {msg} {
    puts stdout "Content-type: text/html\n"
    puts "<html><body>Error:$msg</body></html>"
    exit 0
}


proc handleRequest {} {
    set state ""
    set county ""
    set layer ""


    foreach pair [split $::env(QUERY_STRING) "&"] {
	foreach {name value} [split $pair =] break
	switch $name {
	    s {set state $value}
	    c {set county $value}
	    l {set layer $value}
	}
    }
    if {$state == ""} {handleError "No state specified"}
    if {$county == ""} {handleError "No county specified"}
    if {$layer == ""} {handleError "No layer specified"}

    if {![file exists $::archiveDir]} {
	file mkdir $::archiveDir
    }


    set fips  [getStateFips $state]
    set filename [file join $::archiveDir ${layer}_${state}_${county}.zip]
    if {![file exists $filename]} {
	set stateIndexFile [file join $::archiveDir index ${layer}_${state}.index]
	if {![file exists $stateIndexFile]} {
	    set index [createStateIndex  $layer $state]
	    writeFile $stateIndexFile $index
	    array set ::stateIndex $index
	} else {
	    array set ::stateIndex [readFile $stateIndexFile]
	}
	getShapeFile $filename $layer $fips $county
    }

    if {![file exists $filename]} {
	handleError "Error: file does not exist"
    }

    if {1} {
	set fp [open $filename r]
	fconfigure $fp -translation binary
	fconfigure stdout -translation binary
	puts stdout "Content-type: application/zip\n"
	fcopy $fp stdout
	close $fp
	exit 0
    }

}



if {[catch handleRequest err]} {
    handleError $::errorInfo
}
