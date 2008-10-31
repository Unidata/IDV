#!/bin/sh
# the next line restarts using tclsh \
exec tclsh "$0" "$@"


package require http 

namespace eval ug {}
namespace eval gen {}
namespace eval gen::nav {}
namespace eval gen::js {}
namespace eval gen::hook {}

set ::cssFiles [list]
set ::libs [list]
set ::htmlFiles [list]
set ::handlers [list]
set ::macros [list]




namespace eval html {}
proc html::page {title body {depth 0} {bodyArgs ""} } {
    if {$bodyArgs != ""} {set bodyArgs " $bodyArgs "}
    return  "<html>\n<head>[gen::getCss $depth]\n<title>$title</title>\n</head><body$bodyArgs>\n\n$body\n</body></html>"
}

namespace eval ht {}

proc ht::codep {args} {
    return "<p><code>[join $args { }]</code><p>"
}

proc ht::head {title} {
return "
<html><head>
<title>$title</title>
</head>
<body>
"
}


proc ht::foot {} {
    return "</body>"
}


proc ht::pre {c} {
    set c [gen::replace $c < "&lt;"]
    set c [gen::replace $c > "&gt;"]
    set c [gen::replace $c \" "&quot;"]
    return "<pre>$c</pre>"
}

proc ht::menu {args} {
    set sep "-&gt;"
    return "<code class=\"menu\">[join $args $sep]</code>"
}

global popupCnt
set popupCnt 0
proc ht::popup {url label args} {
    global currentFile popupCnt
    array set A [list -width 440 -height 500 -useimg 0]
    array set A $args
    if {[regexp {^/(.*)$} $url match url]} {
        set url [gen::getLink $currentFile $url]
    }
    set img ""
    if {$A(-useimg)} {
        set link [gen::getLink $currentFile images/Help16.gif]
        set img " <img src=\"$link\" border=\"0\" alt=\"Show popup\" title=\"Show popup\">"
    }
    set anchor "popup[incr popupCnt]"
    set wargs "'comments','width=$A(-width),height=$A(-height),resizable=yes,scrollbars=yes,status=yes'"
    return "<a href=\"$url\" onClick=\"window.open('$url', $wargs);return false;\">$label$img</a>"
##    return "<a name=\"$anchor\"></a><a href=\"#$anchor\" onClick=\"window.open('$url', $wargs);return false;\">$label$img</a>"
}


proc ht::subhead {id label {extra {}}} {
    return "<subhead [ht::attrs id $id] $extra>$label</subhead>"
}

proc ht::desc {args} {
    return "<meta name=\"description\" content=\"[join $args { }]\">"
}

proc ht::overview {args} {
    return "<div class=\"overview\">[join $args { }]</div>"
}

proc ht::qt {args} {
    return "&quot;[join $args { }]&quot;"
}


proc ht::index {s {name ""} {word ""}} {
    if {$word == ""} {set word $s}
    if {$name == ""} {set name $s}
    return "<a name=\"$name\" class=\"index\" word=\"$word\">$s</a>"
}

proc ht::doImage {img class {caption ""} {extra ""}} {
    if {[gen::getDoImageLinks]} {
        set href1 "<a href=\"$img\">"
        set href2 "</a>"
    } else {
        set href1 ""
        set href2 ""
    }

    if {$caption != ""} {
        set cnt [gen::getNextImageId $img $caption]
        set html  "<a name=\"image$cnt\"></a><div class=\"$class\">$href1<img  src=\"$img\" $extra alt=\"$caption\" >$href2"
        append html "<br><span class=\"caption\">Image $cnt: $caption</span></div>"
        return $html
    } else {
        return "<div class=\"$class\">$href1<img  src=\"$img\" $extra alt=\"$img\" >$href2</div>"
    }
}



proc ht::cimg {img {caption ""} {extra ""}} {
    if {[gen::getDoAll]} {
        if {$caption!=""} {
            gen::getNextImageId $img $caption
        }
	if {$caption !=""} {
	    return "<p>&nbsp;<center><img src=\"$img\" alt=\"$caption\"><br><i>$caption</i></center>&nbsp;<p>"
	}
	return  "<p>&nbsp;<center><img src=\"$img\" alt=\"$img\"></center>&nbsp;<p>"
    }
    ht::doImage $img cimg $caption $extra
}

proc ht::img {img {caption ""} {extra ""}} {
    if {[gen::getDoAll]} {
	if {$caption !=""} {
	    return "<p><img src=\"$img\"><br>$caption<p>"
	}
	return  "<p><img src=\"$img\"><p>"
    }
    ht::doImage $img img $caption $extra
}



proc ht::tblimg {args} {
    set html "<table  cellspacing=\"5\" width=\"100%\"><tr align=top>"
    set cnt [llength $args]
    set percentPer [expr int(100.0/$cnt)]
    foreach tuple  $args {
        set extra ""
        set caption ""
        foreach {src caption extra} $tuple break
        append html "<td width=$percentPer%>[ht::cimg $src $caption $extra]</td>"
    }
    append html "</table>"
}


proc gen::thumbName {img} {
    set ext [file extension $img]
    set thumb [file root $img]_thumb$ext
}


proc gen::thumb {img {dim 110x110}} {
    set to [gen::thumbName $img]
    exec convert -interlace NONE -geometry $dim $img $to
}


proc html::quote {s} {
    return "\"$s\""
}
proc html::href {url lbl} {
    return "<a href=[html::quote $url]>$lbl</a>"
}

proc gen::getIconImg {from img desc} {
    if {![gen::getDoIcons]} {
        return $desc
    }
    set top [gen::getLink $from "images"]
    return  "<img src=\"$top/$img\" border=\"0\" [gen::getIconWidthAttr]  alt=\"$desc\" title=\"$desc\">"
}


proc gen::getIconWidthAttr {} {
    set w [gen::getIconWidth]
    if {$w != ""} {return " width=\"$w\" height=\"$w\" "}
    return ""
}


proc gen::js::process {body from depth fileIdx} {
    if {![gen::getDoJSNav]} {return $body}

    set orig $body
    set imgdir [gen::getDotPath $depth]images

    set body $orig
    regsub -all {<li>\s*} $body {<li> } body
    set cnt 1
    set doImg 0
    if {$doImg} {
        while {[regsub  {<li> } $body "<li  id=\"LI$cnt\"><img src=\"$imgdir/blankdart.gif\" id=\"BIMG$cnt\"> " body]} {
            incr cnt
        }
    }  else {
        while {[regsub  {<li> } $body "<li  id=\"LI$cnt\"> " body]} {
            incr cnt
        }
    }

    incr cnt -1
    set keyExtra ""
    set files [gen::getAllNavFiles]

    if {$fileIdx >=0} {
        if {$fileIdx >0} {
            set prevFile [lindex $files [expr $fileIdx-1]]
            append keyExtra "if (s=='p' ||  s == 'P') {document.location='[gen::getLink $from $prevFile]';return false;}\n"
        }

        if {$fileIdx <[expr [llength $files]-1]} {
            set nextFile [lindex $files [expr $fileIdx+1]]
            append keyExtra "if (keyCode==13 || s=='n' ||  s == 'N') {document.location='[gen::getLink $from $nextFile]';return false;}\n"
        }
    }

    set extraJs "bulletCnt = $cnt;\n"
    return  "[gen::js::get $depth $extraJs $keyExtra]\n\n$body" 
}




proc gen::include {file {tag ""}} {
    set fp [open $file r]
    set c [read $fp]
    close $fp
    if {$tag!=""} {
        if {![regexp "<$tag>(.*)</$tag>" $c match subset]} {
            puts "Could not include tagged section: $tag from file $file"
        } else {
            set c $subset
        }
    }

    set c [subst $c]
    set c
}


proc ifInclude {if s} {
    puts $s
    if {$if} return $s
    return ""
}




proc ug::attr {attr} {
    return "<i>$attr</i>"
}

proc ug::tag {tag} {
    return "<i>$tag</i>"
}




proc ug::xml {args} {
  set xml [ht::pre [join $args " "]]
#  foreach t [array names ::taghome] {
#     regsub -all "lt;$t" $xml "lt;<a[ug::tagref $t]" xml
#     regsub -all "/$t" $xml "/<a[ug::tagref $t]" xml
#  }
  return "<blockquote>$xml</blockquote>"
}


proc gen::js::setBGColor {color} {
    set ::jsbgcolor $color
}

proc gen::js::setBorders {args} {
    set ::jsborders [list]
    foreach a $args {
        lappend ::jsborders $a
    }
}

proc gen::js::get  {depth extra keyExtra} {
    global jsborders jsbgcolor
    if {![info exists jsbgcolor]} {
        set jsbgcolor "#eeeeee"
    }
    if {![info exists jsborders]} {
        set jsborders [list Top Left]
    } 

    set borderClear ""
    set borderSet ""
    set bgSet ""
    set bgClear ""
    if {[gen::getDoJSBG]} {
        set bgSet "
            defaultLiColor = li.style.backgroundColor;
            li.style.backgroundColor='$jsbgcolor';
        "
        set bgClear {
            li.style.backgroundColor=defaultLiColor;
        }
    }

    if {[gen::getDoJSBorder]} {
        foreach b $jsborders {
            append borderClear "li.style.border${b}Style = defaultLiBorderStyle;\n"
            append borderClear "li.style.border${b}Color = defaultLiBorderColor;\n"
            append borderSet "li.style.border${b}Style = 'solid';\n"
            append borderSet "li.style.border${b}Color = 'black';\n"
        }
    }



return "
<script language=\"JavaScript\">
    //<!--
    var isNav=false;
    if (parseInt(navigator.appVersion) >= 4) {
	if (navigator.appName == \"Netscape\") {
	    isNav = true
	} 
    }

    function getCharCode(evt) {if (isNav)  {return evt.charCode;}return evt.charCode;}
    function getEvent(evt) {if (isNav)  {return evt;}  return window.event;}
    function rollTo(nextBullet, andScroll) {
	if(nextBullet>bulletCnt) nextBullet=1;
        if (nextBullet<=0) nextBullet  =1;
	doRollTo(nextBullet, andScroll);
    }
    var defaultLiColor = null;
    var defaultLiBorderStyle  = null;
    var defaultLiBorderColor  = null;
    var bulletCnt = 0;
    var currentBullet = 0;
    $extra
    function showAttrs (o) {
      var cnt = 0;
      var t = ''+o+'\\n';
      for (i in o) {
         t = t+i+'==' + o\[i\] +' ';
         if (++cnt>1 || true) {
           t = t+'\\n';
           cnt=0;
         }
      }
      var w = window.open('','comments','width=440,height=500,resizable=yes,scrollbars=yes,status=yes');
      w.document.write ('<pre>'+t+'</pre>');
//      alert(t);
    }

    function findFromId (id) {
        var o = document\[id\];
        if (o) return o;
        if (document.getElementById)  {return document.getElementById (id);}
    }
    function doRollTo(toBullet, andScroll) {
        var img = findFromId ('BIMG'+currentBullet);
        if(img) {
          img.src = '[gen::getDotPath $depth]images/blankdart.gif';
        }
        var li = findFromId ('LI'+currentBullet);
        if (li) {
           $bgClear
           $borderClear
        }
        currentBullet = toBullet;
        img = findFromId ('BIMG'+currentBullet);
        li = findFromId ('LI'+currentBullet);
        var y = 0;
        if (li) {
           
           defaultLiBorder = 'thin white';
           $borderSet
           $bgSet
           y = li.offsetTop;
//           showAttrs (li);
        }
        if (img) {
          img.src = '[gen::getDotPath $depth]images/dart.gif';
          y = img.y;
       }
       var windowY = window.scrollY;
       if (andScroll && y > (windowY+300)) {
          window.scroll (0,y-50);
       } else  if (y < (windowY-10)) {
          window.scroll (0,y-50);
       }
    }
    function keypress (evt) {
	evt = getEvent(evt);
//       showAttrs (evt);
        var keyCode =  evt.keyCode;
	var s = String.fromCharCode(getCharCode(evt));
	if (bulletCnt) {
            var shouldRoll = false;
//	    if (s=='b' || s=='+') {shouldRoll = true;}
            if (keyCode == evt.DOM_VK_F1) {shouldRoll = true;}
	    if (shouldRoll) {rollTo(currentBullet+1, 0);return false;}
//	    if (s=='B' || keyCode == evt.DOM_VK_F3) {rollTo(currentBullet+1, 0);return false;}
//if (s=='-' || keyCode == evt.DOM_VK_F2) {rollTo (currentBullet-1, 0);return false;}
            if (keyCode == evt.DOM_VK_F2) {rollTo (currentBullet-1, 0);return false;}
//            if (s=='=') {window.scroll (0,0);return false;}
//            if (s>='1' && s <='9') {rollTo (s,0);return false;}
        }
        $keyExtra
    }
    document.onkeypress =  keypress;
    function initBullet() {if (bulletCnt) {roll();}}
    function initPresentation() {initBullet();}
    document.onload = 'initPresentation();'
    //--></script>
"

}





proc gen::getFile {f} {
    set fp [open $f r]
    set c [read $fp]
    close $fp
    foreach {name value} $::macros {
	regsub -all $name $c $value c
    }
    return $c
}

proc gen::writeFile {f c} {
    set fp [open $f w]
    fconfigure $fp -buffering full -buffersize 262144
    puts $fp $c
    close $fp

}









##
##Check for tags of the form:
##<glossary definition="the definiton">Word</glossary>
##And insert the word/definition pair into the glossary
##
proc gen::checkForGlossary {content} {
    if {![gen::getDoGlossary]} { return}
    set overrideWord ""
    while {[regexp {<glossary([^>]*?)definition\s*?=\s*?"(.*?)"([^>]*?)>(.*?)</glossary>(.*$)} $content match attr1 definition attr2 word content]} {
#"
        set attr "$attr1 $attr2"
        if {[regexp {word\s*=\s*"([^"]+)"} $attr match override]} {
#"
            set word $override
        }  
        glossary [string trim $word]  [string trim $definition]
    }
}


proc gen::processIfs {from content} {
    set body ""

    while {1} {
        set idx1 [string first  "<ifdef" $content]
        if {$idx1<0} {
            append body $content
            break;
        }
        set idx2 [string first ">" $content $idx1]
        if {$idx2<0} {
            append body $content
            break;
        }

        set idx3 [string first  "</ifdef>" $content $idx2]
        if {$idx3<0} {
            append body $content
            break;
        }

        append body [string range $content 0 [expr {$idx1-1}]]
        set ifBody [string range $content [expr {$idx2+1}] [expr {$idx3-1}]]
        set if  [string trim [string range $content [expr {$idx1+7}] [expr {$idx2-1}]]]
        if {[gen::isDefined $if]} {
            append body $ifBody
        }
        incr idx3 8
        set content [string range $content $idx3 end]
    }



##Now do the ifndefs
    set content $body
    set body ""
    while {1} {
        set idx1 [string first  "<ifndef" $content]
        if {$idx1<0} {
            append body $content
            break;
        }
        set idx2 [string first ">" $content $idx1]
        if {$idx2<0} {
            append body $content
            break;
        }

        set idx3 [string first  "</ifndef>" $content $idx2]
        if {$idx3<0} {
            append body $content
            break;
        }


        append body [string range $content 0 [expr {$idx1-1}]]
        set ifBody [string range $content [expr {$idx2+1}] [expr {$idx3-1}]]
        set if  [string trim [string range $content [expr {$idx1+7}] [expr {$idx2-1}]]]



        if {![gen::isDefined $if]} {
            append body $ifBody
        }
        incr idx3 9
        set content [string range $content $idx3 end]
    }




    set body
}

proc gen::isDefined {tag} {
    info exists ::defined($tag)
}

proc gen::define {tag} {
    set ::defined($tag) 1
}



proc gen::addSubHead {from content} {
    set body ""
    set cnt 0
    set levelLabel [gen::getLevelLabel $from]
#    set levelLabel ""
    while {1} {
        set idx1 [string first  "<subhead" $content]
        if {$idx1<0} {
            append body $content
            break;
        }
        set idx2 [string first  "</subhead>" $content $idx1]
        if {$idx2<0} {
            append body $content
            break;
        }
        incr idx2 9
        append body [string range $content 0 [expr {$idx1-1}]]
        set tag [string range $content $idx1 $idx2]
        incr idx2
        set content [string range $content $idx2 end]
        set intoc true
        if {[regexp {(.*)>(.*)<} $tag match attrs label]} {
	    foreach {attr dflt}  {id "" intoc true desc ""} {
		set $attr $dflt
		regexp   "${attr}=\"(\[^\"\]+)\"" $attrs match $attr
		set $attr [string trim [set $attr]]
	    }

            foreach id [split $id ,] {
                append body "<a name=\"$id\"></a>"
            }
            append body "<div class=\"pagesubtitle\">$levelLabel.$cnt $label</div> "
            incr cnt
            if {$intoc != "false"} {set intoc 1} else {set intoc 0}
            #set intoc 1
            gen::definePage "$from#$id" "" $from  0 $intoc  link [string trim $label] ""
	    gen::setDescription  "$from#$id" $desc
        }
    }
    set body
}



proc gen::processSlideShow {from to content} {
    set body ""
    set cnt 0
#    set levelLabel ""
    set root [file root $to]
    while {1} {
        set idx1 [string first  "<slideshow" $content]
        if {$idx1<0} {
            append body $content
            break;
        }
        set idx2 [string first  "</slideshow>" $content $idx1]
        if {$idx2<0} {
            append body $content
            break;
        }
        incr cnt
        set ss [string range $content $idx1 $idx2]
        set linkLabel "Slideshow"
        regexp {<slideshow[^>]*title\s*=\s*"([^"]+)"} $ss match linkLabel
        #"
        append body [string range $content 0 [expr {$idx1-1}]]
        incr idx2 12
        set content [string range $content $idx2 end]
        set ssHtml ""
        set windowHeight 550
        set windowWidth 440
        set wargs "'comments','width=$windowWidth,height=$windowHeight,resizable=yes,scrollbars=yes,status=yes'"

        set ssFile ${root}_ss${cnt}_
        puts $ssFile
        append body "<a href=\"javascript:void(0);\" onClick=\"javascript:window.open('[file tail $ssFile]1.html', $wargs);return false;\" >$linkLabel</a>"


        set slides [list]
        while {[regexp {<img\s*src="([^"]+)"\s*(title\s*=\s*"([^"]+)")?\s*>(.*$)} $ss match src titleOuter title ss]} {
            lappend slides $src  $title
        }
        set numSlides [expr [llength $slides]/2]
        set slideCnt 1
        foreach {src title} $slides  {
            set slideHtml "<head><title>$linkLabel -- Slide $slideCnt/$numSlides</title></head><body>"
            if {$slideCnt==1} {
                set nav "&nbsp;Prev&nbsp;" 
            } else {
                set prevIdx [expr {$slideCnt-1}]
                set nav "&nbsp;<a href=\"[file tail $ssFile]$prevIdx.html\">Prev</a>&nbsp;" 
            }
            if {$slideCnt==$numSlides} {
                append nav "Next&nbsp;"
            } else {
                set nextIdx [expr {$slideCnt+1}]
                append nav "<a href=\"[file tail $ssFile]$nextIdx.html\">Next</a>&nbsp;"

            }

            append slideHtml "<table width=\"100%\" cellpadding=\"10\">"
            append slideHtml "<tr><td valign=\"top\" align=\"right\">$nav</td>"
            append slideHtml "<tr><td height=\"[expr $windowHeight-120]\" width=\"90%\" align=\"center\" valign\"top\">"
            append slideHtml "<a href=\"$src\" ><img border=\"0\" src=\"$src\"></a><p>$title<p></td>"
            append slideHtml "<tr><td align=\"right\" valign=\"bottom\">$nav</td>"
            append slidetml "</table>"
            gen::writeFile $ssFile$slideCnt.html $slideHtml
            incr slideCnt
        }    
    
    }
    set body
}




proc gen::addGlossary {from content} {
    if {![gen::getDoGlossary]} {return $content}
    global glossary
    set orig $content
    set top [gen::getLink $from ""]
    set path [gen::getDotPath [gen::getDepth $from]]

    set orig $content
    set wordList [list]
    set attrList [list]
    set overrideWord ""
    while {[regexp {<glossary([^>]*)>(.*?)</glossary>(.*$)} $content match  attrs  word content]} {
        lappend attrList $attrs
        lappend wordList [string trim $word]
    }
    set content $orig

    foreach word $wordList attr $attrList {
        if {[regexp {word\s*=\s*"([^"]+)"} $attr match override]} {
#"
            set word $override
        }  
        set canonWord [string tolower $word]
        if {![info exists glossary($canonWord)]} {
            puts "No glossary entry:$canonWord:"
            foreach  n [array names glossary] {
                puts "\t:$n:"
            }
            continue;
        }
        regsub "<glossary.*?>(.*?)</glossary>"  $content "<a href=\"${path}glossary.html#$canonWord\">\\1</a>" content
    }
    return $content
}


proc gen::checkForIndexDefinitions {file title content} {
    if {![gen::getDoIndex]} { return}
    global indexInfo
    set orig $content
    while {[regexp {<a\s*?name\s*?=\s*?"([^"]+)"\s*?class\s*?=\s*?"index"\s*?>(.*?)</a>(.*)$} $content match name value content]} {
#"
           set value [gen::up1 [string trim $value]]
           if {![info exists indexInfo($value)]} {
               set indexInfo($value) [list]
           }
           lappend indexInfo($value) $title
           lappend indexInfo($value) $file\#$name 
    }

    set content $orig
    while {[regexp {<a\s*?name\s*?=\s*?"([^"]+)"\s*?class\s*?=\s*?"index"\s*?word=\"(.*?)"\s*?></a>(.*)$} $content match name value content]} {
#"
           set value [gen::up1 [string trim $value]]
           if {![info exists indexInfo($value)]} {
               set indexInfo($value) [list]
           }
           lappend indexInfo($value) $title
           lappend indexInfo($value) $file\#$name 
    }



}



proc  gen::extraFormatHtml {path c} {
    return $c
}





proc  gen::formatHtml {path c justExtra} {
    if {!$justExtra} {
        set base "  "
        array set tabs [list 0 "" 1 $base 2 "$base$base"  3 "$base$base$base"  4 "$base$base$base$base"   5 "$base$base$base$base$base"]  
        regsub -all {\s*</li>\s*} $c "</li>\n" c
        regsub -all {\s*<li>\s*} $c "\n<li>" c
        regsub -all {\s*<ul([^>]*>)\s*} $c "\n\n<ul\\1\n" c
        regsub -all {\s*<ol([^>]*>)\s*} $c "\n\n<ol\\1\n" c
        regsub -all {\s*</ul>\s*} $c "\n</ul>\n\n" c
        regsub -all {\s*</ol>\s*} $c "\n</ol>\n\n" c
        regsub -all {\s*<li>} $c "\n\n<li>" c
    }


    set c [gen::extraFormatHtml $path $c]


    if {!$justExtra} {
        set html ""
        set tab ""
        set tabCnt 0
        set inLi 0
        set lines  [split $c \n];
        set maxLength 80
        set inPre 0
        foreach line $lines {
            set line [string trim $line]

            if {[regexp <pre> $line]} {
                set inPre 1
            }
            if {$inPre} {
                append html "$line\n"
                if {[regexp </pre> $line]} {
                    set inPre 0
                }
                continue;
            }


            if {[regexp {^</ul>} $line]|| [regexp {^</ol>} $line]} {
                incr tabCnt -1
            }  

            if {$tabCnt<0} {set tabCnt 0}
            if {$tabCnt>5} {set tabCnt 5}


            if {[regexp {^<li>} $line]} {set inLi 0}
            if {[regexp {^<ul} $line]|| [regexp {^<ol} $line]} {set inLi 0}

            set indent "$tabs($tabCnt)"
            if {$inLi} {append indent "    "}
            
            if {[string length $line]+[string length $indent]> $maxLength} {
                set idx1 $maxLength
                incr idx1 -1 
                while {$idx1>0 && ([string range $line $idx1 $idx1] != " ")}   {
                    incr idx1 -1
                }
                set line1 "[string range $line 0 $idx1]\n[string range $line $idx1 end]"
                regsub {(\[[^\]]+)\n([^\]]+\])} $line1 "\n\\1 \\2"  line1
                foreach {l1 l2} [split $line1 \n] break
                append html "$indent$l1\n"
                append html "$indent$l2\n"
            } else {
                append html "$indent$line\n"
            }


            if {[regexp {^<ul} $line]|| [regexp {^<ol} $line]} {
                set inLi 0
                incr tabCnt
            } elseif {[regexp {^<li>} $line]} {
                set inLi 1
            }
            if {[regexp {</li>} $line]} {
                set inLi 0
            }
        }
        set c $html
    }
    gen::writeFile $path $c
    return $c
}





proc gen::getTitleOverviewBody {path {canUseBodyForOverview 0} {htmlRaw 0}} {
    if {![file exists $path]} {
        puts "File not found: $path"
        return  [list "" "" ""] 
    }
    set content [string trim [gen::getFile $path]]
    set ::currentFile $path
    gen::setImageCnt $path 0
    gen::setImageInfo $path [list]



    foreach {pattern func} $::handlers {
        if {[regexp $pattern $path]} {
            set content [$func $path $content]
            break
        }
    }


    
    if {[gen::getDoTclEvaluation]} {
        set ::currentFile $path
        if {[catch {set content [subst -novariables $content]} err]} {
            puts "Error evaluating $path\n$::errorInfo"
        }
    }

    set title ""
    set overview ""
    set body $content

##    set content "hello<title>This is the title</title>Content"
    set idx1 [string first <title> $content]
    if {$idx1<0} {
	set idx1 [string first <TITLE> $content]
    }
    if {$idx1<0} {
	puts "No title found in: $path"
    } else {
	set idx2 [string first </title> $content $idx1]
	if {$idx2<0} {
	    set idx2 [string first </TITLE> $content $idx1]
	}
	if {$idx2<0} {
	    puts "No title found in: $path"
	} else {
	    incr idx1 7
	    incr idx2 -1
	    set title [string range $content $idx1 $idx2]
	    incr idx2 9
	    set body [string range $content $idx2 end]
#	    puts "title:$title"
#	    puts "body:$body"
	}

    }
##    if {![regexp -nocase {<title>(.*)</title>(.*)$} $content match title body]} {
##        puts "No title found in: $path"
##    } 
    if {![regexp -nocase {<meta\s*name\s*=\s*\"description\"\s*content\s*=\s*\"([^\"]+)\"} $content match overview]} {
        if {![regexp -nocase {<div\s*?class=\s*?\"overview\"\s*?>(.*?)</div>} $content match overview]} {
            if {$canUseBodyForOverview} {
                if {![regexp -nocase {<body>(.*)</body>\s*$} $content match  overview]} {
                    regexp -nocase {</title>(.*)$} $content match  overview
                }
            } 
        }
    }



    regexp -nocase {<body>(.*)</body>} $body match body


    if {$overview == ""} {
##        puts "No overview found in: $path"
    }

    gen::checkForIndexDefinitions $path $title $content
    gen::checkForGlossary   $content

    if {$htmlRaw} {
        set body $content
    } 
    return [list [string trim $title] [string trim $overview] $body]
}


proc   schedule {day from to args} {
    array set A {-title {} -links {}}
    array set A $args

    set time "$from-$to"
    foreach path $A(-links) {
        if {![gen::fileExists $path]} {
            puts "Error bad path in schedule file: $path"
        }
    }

    if {![info exists ::schedule($day)]} {
        set ::schedule($day) [list]
    }
    lappend ::schedule($day) [list $time $A(-title) $A(-links)]
}





proc gen::getToc {path} {
    if {![gen::getIncludeInToc $path]} {return [list "" "" ""]}
    set aname "<a name=\"$path\"></a>"
    set toc "$aname <b>[gen::getLevelLabel $path]</b> <a href=\"$path\">[gen::getTitle $path]</a><br>\n" 
    set fulltoc "$aname  <b>[gen::getLevelLabel $path]</b> <a href=\"$path\">[gen::getTitle $path]</a>\n<br>[gen::getOverview $path]<br>\n"
    set frametoc "<tr><td class=\"framenav\"  nowrap>[gen::getNbsp $path]<a href=\"$path\" target=\"right\" style=\"font-size:9pt;\">[gen::getTitle $path]</a></td></tr>\n"
    set didone 0
    foreach child [gen::getChildren $path] {
        foreach {childToc childFullToc  childFrameToc} [gen::getToc $child] break
        if {$childToc == ""} {continue}
        if {!$didone} {
            append toc "<ul>\n"
            append fulltoc "<ul>\n"
        }
        set didone 1
        append toc $childToc
        append fulltoc $childFullToc
        append frametoc $childFrameToc
    }
    if {$didone} { 
        append toc "</ul>\n"
        append fulltoc "</ul>\n"
    }
    list $toc $fulltoc $frametoc
}



proc gen::walkTree {indexFile {parent ""}} {
    if {![file exists $indexFile]} {
        puts "index file not found: $indexFile"
    }
    set dir [file dirname $indexFile]
    if {$dir == "."} {set dir ""}

    set ::alldirs($dir) 1
    set ::alldirs([file join $dir images]) 1

    
    foreach line [split [gen::getFile $indexFile] "\n"] {
        set line [string trim $line]
        if {$line == "exit"} {
	    return
	}
        if {$line == "" || [regexp "^#" $line]} {continue;}
        if {[regexp {^-} $line]}  {
            set file [string range $line 1 end]
            set file [file join $dir $file]
            foreach f [glob  $file]  {
                set fileDir [file dirname $f]
                if  {![file exists $f]} {
                    puts "Error: file $f does not exist."
                } else {
                        
                    lappend ::filesToCopy  $f [file join [gen::getTargetDir] [file join $dir $fileDir] ]
##                    puts "lappend ::filesToCopy  $f [file join [gen::getTargetDir] [file join $dir $fileDir]]"
##                    lappend ::filesToCopy  $f [file join [gen::getTargetDir] $dir [file dirname $f]]
                }
            }
            continue
        }
        set includeInNav 1
        set  includeInToc 1
        if {[regsub {^\+} $line {} line]}  {
            set includeInNav 0
        }
        if {[regsub {^\|} $line {} line]}  {
            set includeInNav 0
            set includeInToc 0
        }
        set indents ""
        set file $line
        regexp {^(>*)([^>]+)$} $line match indents file
#        puts "$line -- I: $indents"
        set currentParent $parent

        if {[string length $indents]} {
            set tmp [string range $indents 1 end]
            while {$tmp!="" && ![info exists parents($tmp)]} {
                set tmp [string range $tmp 1 end]
            }
            if {[info exists parents($tmp)]} {
                set currentParent $parents($tmp)
            }
        } 


        if {[regexp {^title:(.*)} $file match title]} {
            set overview ""
	    set body ""
	    if {![regexp {^([^:]+):([^:]*):(.*)$} $title match title overview body]} {
		regexp {^([^:]+):(.*)$} $title match title overview
	    }
            set fileName "page_[string tolower $title]"
            regsub -all {[^a-zAZ0-9_]} $fileName {} fileName
            append fileName ".html"

            ##Check for the title>filename syntax
            if {[regexp {^(.*)>(.*)$} $title match title fileName]} {
            #    puts "Got filename: $fileName"
            }


            set parents($indents) $fileName
	    if {[regexp {^\[} [string trim $overview]]} {
		set overview [subst $overview]
	    }
	    if {[regexp {^\[} [string trim $body]]} {
		set body [subst $body]
	    }
            gen::definePage $fileName "" $currentParent  $includeInNav $includeInToc virtual $title $overview $body
            continue
        }

        if {[regexp {^if:(.*):(.*)} $file match if file]} {
            if {![gen::isDefined $if]} {
                continue;                
            }
        }


        if {[regexp {^link:(.*)} $file match link]} {
            set overview ""
            set file $link
            set title ""
            regexp {^([^:]+):(.*)$} $link match file title
            set file [file join $dir $file]
            set id [gen::getUniqueId]
            gen::setUniqueId [expr {$id+1}]
            set parents($indents) $file
            gen::definePage $file "" $currentParent  $includeInNav $includeInToc link [string trim $title] ""
            continue
        }


        foreach file [split $file ,] {
            set file [string trim $file]
            if {$file==""} {continue;}
            set files [glob -nocomplain [file join $dir $file]] 
            if {[llength $files] ==0} {
                puts "File: $file does not exist"
            }
            
            foreach file $files {
                if {![file exists $file]} {
                    puts "The directory or file, $file, defined in $indexFile does not exist"
                    continue;
                }
                if {[gen::fileExists $file]} {
                    puts "Already have loaded file: $file"
                    continue;
                }
                set actualFilePath $file
                while {[regsub -all {^\.\.} $file {} file]} {
                }
                regsub -all {^/} $file {} file
                if {$file != $actualFilePath} {
                    puts "***** $file"
                }

                if {[file isdirectory $file]} {
                    set file [file join $file [gen::getIndexFile]]
                }
                if {[regexp {index$} $file]} {
                    walkTree $file $currentParent
                    continue
                }
                
                if {$includeInNav ||$includeInToc} {
                    set parents($indents) $file
                }
                gen::definePage $file $actualFilePath $currentParent  $includeInNav $includeInToc real

            }
        }
    }
}



proc gen::definePage {file actualFilePath parent includeInNav includeInToc {pageType real} {title ""} {overview ""} {dfltBody ""}} {
    if {[gen::getTopFile] == ""} {
        gen::setTopFile $file
    }

    if {[gen::fileExists $file]} {
        puts "Already have added: $file"
        return
    }

    set ::info($file,Children) [list]

    if {$includeInNav} {
        set files [gen::getAllNavFiles]
        lappend files $file
        gen::setAllNavFiles $files
    } else {
        set files [gen::getAllNonNavFiles]
        lappend files $file
        gen::setAllNonNavFiles $files
    }




    gen::setPageType $file $pageType

    switch $pageType {
        real {
            foreach {title overview body} [gen::getTitleOverviewBody $actualFilePath 0] break
        }
        raw {
            foreach {title overview body} [gen::getTitleOverviewBody $actualFilePath 0 1] break
        }
        link {
            set body $overview
        }
        virtual {
            set body $overview
	    append body $dfltBody
        }
    }

    set siblingOrder 0
    if {![gen::fileExists $parent]} {
        if {[gen::getNumberTop]} {
            set levelLabel "1"
        } else {
            set levelLabel ""
        }
    } elseif {!$includeInNav && !$includeInToc &&  $pageType!="link"} {
        set levelLabel ""
    } else {
        set siblingOrder [llength [gen::getChildren $parent]]
        set parentLevelLabel [gen::getLevelLabel $parent]
        set levelLabel ""
        set hierarchyLevel [expr [gen::getHierarchyLevel $parent]+1]
        if {$includeInToc || $pageType != "real"} {
            if {$parentLevelLabel == ""} {
                if {$hierarchyLevel == 1} {
                    set levelLabel  "[expr $siblingOrder+1]"
                } else {
                    set levelLabel  "$siblingOrder"
                }
            } else {
                set levelLabel  "$parentLevelLabel.$siblingOrder"
            }
        }
    }



    set dirName [file dirname $file]
    set ::alldirs($dirName) 1
    set ::alldirs([file join $dirName images]) 1

    gen::setParent $file $parent
    gen::setTitle $file  $title
    gen::setOverview $file  $overview
    gen::setDescription $file  ""
    gen::setPopupText $file  [list]
    gen::setDetailsText $file  ""
    gen::setLevelLabel $file  $levelLabel
    gen::setIncludeInNav   $file $includeInNav
    gen::setIncludeInToc    $file $includeInToc
    gen::setDepth   $file  [expr [llength [file split $file]]-1]
    gen::setChildren   $file [list]

    if {[gen::fileExists $parent]} {
        set nbsp "[gen::getNbsp $parent]&nbsp;&nbsp;"
        set space  "[gen::getSpace $parent]  "
    } else {
        set nbsp "&nbsp;&nbsp;"
        set space ""
    }
    gen::setNbsp $file $nbsp
    gen::setSpace  $file $space
    gen::msg "$space $title"
    if {$includeInNav || $includeInToc || $pageType=="link"} {
        gen::addChild $parent  $file
    }

    if {$pageType == "real"} {
        set body [gen::addSubHead $file $body ]
    }


    gen::setDoTemplate $file 1
    gen::setBody $file $body
    gen::hook::definePage $file

}

proc gen::hook::definePage {file} {

}




proc gen::getDotPath {depth} {
    if {$depth <= 0} {
	return "./"
    }
    set path ""
    while {$depth >0} {
        append path "../"
        incr depth -1
    }
    return $path
}

#Return the html links to any of the css files we have
proc gen::getCss {depth} {
    set html ""
    foreach cssFile [gen::getCssFiles] {
        append html  "  <link rel=\"stylesheet\" type=\"text/css\" href=\"[gen::getDotPath $depth][file tail $cssFile]\" title=\"Style\">\n"
    }


    if {[gen::getDoJSNav]} {
            append html  " <script language=\"JavaScript1.2\" src=\"[gen::getDotPath $depth]/unidata.js\"></script>\n"
    }
    return $html
}



proc gen::getLink {from to} {
    set fromDir [file dirname $from]
    set toDir [file dirname $to]
    if {$fromDir == $toDir} {
        return [file tail $to]
    }
    
    if {[regexp "^$fromDir" $toDir]} {
        regsub "^$fromDir/" $to {} to
        return $to
    }
    set depth [llength [file split [file dirname $from]]]
    return [gen::getDotPath $depth]$to
}


##Create the file state accessor methods
foreach name [list Body Depth Children LevelLabel IncludeInToc IncludeInNav Title Nbsp Space Parent Overview Description PageType ImageCnt ImageInfo DoTemplate DetailsText PopupText] {
    proc gen::get$name {path} "
        set ::info(\$path,$name)
    "
    proc gen::set$name {path v} "
        set ::info(\$path,$name) \$v
    "
    proc gen::exists$name {path} "
        info exists ::info(\$path,$name) 
    "
}

proc gen::addPopupText {from title content} {
    set l [gen::getPopupText $from ]
    lappend l $title $content
    gen::setPopupText $from $l
}


proc gen::getNextImageId {img caption} {
    global currentFile
    set dir [file dirname $currentFile]
    set img [file join $dir $img]
##    puts "Current: $currentFile - img = $img"
    set path $::currentFile
    set id [gen::getImageCnt $path]
    incr id
    gen::setImageCnt $path $id
    set imageInfo  [gen::getImageInfo $path]
    lappend imageInfo  $id $img $caption
    gen::setImageInfo $path $imageInfo
    set id
}



proc gen::getLevelLabel {path} {
    if {![gen::getNumbering]} {
        return ""
    }
    if {![gen::fileExists $path]} {return ""}
    set ::info($path,LevelLabel)
}


proc gen::getHierarchyLevel {path} {
    if {![gen::fileExists $path]} {return -1}
    return [expr [gen::getHierarchyLevel [gen::getParent $path]]+1]
}




proc gen::addChild {parent child} {
    if {[gen::fileExists $parent]} {
        lappend ::info($parent,Children) $child
    } else {
        lappend ::top $child
    }
}



proc gen::fileExists {path} {
    info exists ::info($path,Title)
}


proc gen::getTopImage {from img} {
    return [gen::getLink $from "images"]/$img
}


proc gen::initMacroArray {} {
    set l [list]
    foreach macro {title head navprev navnext navtoc navindex navglossary  navimages navbreadcrumb level dotpath childlist navschedule} {
        lappend l $macro
        lappend l ""
    }
    return $l
}

proc gen::getHeadContent {from depth} {
    return "[gen::getCss $depth]"
}


set ::moreId 0
proc ht::more {content {more {More...}} {less {...Less}}} {
    if {![gen::getDoJSNav]} {
         return $content
    }

    set  base  [incr ::moreId]
    set  divId  "morediv_$base"
    set  linkId "morelink_$base"
    set  moreLink   "javascript:showMore('$base')"
    set  lessLink   "javascript:hideMore('$base')"
    set  text   "<br><a id=\"$linkId\" href=\"$moreLink\">$more</a><div class=\"moreblock\" id=\"$divId\">$content<br><a href=\"$lessLink\">$less</a></div>"
}


proc gen::getCommonContent {{from ""}} {
    if {$from == ""} {set from [gen::getTopFile]}
    set depth         [gen::getDepth $from]
    return [list  \
                title      [gen::getTitle $from]\
                level      [gen::getLevelLabel $from]\
                depth      [gen::getDepth $from]\
                dotpath    [gen::getDotPath $depth]\
                head       [gen::getHeadContent $from $depth]\
                filename   $from \
                ]
}


proc gen::getCommonNav {{from ""}} {
    if {$from == ""} {set from [gen::getTopFile]}
    return [list  \
                navindex       [gen::nav::getIndex $from]\
                navtop         [gen::nav::getTop  $from]\
                navtoc         [gen::nav::getToc  $from]\
                navglossary    [gen::nav::getGlossary  $from]\
                navimages      [gen::nav::getImages  $from]\
                navframe       [gen::nav::getFrame  $from]\
                navschedule    [gen::nav::getSchedule  $from]\
                ]

}


proc gen::processAllHref {from href} {

    ##Look for http://urls

    if {[string first http $href]==0} {
	return "$href"
    }

    ##Is this a local link
    if {[string first {#} $href]==0} {
        regsub -all / $from _ from
	return "#${from}_[string range $href 1 end]"; 	#
    }
    set dir [file dirname $from]
    if {$dir=="."} {set dir ""}

    set tmp $href
    set href ""
#    puts "from:$from dir = $dir"
    while {[string first "../" $tmp]==0} {
	set tmp [string range $tmp 3 end]
	set dir [file dirname $dir]
    }
    if {$dir=="." || $dir==""} {
	set href "$tmp"
    } else {
	set href "$dir/$tmp"
    }
    regsub -all {#} $href _ href
    regsub -all / $href _ href
#    puts "\thref=\#$href"
    return "#$href"
}

proc gen::getAllPagesTitle {} {
    return "All pages"
}



proc gen::writeToAll  {from body} {
    if {![gen::getDoAll]} {return}
    set popupTextList [gen::getPopupText $from]
    if {[llength $popupTextList]} {
        append body "<hr><b>Footnotes:</b>"
        foreach {title content} $popupTextList {
            append body  [ht::tag div class embeddedpopup body $content] "<p>&nbsp;"
        }
        append body "<hr>"
    }

    regsub -all {onClick\s*=\s*"window.open} $body {xxx="} body
    global allfp allfpnoimg
    if {![info exists allfp]} {
        set allfp [open  [file join [gen::getTargetDir] [gen::getAllFileName]] w]
        puts $allfp "<html>\n<head>[gen::getCss 0]\n<title>[gen::getAllPagesTitle]</title>\n</head><body>"
        set allfpnoimg [open  [file join [gen::getTargetDir] allnoimages.html] w]
        puts $allfpnoimg "<html>\n<head>[gen::getCss 0]\n<title>[gen::getAllPagesTitle] -- no images</title>\n</head><body>"
    }
    set title [gen::getTitle  $from]
    set level [gen::getLevelLabel  $from]
    set dir [file dirname $from]
    set bodynoimg $body
    regsub -all {<img src=\"(.*?)\"(.*?)>} $bodynoimg "Image: <a href=\"$dir/\\1\">$dir/\\1</a>" bodynoimg

    regsub -all {<img src=\"(.*?)\"(.*?)>} $body "<img src=\"$dir/\\1\" \\2>" body
    regsub -all -nocase {<div\s*class="pagesubtitle">([^<]+)</div>} $body "<h3>\\1</h3>" body
    regsub -all -nocase {<code\s*class="menu">([^<]+)</code>} $body "<b>\\1</b>" body 
    
    set aname $from
    regsub -all {/} $aname _ aname
    regsub -all -nocase {<a\s*name="([^"]+)"} $body "<a name=\"${aname}_\\1\"" body

#    puts "aname=$aname from=$from"
    regsub -all {\[} $body {\\[} body
    if {[regsub -all -nocase {<a[^>]+href\s*=\s*"(([^">\s]+.html)?(\#[^">\s]+)?)"} $body "<a href=\"\[gen::processAllHref $from \{\\1\}\]\"" body]} {
        #
        set body [subst -novariables $body]
    }


    puts $allfp "<h1><a name=\"$aname\"></a>$level $title</h1>"
    puts $allfpnoimg "<h1>$level $title</h1>"

    puts $allfp     $body
    puts $allfpnoimg  $bodynoimg
}


proc gen::mkdir {dirname} {
    if {[file exists $dirname]} return
    set dirname [string trim $dirname]
    if {$dirname == "" || $dirname == "."} return
    gen::mkdir [file dirname $dirname]
    file mkdir $dirname
} 



proc gen::doLinkCheck {from HTML} {
    set orig $HTML
    set dir [file dirname $from]
    set html [string tolower $HTML]

    while {1} {
        set idx1 [string first { href} $html]
        if {$idx1<0} {break}
        set idx2 [string first {>} $html $idx1]
        if {$idx2<0} {break}

        set  link [string range $html $idx1 $idx2]
        set  LINK [string range $HTML $idx1 $idx2]

        incr idx2 3
        set html [string range $html $idx2 end]
        set HTML [string range $HTML $idx2 end]

        if {![regexp -nocase {href\s*=\s*"([^"]+)"} $LINK match url]} {
#"
            if {![regexp -nocase {href\s*=\s*([^\s>]+)?} $LINK match url]} {
                continue
            }
        }

        if {[info exists seen($url)]} {
            continue;
        }
        set seen($url) 1

        if {[regexp {^[a-zA-Z]+:} $url]} {continue}
        set hashIdx [string first \# $url]
        if {$hashIdx == 0} {
            set name [string range $url 1 end]
            if {![regexp -nocase "<a\\s*name\\s*=\\s*\"?$name\"?" $orig]}  {
                puts "Bad local link: $from --> $url"
            }
            continue
        }


        if {$hashIdx>0} {
            incr hashIdx -1
            set url [string range $url 0 $hashIdx]
        }
        set toFile [file join $dir $url]
        if {![file exists $toFile]} {
            puts "Bad link: $from --> $url"
        } else {
##            puts "ok:$toFile"
        }

    }
}


set ::translations [list]
proc gen::defineTranslation {type name} {
    lappend ::translations $type $name
}


proc gen::getTranslateLinks {from} {
    if {[gen::getDoTranslateLinks]} {
        if {[gen::getUrlRoot] == ""} {
            puts "No Url root defined. Create a lib.tcl file in your content directory with:\ngen::setUrlRoot <The Url directory where your content is on the web>"
            gen::setDoTranslateLinks 0
        } else {
            set myUrl "[gen::getUrlRoot]/$from"
            set myUrl [::http::formatQuery trurl $myUrl]
            set translation ""
            if {[llength $::translations] == 0} {
                set ::translations [list  fr Français es Español pt Portugese]
            }
            foreach {type  name} $::translations {
                append translation " <a target=\"_TRANSLATION\" href=\"http://babelfish.altavista.com/babelfish/trurl_pagecontent?lp=en_$type&$myUrl\">$name</a>"
            }
            return "$translation<p>"
        }
    }
    return ""
}

proc gen::processFile {from to fileIdx template} {


    gen::mkdir [file dirname $to]



    array set A [gen::initMacroArray]
    set depth         [gen::getDepth $from]

    set body [gen::getBody       $from]
    set body [gen::addGlossary   $from $body ]
    set body [gen::processSlideShow $from $to $body]
    set body [gen::processIfs $from $body]
    set body [gen::processFaq    $body]
    set body [gen::processSee    $from $body $depth]
    set body [gen::processNote   $body]
    set A(body)       $body

    array set A [gen::getCommonContent $from]
    array set A [gen::getCommonNav     $from]
    set A(navbreadcrumb)  [gen::nav::getBreadCrumb $from]
    set A(navprev)        [gen::nav::getPrev $from $fileIdx]
    set A(navnext)        [gen::nav::getNext $from $fileIdx]

    set childlist "[ht::tag div class childlist][ht::tag table width 100%]"


    foreach child [gen::getChildren $from] {
#        if {![gen::getIncludeInToc $child]} {continue}
	set desc [gen::getDescription $child]
	if {$desc !=""} {
	    set  desc "<br>$desc"
	}
        append childlist [ht::tag tr  valign top] [ht::tag td width 10% align right] 
        append childlist [ht::tag span class childlist-level body "[gen::getLevelLabel $child]&nbsp;"]
	append childlist "</td><td>" [ht::tag a  class childlist-link href [gen::getLink $from $child] body [gen::getTitle $child]] $desc
        if {[gen::getDoChildOverview]} {
            append childlist "<br>[gen::getOverview $child]"
        }
	append childlist "</td></tr>"
    }

    append childlist "</table></div>"
    set A(childlist) $childlist

##    puts "type: [gen::getPageType $from]"

    if {[gen::getPageType $from] == "link"} {
        return
    }


    if {[string first {<%childlist%>} $A(body)] >=0} {
        set A(body) [gen::replace $A(body) {<%childlist%>} $A(childlist)]
        set A(childlist) ""
	set childlist ""
    }

    if {[string first {<%nochildlist%>} $A(body)] >=0} {
        regsub -all {<%nochildlist%>} $A(body) {} A(body)
        set A(childlist) ""
    }

    if {[gen::getDoLinkCheck]} {
        gen::doLinkCheck $from $A(body)
    }




    set extraNav [gen::getTranslateLinks $from]
    set A(extranav) $extraNav

    if {[gen::getDoTemplate $from]} {
        set template [gen::getTemplateForPage $from $template]
        set html [gen::processTemplate $template A]
    } else {
        set html $body
    }
    set html [gen::js::process  $html $from $depth $fileIdx]
    set html [gen::processPopups $from $to $html $depth 0]
    set html [gen::processDetails $from $to $html $depth 0]
    set html [gen::processExtra  $from  $html]



    gen::writeFile $to $html


    set allHtml $A(body)
    if {[gen::getPageType $from] == "virtual"} {
        append allHtml $childlist
    }
    set allHtml [gen::processPopups $from $to $allHtml $depth 1]
    set allHtml [gen::processDetails $from $to $allHtml $depth 1]
    gen::writeToAll $from  $allHtml
}





##This returns the template to be used for the given page
##Override this to specify our own templates
proc gen::getTemplateForPage {page template} {
    set template
}

##Define a handler function for files that match the given 
##pattern. The handler is called with the filename and the 
##file contents and returns the (possibly) modified contents
proc gen::addHandler {pattern func}  {
    lappend  ::handlers $pattern $func
}



proc gen::getParentList {from} {
    set parent [gen::getParent $from]
    if {$parent == ""} {
        return [list]
    }
    concat  [gen::getParentList $parent] [list $parent]
}


proc gen::nav::getBreadCrumb {from} {
    set trail ""
    set didone 0
    foreach parent [gen::getParentList $from] {
        if {$didone} {append trail " > "}
        set didone 1
        append trail "<a href=\"[gen::getLink $from $parent]\">[gen::getTitle  $parent]</a> "
    }
    set trail
}



proc gen::createGeneralFile {to title body} {
    array set A [gen::initMacroArray]
    array set A [gen::getCommonContent]
    array set A [gen::getCommonNav    ]

##    set extraNav [gen::getTranslateLinks $to]
    set extraNav ""
    set A(extranav) $extraNav

    set A(title) $title
    set A(body) $body
    set html [gen::processTemplate $::generalTemplate A]
    set html [gen::processExtra  $to  $html]
    gen::writeFile $to $html
}




proc gen::nav::getPrev {from fileIdx} {
    if {$fileIdx<0} {return ""}
    if {$fileIdx ==0} {
        return [gen::getIconImg $from PreviousArrowDisabled.gif ""]
    } 
    set prevFile [lindex [gen::getAllNavFiles] [expr $fileIdx-1]]
    set title [gen::getTitle $prevFile]
    set img [gen::getIconImg $from PreviousArrow.gif "Previous: $title"]
    return  "<a href=\"[gen::getLink $from $prevFile]\">$img</a>"
}


proc gen::nav::getNext {from fileIdx} {
    if {$fileIdx<0} {return ""}
    set files [gen::getAllNavFiles]

    if {$fileIdx ==[expr [llength $files]-1]} {
        return  [gen::getIconImg $from NextArrowDisabled.gif ""]
    } 
    set nextFile [lindex $files [expr $fileIdx+1]]
    set title [gen::getTitle $nextFile]
    set img [gen::getIconImg $from NextArrow.gif "Next: $title"]
    return  "<a href=\"[gen::getLink $from $nextFile]\">$img</a> "
}


proc gen::nav::getTop {from} {
    set top [gen::getLink $from "images"]
    set img [gen::getIconImg $from TopArrow.gif "Main page"]
    return  "<a href=\"[gen::getLink $from  [gen::getTopFile]]\">$img</a>"
}


proc gen::nav::getToc {from} {
    set top [gen::getLink $from "images"]
    set img [gen::getIconImg $from TOCIcon.gif "Table of contents"]
    return "<a href=\"[gen::getLink $from toc.html]\#$from\">$img</a>"
}


proc gen::nav::getIndex {from} {
    set top [gen::getLink $from "images"]
    if {[gen::getDoIndex]} {
        set img [gen::getIconImg $from Index.gif "Index"]
        return  "<a href=\"[gen::getLink $from mainindex.html]\">$img</a> "
    }
    return ""
}


proc gen::nav::getGlossary {from} {
    set top [gen::getLink $from "images"]
    if {[gen::getDoGlossary]} {
        set img [gen::getIconImg $from Glossary.gif "Glossary"]
        return "<a href=\"[gen::getLink $from glossary.html]\">$img</a> "
    }
    return ""
}

proc gen::nav::getImages {from} {
    set top [gen::getLink $from "images"]
    if {[gen::getDoImages]} {
        set img [gen::getIconImg $from Images.gif "Images"]
        return "<a href=\"[gen::getLink $from imageindex.html]\">$img</a> "
    }
    return ""
}


proc gen::nav::getFrame {from} {
    set top [gen::getLink $from "images"]
    if {[gen::getDoFrames]} {
        set img [gen::getIconImg $from Frames.gif "Frames"]
        return "<a href=\"[gen::getLink $from frames.html]\" target=\"_top\">$img</a> "
    }
    return ""
}

proc gen::nav::getSchedule {from} {
    if {[gen::haveSchedule]} {
        set top [gen::getLink $from "images"]
        set img [gen::getIconImg $from Schedule.gif "Schedule"]
        return "<a href=\"[gen::getLink $from schedule.html]\">$img</a> "
    }
    return ""
}


proc glossary {word definition} {
    set ::glossary([string tolower $word]) [list $word $definition]
}


proc gen::alias {alias realName} {
    set ::aliases($alias) $realName
    return ""
}


proc gen::processAliases {body} {
    global aliases
    foreach alias [array names aliases] {
	regsub -all "%$alias%" $body $alias body
    }
    return $body
}





proc gen::processExtra {from content} {
    return $content;
}

proc gen::findAttr {str attr dflt} {
    regexp "$attr\\s*?=\\s*?\"(\[^\"\]*)\"" $str match dflt
    return $dflt
}

proc ht::div {body {class {}} } {
    return "<div class=\"$class\">$body</div>"
}


proc ht::attrs {args} {
    set attrs ""
    foreach {name value} $args {
        append attrs " $name=\"$value\" "
    }
    set attrs
}

proc gen::processFaq {content} {
    set faqCnt 0
    set processed ""
    while {1} {
        set idx1 [string first {<faq} $content]
        if {$idx1<0} {break}
        set idx2 [string first {</faq>} $content $idx1]
        if {$idx2<0} {break}

        append processed [string range $content 0 [expr {$idx1-1}]]
        set faq [string range $content $idx1 $idx2]
        set content   [string range $content [expr {$idx2+6}] end]
        append processed [gen::processFaqInner $faq [incr faqCnt]]
    }
    append processed $content
}



proc gen::processFaqInner {faq faqCnt} {
    set qlabel "Q."
    regexp {<faq[^>]*qlabel=\"([^\"]+)\"} $faq match qlabel
    set alabel "A."
    regexp {<faq[^>]*alabel=\"([^\"]+)\"} $faq match alabel
    set dfltCat ""
    regexp {<faq[^>]*cat=\"([^\"]+)\"} $faq match dfltCat

    set cnt 0
    set cats [list]
    set currentCat "$dfltCat"
    while {[regexp {<faqitem([^>]+)>(.*?)</faqitem>(.*)$} $faq match attrs answer  faq]} {
        set cat "$currentCat"
##        puts "ATTR:$attrs"
        regexp {cat\s*=\s*\"([^\"]+)\"} $attrs match cat
        set question ""
        set faqid ""
        regexp {id\s*=\s*\"([^\"]+)\"} $attrs match faqid
        regexp {q\s*=\s*\"([^\"]+)\"} $attrs match question
        set name ""
        regexp {name\s*=\s*\"([^\"]+)\"} $attrs match name
        if {$cat!=""} {
            set currentCat $cat
        }
        if {![info exists faqitems($currentCat)]} {
            set faqitems($currentCat) [list]
            lappend cats $currentCat
        }
        if {$question!=""} {
##            puts "\t($currentCat) $question"
            lappend faqitems($currentCat) $question $answer $name $faqid
        }
    }
    
    set prefix "faq${faqCnt}_"
    if {![llength $cats]} {return ""}
    set faqTop ""
    set faqBottom "<p><hr><p>"
    set catCnt 0
    foreach cat $cats {
        incr catCnt
        set catTop  ""
        set catBottom  ""
        set catPrefix "${prefix}cat${catCnt}_"
        set didOne 0
        foreach {q a name faqid} $faqitems($cat) {
            set didOne 1
            if {$faqid==""} {
                set faqid "$catPrefix$cnt"
            } 
            append catTop "<div class=\"faq-question\"> <li> <a class=\"faq-question-link\" href=\"\#$faqid\">$q</a></div>\n"
            if {$name!=""} {
                append catBottom "<a name=\"$name\"></a>\n"
            }
            append catBottom "<a name=\"$faqid\"></a><div class=\"faq-question\"><h4>$qlabel $q</h4></div>\n"
            append catBottom "</a><div class=\"faq-answer\"><b>$alabel</b> $a</div>\n"
            append catBottom "<p><hr align=\"center\" width=\"10%\"><p>"
            incr cnt
        }
        if {$cat !=""} {
            append faqTop [ht::div "<a class=\"faq-category-link\" href=\"#$catPrefix\">$cat</a>" faq-category-title]
            append faqBottom [ht::div "<a name=\"$catPrefix\"></a>$cat" faq-category-title]
        } 
        if {$didOne} {
            append faqTop [ht::div $catTop faq-category]
            append faqBottom [ht::div $catBottom faq-category]
        }
    }

    set faqTop [ht::div $faqTop faq-top]
    set faqBottom [ht::div $faqBottom faq-bottom]
    ht::div "$faqTop\n$faqBottom" faq

}




proc ht::tag {tag args} {
    set attrs ""
    set body ""
    foreach {name value} $args {
	if {$name=="body"} {set body $value;continue}
	append attrs " $name=\"$value\" "
    }
    set html "<$tag $attrs>"
    if {$body!=""} {
	append html "\n$body\n</$tag>"
    }
    set html
}


proc gen::processPopups {from dest content depth {forAll 0}} {
    set orig $content
    set didone 0
    set cnt 0
    set thisFile [file tail $dest]
    set root [file tail [file root $dest]]
    set id [file root $from]
    regsub -all / $id _ id
    set id popup_${id}
    set img [ht::tag img src [gen::getDotPath $depth]images/Help16.gif border 0 alt "Show popup" title "Show popup"]
    set popupHtml ""
    while {[regexp {<popup([^>]*?)>(.*?)</popup>} $content match attrs popup]} {
#"
        incr cnt
        set destdir [file dirname $dest]
	if {!$forAll} {
	    set html [html::page "$root popup" [ht::tag div class popupbody body $popup] $depth "class=\"popup\""]
            set html [gen::replace $html {%dotpath%} [gen::getDotPath $depth] ]
	    gen::writeFile [file join $destdir ${id}_$cnt.html] $html
	} else {
	    gen::addPopupText $from "" "<a name=\"${id}_$cnt\"></a>$popup"
 	}
        set label [gen::findAttr $attrs label {}]
	if {!$forAll} {
	    set width [gen::findAttr $attrs width 440]
	    set height [gen::findAttr $attrs height 550]
	    set wargs "'comments','width=$width,height=$height,resizable=yes,scrollbars=yes,status=yes'"
	    regsub  {<popup[^>]*?>.*?</popup>} $content "<a href=\"${id}_$cnt.html\" onClick=\"window.open('${id}_$cnt.html', $wargs);return false;\">$label $img</a>" content
	} else {
	    regsub  {<popup[^>]*?>.*?</popup>} $content "<a href=\"\#${id}_$cnt\">$label</a>" content
	}
    }
    return $content
}



proc  gen::processDetails {from dest content depth {forAll 0}} {
    set orig $content
    set didone 0
    set cnt 0
    set thisFile [file tail $dest]
    set root [file tail [file root $dest]]
    set detailsHtml ""

    set links [list]

    while {[regexp {<details([^>]*?)>(.*?)</details>(.*)$} $content match attrs details content]} {
#"
        set link "Details"
        set title  "Details"
        set gotLink [regexp {link\s*?=\"(.*?)\"} $attrs match link]
        set gotTitle [regexp {title\s*?=\"(.*?)\"} $attrs match title]
        if {!$gotTitle} {
            set title $link
        }

        lappend links $link
        incr cnt
        append detailsHtml  "<a name=\"anchor$cnt\"></a><div class=\"pagesubtitle\">$title</div>$details"
    }


    if {$cnt} {
        set destdir [file dirname $dest]
	if {!$forAll} {
	    set detailsHtml [html::page "$root details" "<div class=\"pagetitle\">[gen::getTitle $from] Details</div>$detailsHtml"  $depth]
	    set detailsFile ${root}_details.html
	    gen::writeFile [file join $destdir $detailsFile] $detailsHtml
	} else {
	    gen::addPopupText $from "Details"  $detailsHtml
	}

        set content $orig
        set cnt 1
        set wargs "'comments','width=440,height=550,resizable=yes,scrollbars=yes,status=yes'"
        
	if {!$forAll} {
	    foreach link $links {
		if {![regsub  {<details([^>]*?)>.*?</details>} $content "<a href=\"#\" onClick=\"javascript:window.open('$detailsFile\#anchor$cnt', $wargs);return false;\" >$link</a>" content]} {break;}
		incr cnt
	    }
	} else {
	    foreach link $links {
		if {![regsub  {<details([^>]*?)>.*?</details>} $content "<a href=\"#anchor$cnt\" >$link</a>" content]} {break;}
		incr cnt
	    }

	}
        set orig $content
    }
    return $orig
}



##
##The start of a quiz creator
##
proc gen::processQuiz {from content depth} {
    set orig $content
    set quizzes [list]
    while {[regexp {<quiz>(.*?)</quiz>(.*)$} $content match quiz content]} {
        lappend quizzes $quiz
    }
    set cnt 0
    foreach quiz $quizzes {
        incr cnt
        set javascript ""
        set form ""
        set type multi
        while {[regexp {<question\s*?name\s*?=\s*?"(.*?)">(.*?)</question>(.*$)} $quiz match name answers quiz]} {
        }

        set quizBody "
<script language=\"javascript1.2\">
function quiz$cnt () {
    var w = window.open('', '','width=440,height=500,resizable=yes,scrollbars=yes,status=yes')
    var html = '';
    $javascript\
    w.document.write (html);
}
</script>

<form action=\"javascript:quiz$cnt();\">
$form
<input type=\"submit\" value =\"Submit\">
</form>"
        regsub {<quiz>(.*?)</quiz>} $orig $quizBody orig
    }



    return $orig
}


##
##The converts any <see href=...> tags into the appropriate 
##<a href=...>Title of target page</a>
##
proc gen::processSee {from content depth} {
    set orig $content
    set sees [list]
    while {[regexp {<see\s*?href\s*?=\s*?"(.*?)".*?>(.*)$} $content match href content]} {
#"
        set href [string trim $href]
        set extra ""
        if {![regsub "^/" $href {} href]} {
            ##relative link
            set dirname [file dirname $from]
            while {[regsub "^\.\./" $href {} href]} {
                set dirname [file dirname $dirname]
            } 
            regsub {^\./} $dirname {} dirname
            regsub {^\./} $href {} href
            set href "$dirname/$href"
            regsub {^\./} $href {} href
        }
        
        regexp {(.*\.html)(\#.*)} $href match href extra
        lappend sees  [list $href $extra]
    }

    foreach see $sees {
        foreach {href extra} $see break
        set link [gen::getLink $from $href]
        if {![gen::fileExists $href]} {
            puts "Error: Unknown see link: $href"
        } else {
            regsub {<see\s*?href\s*?=\s*?"(.*?)".*?>} $orig [html::href $link$extra  [gen::getTitle $href]] orig
        }
    }
    return $orig
}




##
##Either excises any <note>...</note> tags for the final version
##or includes them in development versions
##
proc gen::processNote {content} {
    set orig $content
    set sees [list]
    if {[gen::getDoFinalVersion]} {
        regsub -all {<note>.*?</note>} $orig {} orig
    } else {
        regsub -all {<note>(.*?)</note>} $orig {<div class="note">\1</div>} orig
    }
    return $orig
}




proc gen::processIdv {dest content depth} {
    set img "<img src=\"[gen::getDotPath $depth]images/Play16.gif\" border=\"0\" alt=\"Execute command\" title=\"Execute command\">"
    regsub -all  {<idv\s*?command\s*?=\s*?"(.*?)"\s*?>(.*?)</idv>} $content "<code class=\"menu\">\\2</code> <a href=\"jython:\\1\"  onClick=\"\">$img</a>" content
    return $content
}



proc gen::isHtml {comp} {
    regexp -nocase {(\.html$|\.htm$|\.tml$)} $comp
}


proc gen::up1 {value} {
    return [string toupper [string range  $value 0 0]][string tolower [string range  $value 1 end]]
}

proc gen::msg {m} {
    if {[gen::getVerbose]} {
        puts $m
    }
}

##
##Writes  out the mainindex file
##
proc gen::writeMainIndex {} {
    global indexInfo
    set html ""
    set currentLetter ""
    set letters ""
    foreach value [lsort -dictionary  [array names indexInfo]] {
        set firstLetter [string range $value 0 0]
        if {$firstLetter != $currentLetter} {
            if {$html != ""} {
                append html "</ul>"
            }
            set currentLetter $firstLetter
            append letters "&nbsp;<a href=[html::quote \#letter_$currentLetter]>$currentLetter</a>&nbsp;"
            append html "<a name=[html::quote letter_$currentLetter]></a><div class=\"pagesubtitle\">$firstLetter</div><ul>"
        }
        append html "<i>$value</i><ul>"
        foreach {title url} $indexInfo($value) {
            append html "<li><a href=\"$url\">$title</a>"
        }
        append html "</ul>"
    }

    append html "</ul>"
    set html "<center><div class=\"pagesubtitle\">$letters</div></center>$html"
    gen::createGeneralFile [file join [gen::getTargetDir] mainindex.html] "Index" $html
}

proc gen::writeGlossary {} {
    global glossary
    set html "<dl>"
    set currentLetter ""
    set cnt  0
    set letters ""
    foreach n [lsort -dictionary [array names glossary]] {
        set firstLetter [string toupper [string range $n 0 0]]
        if {$firstLetter != $currentLetter} {
            set currentLetter $firstLetter
            if {$cnt > 0} {
                append html "</ul>"
                append letters " &nbsp; | &nbsp; "
            }  
            append letters "<a href=\"\#letter_$currentLetter\">$currentLetter</a>"
            incr cnt
            append html "<a name=\"letter_$currentLetter\"></a><ul>"
        }
        foreach {Word definition} $glossary($n) break;
        append html "<p><dt><a name=\"$n\"></a><b>$Word</b></dt><dd> $definition</dd>\n"
    }

    append html "</ul></dl>"

    set html "<center>$letters</center>\n$html"
    gen::createGeneralFile [file join [gen::getTargetDir] glossary.html] "Glossary" $html
}



proc gen::replace {s from to} {
    set len [string length $from]
    while {1} {
	set idx [string first  $from $s]
	if {$idx<0} {break}
	set s "[string range $s 0 [expr {$idx-1}]]$to[string range $s [incr idx $len] end]"
    }
    set s
}



proc gen::processTemplate {template arrayName} {
    upvar $arrayName A
#    set template [gen::replace $template & \\&]
    ##DO the body first
    if {[info exists A(body)]} {
        set template [gen::replace $template %body% $A(body)]
    }

    foreach name [array names A] {
	set macro [string tolower $name]
        if {$macro=="body"} continue;
#        regsub -all $template
        set template [gen::replace $template %$macro% $A($name)]
    }
    return $template
}


proc gen::findTemplate {name {dflt Template.html}} {

##First check if we have one in the content dir
    if {[file exists $name]} {
	return [gen::getFile $name]
    }


##check if we have one in the content/templates dir
    if {[file exists [file join templates $name]]} {
	return [gen::getFile [file join templates $name]]
    }


##Next, check if there is a dflt
    if {[file exists $dflt]} {
	return [gen::getFile $dflt]
    }


##Next, check if there is a dflt
    if {[file exists [file join templates $dflt]]} {
	return [gen::getFile [file join templates $dflt]]
    }
    
    set scriptDir [file dirname [info script]]

##Now, check in the script dir
    if {[file exists [file join $scriptDir $name] ]} {
	return [gen::getFile [file join $scriptDir $name]]
    }

##Now, check in the script dir for the dflt
    if {[file exists [file join $scriptDir $dflt] ]} {
	return [gen::getFile [file join $scriptDir $dflt]]
    }    

    return "No template found: $name"
    
}


proc gen::procExists {p} {
    llength [info procs $p]
}

proc gen::defineMacro {name value} {
    set ::macros [concat [list $name $value] $::macros]
#    lappend ::macros $name $value
}


proc gen::parseArgs {} {
    global argv

#####    set argv [list -finalversion -nofinalversion -noverbose -verbose -clean -noclean -iconwidth width -glossary -noglossary -frames -noframes -icons -noicons   -nonumbertop -numbertop -nonumbering -numbering -notclevaluation -tclevaluation  -nolinkcheck -linkcheck -nodoall -doall  -nojsnav -jsnav       -nojsborder -jsborder -nojsbg -jsbg -noformat -format]


    set argc [llength $argv]
    set flags [list Glossary Frames Icons DoAll DoTranslateLinks LinkCheck NumberTop Numbering Icons Verbose Format JustExtraFormat Clean JSNav JSBorder JSBG SkipIndex ChildOverview TclEvaluation FinalVersion Thumbnails]


    for {set i 0} {$i < $argc} {incr i} {
        set arg [lindex $argv $i]
        set didone 0
        foreach method $flags {
            set flag [string tolower $method]
            set proc $method
            if {![gen::procExists set$proc]} {
                set proc Do$method
                if {![gen::procExists set$proc]} {
                    puts "Unknown proc: $method"
                    exit
                }
            }
#            puts "Ok: $proc"
            if {$arg=="-$flag"} {
                set didone 1
                gen::set$proc 1
            } elseif {$arg=="-no$flag"} {
                set didone 1
                gen::set$proc  0
            }
            if {$didone} {break;}
        }
        ##exit
        if {$didone} {continue;}


        switch -glob -- $arg  { 
	    -allfilename {
                incr i
		gen::setAllFileName  [lindex $argv $i]
	    }
            -target {
                incr i
                gen::setTargetDir   [lindex $argv $i]
            }
            -index {
                incr i
                gen::setIndexFile   [lindex $argv $i]
            }
            -iconwidth {
                incr i
                gen::setIconWidth [lindex $argv $i]
            }
            -lib {
                incr i
                lappend ::libs [lindex $argv $i]
            }
            -css {
                incr i
                gen::addCssFile [lindex $argv $i]
            }
            default {
                if {[string first -D $arg] == 0} {
                    gen::define [string trim [string range $arg 2 end]]
                    continue;
                }
                if {[regexp {\.html} $arg]} {
                    lappend ::htmlFiles $arg
                } else {
		    set i [gen::hook::parseArgs $argv $arg $i]
                }
                    
            }
        }
    }
}

proc gen::hook::parseArgs {argv arg i} {
    puts "Unknown argument: $arg"
    gen::usage
}


proc gen::usage {} {
    puts {usage tclsh generate.tcl 
        [-target <target directory>] 
        [-lib <extra tcl library>]
        [-finalversion (Should <note tags be excised, etc.)]
        [-nofinalversion (Should <note tags NOT be excised, etc.)]
        [-noverbose/-verbose <Don't be verbose>]
        [-clean] [-noclean] 
        [-iconwidth width <Override the default icon width>]
        [-glossary] [-noglossary]
        [-frames] [-noframes]
        [-icons] [-noicons]
        [-nonumbertop/-numbertop <Don't/Do start secion numbering at the top most level>]
        [-nonumbering/-numbering     <Don't/Do do section numbering>]
        [-notclevaluation/-tclevaluation   <Don't/Do evaluate the html for embedded tcl>]
        [-nolinkcheck/-linkcheck <Don't/Do checking of links>]
	[-allfilename <Name of the all.html file>]
        [-nodoall/-doall <Don't/Do generate all.html files>]
        [-nojsnav -jsnav <Don't/Do insert the javascript navigation>]
        [-nojsborder/-jsborder  <Don't/Do  borders>]
        [-nojsbg/-jsbg  <Don't/Do  background>]
        [-noformat/-format]  <Don't do format  the original html source>
    }
    exit

}

proc gen::haveSchedule  {} {
    return [llength [array names ::schedule]]
}


proc gen::addCssFile {f} {
    set tail [file tail $f]
    if {[info exists ::cssSeen($tail)]} {return}
    set ::cssSeen($tail) 1
    lappend ::cssFiles $f
}

proc gen::doFormat {files} {
    foreach f $files {
        puts "doFormat $f"
        gen::formatHtml ${f} [gen::getFile $f]  [gen::getJustExtraFormat]
    }
}



proc gen::writeFiles  {} {
    global  state files  alldirs top 
    set top [list]
    set ::filesToCopy [list]
    set target [gen::getTargetDir]
    


    gen::walkTree [file join $state(topDir)  [gen::getIndexFile]]

    if {[file exists schedule.tcl]} {
###        source schedule.tcl
    }

    set ::fileIdx 0



    set files [list]
    foreach file [gen::getAllNavFiles]  {
        lappend files $file
        gen::processFile $file [file join $target $file]   $::fileIdx      $::lessonTemplate
        incr ::fileIdx
    }


    foreach file [gen::getAllNonNavFiles]  {
        lappend files $file
        gen::processFile $file [file join $target $file]  -1 $::lessonTemplate
    }

    foreach  {from to} $::filesToCopy {
        gen::msg "Copying file: $from->$to"
        file copy -force $from $to
    }



    if {[gen::getDoAll]} {
        catch {
            puts $::allfp "\n</body></html>"
            close $::allfp

            puts $::allfpnoimg "\n</body></html>"
            close $::allfpnoimg
        }
    }


    ##Now copy the images
    set targetDir [gen::getTargetDir]
    
    foreach dir [array names alldirs] {
        gen::copyFiles $dir
    }



    if {[gen::haveSchedule]} {
        foreach  day  [lsort [array names ::schedule]] {
            append html "<div class=\"pagesubtitle\">Day $day</div><ul><table>\n"
            foreach item  $::schedule($day) {
                foreach {time title paths} $item break
                if {[llength $paths]} {
                    set prefix ""
                    if {$title != ""} {
                        set prefix "&nbsp;&nbsp;"
                    }
                    set links ""
                    foreach path $paths {
                        append links "$prefix<a href=\"$path\">[gen::getTitle $path]</a><br>\n"
                    }
                    if {$title != ""} {
                        append html "<tr valign=\"top\"><td> $time</td><td>$title<br>$links</td></tr>\n" 
                    } else {
                        append html "<tr valign=\"top\"><td> $time</td><td>$links</td></tr>\n" 
                    }
                } else {
                    append html "<tr valign=\"top\"><td> $time</td><td> $title</td></tr>\n" 
                }
            }
            append html "</table></ul>\n"
        }
        gen::createGeneralFile [file join [gen::getTargetDir] schedule.html] "Schedule" $html
    }


    set imageHtml ""
    foreach file $files {
       if {![gen::existsImageInfo $file]} {continue}
        set cnt 0
        foreach {id img caption} [gen::getImageInfo $file] {
            if {$cnt == 0} {
                append imageHtml "<p><b>[gen::getLevelLabel $file]</b> <a href=\"$file\"> [gen::getTitle $file]</a><ul>\n"
            }
            incr cnt
            if {[gen::getDoThumbnails]} {
                set thumb "<img src=\"[gen::thumbName $img]\" border=\"0\"><br>"
            } else {
                set thumb ""
                append imageHtml "<li> "
            }
            append imageHtml "<a href=\"$file\#image$id\"> $thumb Image $id:</a> $caption\n"
            if {[gen::getDoThumbnails]} {
                append imageHtml "<p>\n"
            }
        }
        if {$cnt} {
            append imageHtml "</ul>\n"
        }
    }

    gen::createGeneralFile [file join [gen::getTargetDir] imageindex.html] "Images" $imageHtml



    foreach {toc fulltoc  frametoc} [gen::getToc [gen::getTopFile]] break
    gen::createGeneralFile [file join [gen::getTargetDir] toc.html] "Table of Contents" $toc
    gen::createGeneralFile [file join [gen::getTargetDir] fulltoc.html] "Full Table of Contents" $fulltoc

    if {[gen::getDoFrames]} {
        set html "<frameset cols=\"200,*\" > <frame name=\"left\" src=\"frameleft.html\"><frame name=\"right\" src=\"[gen::getTopFile]\"></frameset>"
        gen::writeFile  [file join [gen::getTargetDir] frames.html] $html 

        set html "<a href=\"[gen::getTopFile]\" target=\"_TOP\"><img src=\"images/TopArrow.gif\" border=\"0\"></a><p>"
        append html [html::page {Frame navigation} "<div class=\"framenav\"><table>$frametoc</table></div>"]
        gen::writeFile  [file join [gen::getTargetDir] frameleft.html]  $html
    }

    if {[gen::getDoIndex]} {
        gen::writeMainIndex
    }

    if {[gen::getDoGlossary]} {
        gen::writeGlossary
    }

    gen::hook::end

}



proc gen::copyFiles {dir} {
    if {$dir != "" && ![file exists $dir]} {
        return
    }
    set targetDir [gen::getTargetDir]
    set images [list]
    foreach prefix {jnlp gif GIF jpg jpeg JPG JPEG png PNG svg SVG svgz SVGZ} {
        set images [concat $images [glob -nocomplain [file join $dir *.$prefix]]]
    }
    #        puts "images: $images"
    foreach f $images {
        set imgDir [file join $targetDir [file dirname $f]]
        if {![file exists $imgDir]} {
            file mkdir $imgDir
        }
        file copy -force $f $imgDir
        if {[gen::getDoThumbnails]} {
            gen::thumb [file join $imgDir [file tail $f]]
        }
    }
    if {[regexp {/images$} $dir]} {
        foreach subdir [glob -nocomplain [file join $dir *]] {
            if {[file isdir $subdir]} {
                gen::copyFiles $subdir
            }
        }
    }

}

proc gen::hook::end {} {
}




proc gen::setPageTemplateName {templateName} {
    set ::lessonTemplate [gen::findTemplate  $templateName]
}


##Read in the different header and footer templates
gen::setPageTemplateName Template.tml
set generalTemplate [gen::findTemplate GeneralTemplate.html Template.html]



global     state
set state(topDir) ""




foreach {var dflt} [list  UrlRoot {} DoClean 0 Verbose 0 DoChildOverview 1 DoFinalVersion 1 AllFileName all.html TargetDir [file join $state(topDir) ../processed]  DoGlossary 1 DoImages 1 DoFrames 1 DoIcons 1 DoIndex 1  NumberTop 0 Numbering 1 DoTclEvaluation 0 DoTranslateLinks 0 DoAll 0 DoLinkCheck 0 CssFiles [list] AllNavFiles [list] AllNonNavFiles [list]    DoJSNav 0 DoJSBorder 1 DoJSBG 1 DoStrictIndex 1 IconWidth "" Format 0 JustExtraFormat 0 SkipIndex 0 IndexFile main.index TopFile {} UniqueId 1 DoThumbnails 0 DoImageLinks 1]   {
    set state($var) $dflt
    proc gen::get$var {} "set ::state($var)"
    proc gen::set$var {v} "set ::state($var) \$v"
}




if {[file exists lib.tcl]} {
    source lib.tcl
}



gen::parseArgs




foreach lib $libs {
    source $lib
}

set scriptDir [file dirname [info script]]
if {[file exists [file join $scriptDir lib.tcl]]} {
    source [file join $scriptDir lib.tcl]
}



if {[gen::getFormat]} {
    gen::doFormat $htmlFiles
    exit
}


if {[gen::getDoClean] && [file exists [gen::getTargetDir]]} {
    gen::msg "Removing old generated directory:  [gen::getTargetDir]"
    file  delete -force [gen::getTargetDir]
}


if {[llength $::htmlFiles]} {
    set target [gen::getTargetDir]
    foreach file $::htmlFiles {
        gen::definePage $file $file {} 1 1  raw
        gen::processFile $file [file join $target $file]  -1 {%body%}
    }
    exit
}



gen::msg "Writing content to: [gen::getTargetDir]"

foreach cssFile  [glob -nocomplain  *.css] {
    gen::addCssFile $cssFile
}
gen::setCssFiles $::cssFiles


if {![file exists [gen::getTargetDir]]} {
    file mkdir [gen::getTargetDir]
}


##Copy over any top level css files
foreach cssFile  $cssFiles {
    catch {file copy -force $cssFile [gen::getTargetDir]}
}

catch {file copy  [file join $scriptDir/default.css]  [gen::getTargetDir]}
catch {file copy  [file join $scriptDir/unidata.js]  [gen::getTargetDir]}

if {[llength [gen::getCssFiles]] == 0} {
    puts "Adding default.css"
    gen::setCssFiles [list default.css]
}


##Copy the default images
if {![info exists images/TOCIcon.gif]} {
    set scriptDir [file dirname [info script]]
    set newImgDir [file join [gen::getTargetDir] images]
    file mkdir $newImgDir
    foreach img [glob [file join $scriptDir/images/*.gif]] {
        if {![file exists [file join $newImgDir [file tail $img]]]} {
            file copy -force $img $newImgDir
        }
    }
}



##Source the glossary.tcl file if it exists
if {[gen::getDoGlossary]} {
    set glossaryFile [file join $state(topDir) glossary.tcl]
    if {[file exists $glossaryFile]} {
        source $glossaryFile
    }
}




gen::writeFiles 

