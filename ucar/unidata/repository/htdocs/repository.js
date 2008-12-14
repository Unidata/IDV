



function Util () {

    this.loadXML = function (url, callback,arg) {
        var req = false;
        if(window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch(e) {
                req = false;
            }
        } else if(window.ActiveXObject)  {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch(e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch(e) {
                    req = false;
                }
            }
        }
        if(req) {
            req.onreadystatechange = function () { 
                if (req.readyState == 4 && req.status == 200)   {
                    callback(req,arg); 
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    }



    this.setCursor = function(c) {
        var cursor = document.cursor;
        if(!cursor && document.getElementById) {
            cursor =  document.getElementById('cursor');
        }
        if(!cursor) {
            document.body.style.cursor = c;
        }
    }


    this.getDomObject = function(name) {
        obj = new DomObject(name);
        if(obj.obj) return obj;
        return null;
    }



    this.getKeyChar = function(event) {
        event = util.getEvent(event);
        if(event.keyCode) {
            return String.fromCharCode(event.keyCode);
        }
        if(event.which)  {
            return String.fromCharCode(event.which);
        }
        return '';
    }


    this.print = function (s, clear) {
        var obj = util.getDomObject("output");
        if(!obj) return;
        if(clear) {
            obj.obj.innerHTML  ="";
        }
        obj.obj.innerHTML  =obj.obj.innerHTML+"<br>" +s;
    }



    this.getEvent = function (event) {
        if(event) return event;
        return window.event;
    }


    this.getEventX =    function (event) {
        if (event.pageX) {
            return  event.pageX;
        }
        return  event.clientX + document.body.scrollLeft
        + document.documentElement.scrollLeft;
    }

    this.getEventY =function (event) {
        if (event.pageY) {
            return  event.pageY;
        }
        return  event.clientY + document.body.scrollTop
        + document.documentElement.scrollTop;

    }

    this.getTop = function (obj) {
        if(!obj) return 0;
        return obj.offsetTop+this.getTop(obj.offsetParent);
    }



    this.setPosition = function(obj,x,y) {
        obj.style.top = y;
        obj.style.left = x;
    }

    this.getLeft =  function(obj) {
        if(!obj) return 0;
        return obj.offsetLeft+this.getLeft(obj.offsetParent);
    }
    this.getRight =  function(obj) {
        if(!obj) return 0;
        return obj.offsetRight+this.getRight(obj.offsetParent);
    }
}

util = new Util();



function DomObject(name) {
    this.obj = null;
// DOM level 1 browsers: IE 5+, NN 6+
    if (document.getElementById)	{    	
        this.obj = document.getElementById(name);
        if(this.obj) 
            this.style = this.obj.style;
    }
// IE 4
    else if (document.all)	{  			
        this.obj = document.all[name];
        if(this.obj) 
            this.style = this.obj.style;
    }
// NN 4
    else if (document.layers)  { 		
        this.obj = document.layers[name];
        this.style = document.layers[name];
    }
}



function noop() {
}







var menuObject;
document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup   = mouseUp;

var mouseIsDown = 0;
var draggedEntry;
var draggedEntryName;
var mouseMoveCnt =0;

function hideMenuObject() {
    if(menuObject) {
        hideObject(menuObject);
        menuObject = null;
    }
}


function mouseDown(event) {
    if(menuObject) {
        setTimeout("hideMenuObject()",1000);
    }
    event = util.getEvent(event);
    mouseIsDown = 1;
    mouseMoveCnt =0;
    return true;
}



function mouseUp(event) {
    event = util.getEvent(event);
    mouseIsDown = 0;
    draggedEntry   = null;
    util.setCursor('default');
    var obj = util.getDomObject('floatdiv');
    if(obj) {
        hideObject(obj);
    }
    return true;
}

function mouseMove(event) {
    event = util.getEvent(event);
    if(draggedEntry && mouseIsDown) {
        mouseMoveCnt++;
        var obj = util.getDomObject('floatdiv');
        if(mouseMoveCnt==6) {
            util.setCursor('move');
        }
        if(mouseMoveCnt>=6&& obj) {
            moveFloatDiv(util.getEventX(event),util.getEventY(event));
        }
    }    
    return false;
}


function moveFloatDiv(x,y) {
    var obj = util.getDomObject('floatdiv');
    if(obj) {
        if(obj.style.visibility!="visible") {
            obj.style.visibility = "visible";
            obj.style.display = "block";
            obj.obj.innerHTML = draggedEntryName;
        }
        obj.style.top = y;
        obj.style.left = x+10;
    }
}

function mouseOverOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    if(mouseIsDown)  {
        var obj = util.getDomObject("span_" + id);
        if(!obj)  return;
        //       if(obj.style && obj.style.borderBottom) {
        obj.style.borderBottom="2px black solid";
        //        }
    }
}

function mouseOutOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    var obj = util.getDomObject("span_" + id);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
}




function mouseDownOnEntry(event, id, name) {
    event = util.getEvent(event);
    draggedEntry = id;
    draggedEntryName=name;
    mouseIsDown = 1;
    if(event.preventDefault) {
        event.preventDefault();
    } else {
	event.returnValue = false;
        return false;
    }
}


function mouseUpOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    var obj = util.getDomObject("span_" + id);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
    if(draggedEntry && draggedEntry!=id) {
        url = "${urlroot}/entry/copy?action=action.move&from=" + draggedEntry +"&to=" + id;
        document.location = url;
    }
}





function setImage(id,url) {
    img = util.getDomObject(id);
    if(img) {
        img.obj.src  = url;
    }
}



function Tooltip () {
    var STATE_INIT = 0;
    var STATE_LINK = 1;
    var STATE_TIP = 2;
    var lastMove = 0;
    var state = STATE_INIT;
    var currentID;
    var hideDelay = 1000;
    var showDelay = 500;

    this.debug = function(msg) {
        //        util.print(msg);
    }
    this.keyPressed = function (event) {
        if(state==STATE_INIT) return;
        c =util.getKeyChar(event);
        if(c == '\r' && state == STATE_TIP) {
            tooltip.doHide();
        }
    }

    this.onMouseMove = function (event,id) {
        lastMove++;
        if(state!=STATE_INIT) return;
        event = util.getEvent(event);
        setTimeout("tooltip.showLink(" + lastMove+"," +util.getEventX(event)+","+ util.getEventY(event) +"," + "'" + id +"'"+")", showDelay);
    }

    this.onMouseOut = function (event,id) {
        lastMove++;
        if(state !=STATE_LINK) return;
        this.debug('on mouse out ' + id);
        setTimeout("tooltip.checkHide(" + lastMove+ ")", hideDelay);
    }


    this.onMouseOver = function(event,id) {
        event = util.getEvent(event);

        if(state ==STATE_LINK && currentID && id!=currentID) {
            this.doHide();
            currentID = null;

        }
        lastMove++;
        if(state!=STATE_INIT) return;
        setTimeout("tooltip.showLink(" + lastMove+"," +util.getEventX(event)+","+ util.getEventY(event) +"," + "'" + id +"'"+")", showDelay);
    }


    this.checkHide  = function(timestamp) {
	if(timestamp<lastMove) return;
        this.doHide();
    }

    this.doHide  = function() {
        currentID = "";
        if(state !=STATE_LINK && state!=STATE_TIP)
		return;
        state = STATE_INIT;
        hideObject(util.getDomObject("tooltipdiv"));
    }


    this.getX = function(link,eventX) {
        if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
            return eventX-15;
            return util.getLeft(link.obj);
        } else {
            return eventX+20;
        }
    }

    this.getY = function(link,eventY) {
        if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
            return  link.obj.offsetHeight+util.getTop(link.obj)-2;
        } else {
            return eventY;
        }
    }


    this.onClick  = function(event,id) {
	state = STATE_TIP;
        var link = util.getDomObject(id);
        x = this.getX(link);
        y = this.getY(link);
        var obj = util.getDomObject("tooltipdiv");
        if(!obj) return;
        //        util.setPosition(obj, x,y);
        url = "${urlroot}/entry/show?entryid=" + id +"&output=metadataxml";
	util.loadXML( url, handleTooltip,obj);
    }


    this.showLink = function(moveId,x,y,id) {
        if(lastMove!=moveId) return;
	if(state!=STATE_INIT) return;
        currentID = id;
        var obj = util.getDomObject("tooltipdiv");
        if(!obj) return;
        state = STATE_LINK;
        var link = util.getDomObject(id);
        x = this.getX(link,x);
        y = this.getY(link,y);
        util.setPosition(obj, x,y);
        var imgEvents = " onMouseOver=\"tooltip.onMouseOver(event,'" + id +"')\" " +
		" onMouseOut=\"tooltip.onMouseOut(event,'" + id +"')\" " +
		" onMouseMove=\"tooltip.onMouseMove(event,'" + id +"')\" " +
		" onClick=\"tooltip.onClick(event,'" + id +"')\" ";
	obj.obj.innerHTML = "<div class=tooltip-link-inner><img title=\"Show tooltip\" alt=\"Show tooltip\" " + imgEvents +" src=${urlroot}/icons/tooltip.gif></div>";
        showObject(obj);
    }

    function handleTooltip(request, obj) {
        var xmlDoc=request.responseXML.documentElement;
        text = getChildText(xmlDoc);
        obj.obj.innerHTML = "<div class=tooltip-inner><div id=\"tooltipwrapper\" ><table><tr valign=top><img width=\"16\" onmousedown=\"tooltip.doHide();\" id=\"tooltipclose\"  src=${urlroot}/icons/close.gif></td><td>&nbsp;</td><td>" + text+"</table></div></div>";
        showObject(obj);
    }

}

tooltip = new Tooltip();

document.onkeypress = tooltip.keyPressed;




function VisibilityGroup(img) {
	this.numEntries = 0;
	this.entries = new Array();
	this.toggleImg  = img;
	this.on = 1;
        this.groupAddEntry = groupAddEntry;
        this.groupToggleVisibility = groupToggleVisibility;
}


function groupAddEntry(entryId) {
	this.entries[this.numEntries] = entryId;
	this.numEntries++;
}



function groupToggleVisibility () {
    this.on = !this.on;
    if(this.toggleImg) {
	    var img = util.getDomObject(this.toggleImg);
            if(img) {
		if(this.on) {
   		    img.obj.src =  "${urlroot}/icons/downarrow.gif";
		} else {
	            img.obj.src =  "${urlroot}/icons/rightarrow.gif";
		}
            }
    }
    for(i=0;i<this.numEntries;i++) {
        obj = util.getDomObject(this.entries[i]);
        if(!obj) continue;
        if(this.on) {
            showObject(obj,"inline");
        } else {
            hideObject(obj);
        }
    }
}




function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    var img = util.getDomObject(imgid);
    if(toggleVisibility(id)) {
        if(img) img.obj.src = showimg;
    } else {
        if(img) img.obj.src = hideimg;
    }
}




function  handleFolderList(request, id) {
    var block = util.getDomObject("block_" + id);
    var img = util.getDomObject("img_" +id);
    var xmlDoc=request.responseXML.documentElement;
    block.obj.innerHTML = getChildText(xmlDoc);
    if(img) img.obj.src = "${urlroot}/icons/folderopen.gif";

}

function folderClick(id, output,args) {
    if (!output) output = "groupxml";
    if(!args) args ="";
    else args = "&" + args;
    var block = util.getDomObject("block_" + id);
    if(!block) return;
    var img = util.getDomObject("img_" +id);
    if(!block.obj.isOpen) {
        block.obj.isOpen = 1;
        showObject(block);
        if(img) img.obj.src = "${urlroot}/icons/progress.gif";
        url = "${urlroot}/entry/show?entryid=" + id +"&output=" + output+args;
	util.loadXML( url, handleFolderList,id);
    } else {
        if(img) img.obj.src = "${urlroot}/icons/folderclosed.gif";
        block.obj.isOpen = 0;
        hideObject(block);
    }
}




var selectors = new Array();

function Selector(event, id, allEntries, append) {
    this.id = id;
    this.allEntries = allEntries;
    this.append = append;
    this.textComp = util.getDomObject(id);
    if(!this.textComp)return false;

    event = util.getEvent(event);
    x = util.getEventX(event);
    y = util.getEventY(event);


    var link = util.getDomObject(id+'.selectlink');
    if(!link)return false;
    this.div = util.getDomObject('selectdiv');
    if(!this.div)return false;

    if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
        x= util.getLeft(link.obj);
        y = link.obj.offsetHeight+util.getTop(link.obj) + 2;
    } else {
        x+=20;
    }

    util.setPosition(this.div, x+10,y);
    showObject(this.div);
    url = "${urlroot}/entry/show?output=selectxml&append=" + this.append+"&allentries=" + this.allEntries+"&target=" + id;
    util.loadXML( url, handleSelect,id);
    return false;
}


function selectClick(id,entryId,value) {
    selector = selectors[id];
    if (selector.append=="true") {
        selector.textComp.obj.value =selector.textComp.obj.value+"[[" +entryId+"|"+value+"]]";
    } else {
        selector.textComp.obj.value =value;
    }
    selectCancel();
}

function selectCancel() {
    var div = util.getDomObject('selectdiv');
    if(!div)return false;
    hideObject(div);
}



function selectInitialClick(event, id,allEntries,append) {
    selectors[id] = new Selector(event,id,allEntries,append);
    return false;
}


function handleSelect(request, id) {
    selector = selectors[id];
    var xmlDoc=request.responseXML.documentElement;
    text = getChildText(xmlDoc);
    var close = "<a href=\"javascript:selectCancel();\"><img border=0 src=${urlroot}/icons/close.gif></a>";
    selector.div.obj.innerHTML = "<table width=100%><tr><td align=right>" + close +"</table>" +text;
}






function  getChildText(xmlDoc) {
    var text = '';
    for(i=0;i<xmlDoc.childNodes.length;i++) {
        text = text  + xmlDoc.childNodes[i].nodeValue;
    }
    return text;
	
}



function toggleVisibility(id) {
    var obj = util.getDomObject(id);
    return toggleVisibilityOnObject(obj,'block');
}


function hide(id) {
    hideObject(util.getDomObject(id));
}


function showMenu(event,srcId,id) {
    var obj = util.getDomObject(id);
    var srcObj = util.getDomObject(srcId);
    if(!obj || !srcObj) return;
    event = util.getEvent(event);
    x = util.getEventX(event);
    y = util.getEventY(event);
    if(srcObj.obj.offsetLeft && srcObj.obj.offsetWidth) {
        x= util.getLeft(srcObj.obj);
        y = srcObj.obj.offsetHeight+util.getTop(srcObj.obj) + 2;
    } 

    x+=2;
    x+=3;

    menuObject = obj;
    showObject(obj);
    
    util.setPosition(obj, x,y);
}


function show(id) {
    showObject(util.getDomObject(id));
}

function hideObject(obj) {
    if(!obj) return 0;
    obj.style.visibility = "hidden";
    obj.style.display = "none";
}


function hideMore(base) {
        var link = util.getDomObject("morelink_" + base);
        var div = util.getDomObject("morediv_" + base);
	hideObject(div);
	showObject(link);
}

function showMore(base) {
        var link = util.getDomObject("morelink_" + base);
        var div = util.getDomObject("morediv_" + base);
	hideObject(link);
	showObject(div);
}




function showObject(obj, display) {
    if(!obj) return 0;
    if(!display) display = "block";
    obj.style.visibility = "visible";
    obj.style.display = display;
    return 1;
}



function toggleVisibilityOnObject(obj, display) {
    if(!obj) return 0;
    if(obj.style.visibility == "hidden") {
        obj.style.visibility = "visible";
        obj.style.display = display;
        return 1;
    } else {
        obj.style.visibility = "hidden";
        obj.style.display = "none";
        return 0;
    }
}








function selectDate(div,field,id,fmt) {
    var cal = new CalendarPopup(div);
    cal.select(field,id,fmt);
}


function Tab(ids) {
	this.ids = ids;
	this.toggleTab = toggleTab;
	/*
	if (document.styleSheets[0].cssRules) {
		theRules = document.styleSheets[0].cssRules;
	} else if (document.styleSheets[0].rules) {
		theRules = document.styleSheets[0].rules;
	}
        for(i=0;i<theRules.length;i++) {
		rule = theRules[i];
		break;
	}*/


        for(i=0;i<ids.length;i++) {
		var contentId  = 'content_'+ids[i];
	        var content = util.getDomObject(contentId);
		var titleId  = 'title_'+ids[i];
	        var title = util.getDomObject(titleId);
		if(i==0) this.onStyle = title.style;
		else  this.offStyle = title.style;
	}
	this.toggleTab(this.ids[0]);
}

function toggleTab(mainId) {
    var mainContentId = 'content_' + mainId;
    for(i=0;i<this.ids.length;i++) {
	var contentId  = 'content_'+this.ids[i];
        var content = util.getDomObject(contentId);
	var titleId  = 'title_'+this.ids[i];
	var title = util.getDomObject(titleId);
        if(!content) {
		continue;
        }
	if(contentId==mainContentId) {
		content.style.visibility="visible";
                content.style.display = "block";
		title.style.backgroundColor="#ffffff";
		title.style.borderBottom = "2px #ffffff  solid";
	} else {
		content.style.visibility="hidden";
                content.style.display = "none";
		title.style.backgroundColor="#c3d9ff";
		title.style.borderBottom = "2px #000000  solid";
	}
    }

}
