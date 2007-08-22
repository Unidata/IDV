
namespace eval isl {}


foreach html [glob isl/*.html] {
   set fp [open $html r]
   set c [read $fp]
   close $fp
   while {[regexp {tagdef\s+([a-z]+)(.*)} $c match tag c]} {
       set ::taghome($tag) [file tail $html]
   }
}


proc isl::property {prop} {
    return "<i>$prop</i>"
}

proc isl::tagref {tag} {
    return "<a href=\"$::taghome($tag)#$tag\">$tag</a>"
}


proc isl::getTagIndex {} {
    global tagDict
    set html "<ul><table>"
    foreach tuple [lsort -index 0 $tagDict] {
	foreach {tag desc file} $tuple break
	append html "<tr><td><a name=\"$tag\"></a> <li><a href=\"$file\#$tag\">&lt;$tag&gt;</a></td><td> $desc</td></tr>"
        set islindex($tag) $file
    }
    append html "</table></ul>"
    set html
    set fp [open [file join isl tags.index] w]
    puts $fp [array get islindex]
    close $fp
    return $html
}

proc isl::tagdef {tag  desc {args}} {
    global tagDict
    global currentFile
    if {![info exists tagDict]} {
	set tagDict [list]
    }
    lappend tagDict [list $tag $desc   [file tail $currentFile]]
    array set A {-attrs {}}
    array set A $args
    set header "<hr><div class=\"pagesubsubtitle\">&lt;<a name=\"$tag\"></a><i>$tag</i>&gt; $desc</div>"
    set attrs ""
    foreach {name value} $A(-attrs) {
	if {$attrs == ""} {
	    set attrs "&lt;$tag "
	}
        set required [regsub {\+} $name {} name]
        set onerequired [regsub {\*} $name {} name]
        if {$required} {
            set name "<b>$name</b>"
        } elseif {$onerequired} {
            set name "<i>$name</i>"
        }
        append attrs "<br>&nbsp;&nbsp;&nbsp;&nbsp;$name=&quot;$value&quot;\n"
    }
    if {$attrs != ""} {
	append attrs "&gt;<p>"
    }
    return "$header $attrs"
}




proc isl::importxml {file} {
   lappend ::filesToCopy [file join isl $file] [file join [gen::getTargetDir] isl [file dirname $file]]
   set xml [import [file join isl $file]]
   set href "<a href=\"$file\"><img src=\"Folder.gif\" border=\"0\">$file</a>"
   set xml [xml [string trim $xml]]
   regsub {</pre>\s*</blockquote>} $xml "" xml
   append xml "\n$href</pre></blockquote>"
   set xml
}



proc isl::xml {args} {
  set xml [ht::pre [join $args " "]]
  regsub -all {(&lt;!--.*?--&gt;)} $xml {<span class="xmlcomment">\1</span>} xml
  regsub -all {(&lt;!\[CDATA\[.*?\]\]&gt;)} $xml {<span class="xmlcdata">\1</span>} xml
  regsub -all {([^\s]*)=&quot;} $xml {<span class="xmlattr">\1</span>="} xml


  foreach t [array names ::taghome] {
     regsub -all "lt;$t" $xml "lt;<a[tagref $t]" xml
     regsub -all "/$t" $xml "/<a[tagref $t]" xml
  }
  return "<blockquote>$xml</blockquote>"
}


proc isl::attr {attr} {
    return "<i>$attr</i>"
}

proc isl::tag {tag} {
    return "<i>$tag</i>"
}

proc isl::import {file} {
   set fp [open $file r ]
   set c [read $fp]
   return $c
}

