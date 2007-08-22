#!/opt/bin/tclsh



package require http 


proc compare {v1 v2} {
    set l1 [string length $v1]
    set l2 [string length $v2]
    if {$l1 ==$l2} {return 0}
    if {$l1 <$l2} {return -1}
    return 1
    
}

proc readFile {f} {
    set fp [open $f r]
    set c [read $fp]
    close $fp
    set c
}


proc parse {c} {
    set lines [split $c "\n"]
    set lines [lsort -command compare $lines]
    set result [list]
    foreach tuple $lines {
        set tuple [string trim $tuple]
        if {[regexp {^\#} $tuple]} {
            continue
        }
        if {$tuple == ""} continue
        foreach {name value} [split $tuple =] break
        lappend result $name $value
    }
    return $result
}



if {[llength $argv] ==0} {
    puts "Usage: translate.tcl messages.pack (The output from the IDV -recordmessages) <language_specifier  (e.g., zh, es, de, pt, fr, etc.)> <already translated file>"
    exit
}



set packFile [lindex $argv 0]
set base [file root $packFile]


set targetLanguage zh
if {[llength $argv] > 1} {
    set targetLanguage [lindex $argv 1]
}




set goodOnesContents ""
if {[llength $argv] > 2} {
    set goodOnesContents  [readFile  [lindex $argv 2]]
    array set goodOnes [parse $goodOnesContents]
}






set htmlfile $base.html
set fp [open $htmlfile w]

foreach {name value} [parse  [readFile $packFile]] {
    if {[string length $name] <=1} {continue}
    if {[info exists goodOnes($name)]} {
##        puts "exists $name"
        continue;
    }
    puts $fp "<div key=\"$name\">$value</div>"
}
close $fp


puts stderr "Copying $htmlfile to conan"
catch {exec scp  $htmlfile conan.unidata.ucar.edu:/content/software/idv/tmp} err
if {[string trim $err]!=""} {
    puts stderr "scp error: $err"
    exit
}



##fetch the translated page from google
set url "http://64.233.179.104/translate_c?hl=en&ie=UTF-8&oe=UTF-8&langpair=en%7C$targetLanguage&u=http://www.unidata.ucar.edu/software/idv/tmp/$htmlfile"


puts stderr "Translating page"
::http::config -useragent "Mozilla/5.0"
set tok [::http::geturl $url -timeout 30000]



set status [::http::status $tok]
set body [::http::data $tok]
if {$status !="ok"} {
    puts stderr "Error: $body"
    exit
}

puts stderr "Translating page - done"

set fp [open ${targetLanguage}_results.html w]
puts $fp $body
close $fp


puts stderr "Writing initial result to ${targetLanguage}_bin.pack"

set fp [open ${targetLanguage}_bin.pack w]


#set c {<div key="new"><span onmouseover="_tipon(this)" onmouseout="_tipoff()"><span class="google-src-text" style="direction: ltr; text-align: left;">New</span>XXXXX</span></div>}

if {[regexp {</span>} $body]} {
    while {[regexp {<div\s*key="([^"]+)">.*?</span>(.*?)</span>(.*$)} $body match key value body]} {
        puts $fp "$key=$value"
    }
} else {
    while {[regexp {<div\s*key="([^"]+)">(.*?)</div>(.*$)} $body match key value body]} {
        puts $fp "$key=$value"
    }
}

close $fp

set results [exec $env(JAVA_HOME)/bin/native2ascii ${targetLanguage}_bin.pack ]

puts "#Automatically translated messages:"
puts $results

if  {$goodOnesContents !=""} {
    puts "\n\n#\n#Previously translated messages\n\#\n"
    puts $goodOnesContents
}

