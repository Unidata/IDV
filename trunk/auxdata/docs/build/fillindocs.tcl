
proc get {f} {
    set f [string trim $f]
    set fp [open $f r]
    set c [read $fp]
    close $fp
    return $c
}

proc write {f c} {
    set fp [open $f w]
    fconfigure $fp -translation binary
    puts -nonewline $fp $c
    close $fp
}

proc fillin {f} {
    set c [get $f]
    if {![regexp _more_ $c]} {
	puts "Nothing to do for: $f"
	return
    }
    puts "\n---------------------------------------------------------"
    puts "File: $f"
    set lines [split $c "\n"] 
    set cnt 0;
    set content ""
    set ok 1
    set lastResponse ""
    foreach line $lines  {
	if {!$ok || ![regexp _more_ $line]} {
	    append content $line "\n"
	    incr cnt;
	    continue
	}
	set loopOk 1
	while {$loopOk} {
	    set loopOk 0	    
	    set context ""
	    for {set i [expr $cnt-1]} {$i<[expr $cnt+$::contextOffset]} {incr i} {
		if {$i == $cnt} {
		    append context ">>>" [lindex $lines $i] "    <<<\n"
		} else {
		    append context [lindex $lines $i] "\n"
		}
	    }
	    puts "\n---------------------------------------------------------"
	    puts $context
	    puts -nonewline "?:"
	    flush stdout
	    set response [gets stdin]
	    if {$response == "?"} {
		puts "l:Use last response\n+:Show more context\n-:show less context\nq:quit and don't write\ns:skip this one\nd:done with this file."
		set loopOk 1	    
		continue;
	    }

	    if {$response == "l"} {
		set response $lastResponse
	    }

	    
	    if {$response == "e"} {
		set response "On badness"
	    }

	    if {$response =="+"} {
		incr ::contextOffset 1
		set loopOk 1	    
		continue;
	    }
	    if {$response =="-"} {
		incr ::contextOffset -1
		if {$::contextOffset<=0} {set ::contextOffset 1}
		set loopOk 1	    
		continue;
	    }
	    if {$response == "q"} {
		return
	    }
	    if {$response == "s" || [string trim $response] == ""} {
		append content $line "\n"
		continue
	    }
	    if {$response == "d"} {
		append content $line "\n"
		set ok 0
		continue
	    }
	    set lastResponse $response
	    regsub _more_ $line $response line
	    append content $line "\n"
	    incr cnt;
	}
    }
    write $f $content
}

set ::contextOffset 4

#fconfigure stdin -blocking 0
foreach file  $argv {
    fillin $file
}