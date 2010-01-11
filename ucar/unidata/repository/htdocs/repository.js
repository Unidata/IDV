
var root = "${urlroot}";
var urlroot = "${urlroot}";
var icon_close = "${urlroot}/icons/close.gif";
var icon_rightarrow = "${urlroot}/icons/grayrightarrow.gif";

var icon_downdart ="${urlroot}/icons/downdart.gif";
var icon_rightdart ="${urlroot}/icons/rightdart.gif";

var icon_progress = "${urlroot}/icons/progress.gif";
var icon_information = "${urlroot}/icons/information.png";
var icon_folderclosed = "${urlroot}/icons/folderclosed.png";
var icon_folderopen = "${urlroot}/icons/togglearrowdown.gif";
var icon_menuarrow = "${urlroot}/icons/downdart.gif";
var icon_blank = "${urlroot}/icons/blank.gif";



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
        if(!obj) {
            alert('could not find print output\n'+  s);
            return;
        }
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


    this.getBottom = function (obj) {
        if(!obj) return 0;
        return this.getTop(obj) + obj.offsetHeight;
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

    this.getStyle = function(obj) {
        if(obj.style) return obj.style;
        if (document.layers)  { 		
            return   document.layers[obj.name];
        }        
        return null;
    }

}

util = new Util();





var blockCnt=0;
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
   if(this.obj) {
      this.id = this.obj.id;
      if(!this.id) {
	this.id = "obj"+ (blockCnt++);
      }
   } 
   
}



function noop() {
}





var popupObject;
document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup   = mouseUp;


var mouseIsDown = 0;
var dragSource;
var draggedEntry;
var draggedEntryName;
var draggedEntryIcon;
var mouseMoveCnt =0;
var objectToHide;

function hidePopupObject() {
    if(objectToHide!=popupObject) {
	return;
    }
    if(popupObject) {
        hideObject(popupObject);
        popupObject = null;
    }
}



function mouseDown(event) {
    if(popupObject) {
	objectToHide = popupObject;
        setTimeout("hidePopupObject()",500);
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
        var dragSourceObj= util.getDomObject(dragSource);
        if(dragSourceObj) {
            var tox = util.getLeft(dragSourceObj.obj);
            var toy = util.getTop(dragSourceObj.obj);
            var fromx = parseInt(obj.style.left);
            var fromy = parseInt(obj.style.top);
            var steps = 10;
            var dx=(tox-fromx)/steps;
            var dy=(toy-fromy)/steps;
            flyBackAndHide('floatdiv',0,steps,fromx,fromy,dx,dy);
        } else {
            hideObject(obj);
        }
    }
    return true;
}




function flyBackAndHide(id, step,steps,fromx,fromy,dx,dy) {
    var obj = util.getDomObject(id);
    if(!obj) {
        return;
    }
    step=step+1;
    obj.style.left = fromx+dx*step+"px";
    obj.style.top = fromy+dy*step+"px";
    var opacity = 80*(steps-step)/steps;
    //    util.print(opacity);
    //    obj.style.filter="alpha(opacity="+opacity+")";
    //    obj.style.opacity="0." + opacity;

    if(step<steps) {
        var callback = "flyBackAndHide('" + id +"'," + step+","+steps+","+fromx+","+fromy+","+dx+","+dy+");"
        setTimeout(callback,30);
    } else {
        setTimeout("finalHide('" + id+"')",150);
        //        hideObject(obj);
    }
}

function finalHide(id) {
    var obj = util.getDomObject(id);
    if(!obj) {
        return;
    }
    hideObject(obj);
    obj.style.filter="alpha(opacity=80)";
    obj.style.opacity="0.8";
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
            var icon = "";
            if(draggedEntryIcon) {
                icon = "<img src=\"" +draggedEntryIcon+"\"/> ";
            }
            obj.obj.innerHTML = icon +draggedEntryName+"<br>Drag to a group to copy/move/associate";
        }
        obj.style.top = y;
        obj.style.left = x+10;
    }
}


function mouseOverOnEntry(event, entryId, targetId) {
    event = util.getEvent(event);
    if(entryId == draggedEntry) return;
    if(mouseIsDown)  {
        var obj = util.getDomObject(targetId);
        if(!obj)  return;
        //       if(obj.style && obj.style.borderBottom) {
        obj.style.borderBottom="2px black solid";
        //        }
    }
}

function mouseOutOnEntry(event, entryId,targetId) {
    event = util.getEvent(event);
    if(entryId == draggedEntry) return;
    var obj = util.getDomObject(targetId);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
}




function mouseDownOnEntry(event, entryId, name, sourceIconId, icon) {
    event = util.getEvent(event);
    dragSource  = sourceIconId;
    draggedEntry = entryId;
    draggedEntryName=name;
    draggedEntryIcon = icon;
    mouseIsDown = 1;
    if(event.preventDefault) {
        event.preventDefault();
    } else {
	event.returnValue = false;
        return false;
    }
}


function mouseUpOnEntry(event, entryId, targetId) {
    event = util.getEvent(event);
    if(entryId == draggedEntry) {
        return;
    }
    var obj = util.getDomObject(targetId);
    if(!obj)  {
        return;
    }
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
    if(draggedEntry && draggedEntry!=entryId) {
        url = "${urlroot}/entry/copy?action=action.move&from=" + draggedEntry +"&to=" + entryId;
        //	alert(url);
	window.open(url,'move window','') ;
        //        document.location = url;
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
    var showDelay = 1000;

    this.debug = function(msg) {
        util.print(msg);
    }
    this.keyPressed = function (event) {
        alert("key")
        tooltip.doHide();
        return;
        if(state==STATE_INIT) return;
        c =util.getKeyChar(event);
        if(c == '\r' && state == STATE_TIP) {
            tooltip.doHide();
        }
    }

    this.onMouseMove = function (event,id,linkId) {
        lastMove++;
        if(state!=STATE_INIT) return;
        event = util.getEvent(event);
        setTimeout("tooltip.showLink(" + lastMove+"," +util.getEventX(event)+","+ util.getEventY(event) +"," + "'" + id +"'"+  ",'" + linkId +"')", showDelay);
    }

    this.onMouseOut = function (event,id,linkId) {
        lastMove++;
        if(state !=STATE_LINK) return;
        setTimeout("tooltip.checkHide(" + lastMove+ ")", hideDelay);
    }


    this.onMouseOver = function(event,id,linkId) {
        event = util.getEvent(event);

        if(state ==STATE_LINK && currentID && id!=currentID) {
            this.doHide();
            currentID = null;

        }
        lastMove++;
        if(state!=STATE_INIT) return;
        setTimeout("tooltip.showLink(" + lastMove+"," +util.getEventX(event)+","+ util.getEventY(event) +"," + "'" + id +"'"+",'" + linkId +"')", showDelay);
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


    this.showLink = function(moveId,x,y,id,linkId) {
        if(lastMove!=moveId) return;
	if(state!=STATE_INIT) return;
        currentID = id;
        var obj = util.getDomObject("tooltipdiv");
        if(!obj) return;
        state = STATE_LINK;
        var link = util.getDomObject(linkId);
        x = this.getX(link,x);
        y = this.getY(link,y);
        util.setPosition(obj, x,y);
        var imgEvents = " onMouseOver=\"tooltip.onMouseOver(event,'" + id +"')\" " +
        " onMouseOut=\"tooltip.onMouseOut(event,'" + id +"')\" " +
        " onMouseMove=\"tooltip.onMouseMove(event,'" + id +"')\" " +
        " onClick=\"tooltip.onClick(event,'" + id +"')\" ";
	obj.obj.innerHTML = "<div class=tooltip-link-inner><img title=\"Show tooltip\" alt=\"Show tooltip\" " + imgEvents +" src="+icon_information +"></div>";
        showObject(obj);
    }

    function handleTooltip(request, obj) {
        var xmlDoc=request.responseXML.documentElement;
        text = getChildText(xmlDoc);
        obj.obj.innerHTML = "<div class=tooltip-inner><div id=\"tooltipwrapper\" ><table cellspacing=0 cellpadding=0><tr valign=top><img width=\"16\" onmousedown=\"tooltip.doHide();\" id=\"tooltipclose\"  src=" + icon_close +"></td><td>&nbsp;</td><td>" + text+"</table></div></div>";
        showObject(obj);
    }

}

tooltip = new Tooltip();

document.onkeypress = tooltip.keyPressed;
var keyEvent;


function handleKeyPress(event) {
    keyEvent = event;
    c =util.getKeyChar(event);
    div = util.getDomObject("tooltipdiv");
    if(!div) return;
    hideObject(div);
}

document.onkeypress = handleKeyPress;

var groups = new Array();
var groupList = new Array();



function EntryFormList(formId,img,selectId, initialOn) {

    this.entryRows = new Array();
    this.lastEntryRowClicked=null;
    groups[formId] = this;
    groupList[groupList.length] = this;
    this.formId = formId;
    this.toggleImg  = img;
    this.on = initialOn;
    this.entries = new Array();

    this.groupAddEntry = function(entryId) {
        this.entries[this.entries.length] = entryId;
    }

    this.addEntryRow = function(entryRow) {
        this.groupAddEntry(entryRow.cbxWrapperId);
        this.entryRows[this.entryRows.length] = entryRow;
        if(!this.on) {
            hideObject(entryRow.cbx);
        } else {
            showObject(entryRow.cbx,"inline");
	}
    }


    this.groupAddEntry(selectId);
    if(!this.on) {
        hideObject(selectId);
    }

    this.groupToggleVisibility = function  () {
        this.on = !this.on;
        this.setVisibility();
    }


    this.findEntryRow =function(rowId) {
        for (i = 0; i < this.entryRows.length; i++) {
            if(this.entryRows[i].rowId == rowId) {
                return  this.entryRows[i];
            }
        }
        return null;
    }



    this.checkboxClicked = function(event,cbxId) {
        if(!event) return;
        var entryRow;
        for (i = 0; i < this.entryRows.length; i++) {
            if(this.entryRows[i].cbxId ==cbxId) {
                entryRow = this.entryRows[i];
                break;
            }
        }

        if(!entryRow || !entryRow.cbx) return;


        var value = entryRow.getCheckboxValue();
        if(event.ctrlKey) {
            for (i = 0; i < this.entryRows.length; i++) {
                this.entryRows[i].setCheckbox(value);
            }
        }

        if(event.shiftKey) {
            if(this.lastEntryRowClicked) {
                 var pos1 = util.getTop(this.lastEntryRowClicked.cbx);
	         var pos2 = util.getTop(entryRow.cbx);
		 if(pos1>pos2) {
		    var tmp = pos1;
		    pos1 =pos2;
		    pos2=tmp;
                 }
		 for (i = 0; i < this.entryRows.length; i++) {
        		var top = util.getTop(this.entryRows[i].cbx);
			if(top>=pos1 && top<=pos2) {
		            this.entryRows[i].setCheckbox(value);
			}
        	    }
            }
            return;
        }
        this.lastEntryRowClicked = entryRow;
    }

    this.setVisibility = function  () {
        if(this.toggleImg) {
	    var img = util.getDomObject(this.toggleImg);
            if(img) {
		if(this.on) {
   		    img.obj.src =  icon_downdart;
		} else {
	            img.obj.src =  icon_rightdart;

		}
            }
        }

        var form = util.getDomObject(this.formId);
        if(form) {
            form = form.obj;
            for(i=0;i<form.elements.length;i++) { 
                if(this.on) {
                    showObject(form.elements[i],"inline");
                } else {
                    hideObject(form.elements[i]);
                }

                
            }
        }


        for(i=0;i<this.entries.length;i++) {
            obj = util.getDomObject(this.entries[i]);
            if(!obj) continue;
            if(this.on) {
                showObject(obj,"inline");
            } else {
                hideObject(obj);
            }
        }
    }
}


function entryRowCheckboxClicked(event,cbxId) {

    var cbx = util.getDomObject(cbxId);
    if(!cbx) return;
    cbx = cbx.obj;
    if(!cbx.form) return;
    var visibilityGroup = groups[cbx.form.id];
    if(visibilityGroup) {
        visibilityGroup.checkboxClicked(event,cbxId);
    }
}

function initEntryListForm(formId) {
    var visibilityGroup = groups[formId];
    if(visibilityGroup) {
        visibilityGroup.on = 0;
        visibilityGroup.setVisbility();
    }
}


function EntryRow (entryId, rowId, cbxId,cbxWrapperId) {
    this.entryId = entryId;

    this.onColor = "#FFFFCC";
    this.overColor = "#f6f6f6";
    this.overColor = "#edf5ff";
    this.rowId = rowId;
    this.cbxId = cbxId;
    this.cbxWrapperId = cbxWrapperId;
    this.cbx = util.getDomObject(cbxId);
    this.row = util.getDomObject(rowId);
    if(this.row) {
        this.row = this.row.obj;
    }


    if(this.cbx) {
        this.cbx = this.cbx.obj;
        var form = this.cbx.form;
        if(form) {
            var visibilityGroup = groups[form.id];
            if(visibilityGroup) {
                visibilityGroup.addEntryRow(this);
            }
        } else {
            hideObject(this.cbx);
        }
    }


    this.setCheckbox = function(value) {
        if(this.cbx) this.cbx.checked = value;
        this.setRowColor();
    }

    this.getCheckboxValue = function() {
        if(this.cbx) return this.cbx.checked;
        return 0;		
    }
        
    this.setRowColor = function() {
        if(this.cbx && this.cbx.checked) {
            this.row.style.backgroundColor = this.onColor;		
        } else {
            this.row.style.backgroundColor = "#ffffff";
        }
    }


    this.mouseOver = function(event) {
        img = util.getDomObject("entrymenuarrow_" +rowId);
        if(img) {
            img.obj.src =  icon_menuarrow;
        }
        
        this.row.style.backgroundColor = this.overColor;
        this.row.style.border =  "1px #ddd  dotted";
    }

    this.mouseClick = function(event) {
        left = util.getLeft(this.row);
        eventX = util.getEventX(event);
        if(eventX-left<50) return;
        var url = "${urlroot}/entry/show?entryid=" + entryId +"&output=metadataxml";
	util.loadXML( url, this.handleTooltip,this);
    }

    this.handleTooltip = function(request,entryRow) {
        var xmlDoc=request.responseXML.documentElement;
        text = getChildText(xmlDoc);
        div = util.getDomObject("tooltipdiv");
        if(!div) return;

        util.setPosition(obj, util.getLeft(entryRow.row), util.getBottom(entryRow.row));

        div.obj.innerHTML = "<div class=tooltip-inner><div id=\"tooltipwrapper\" ><table><tr valign=top><img width=\"16\" onmousedown=\"hideEntryPopup();\" id=\"tooltipclose\"  src=" + icon_close +"></td><td>" + text+"</table></div></div>";
        showObject(div);
    }



    this.mouseOut = function(event) {
        img = util.getDomObject("entrymenuarrow_" +rowId);
        if(img) {
            img.obj.src =  icon_blank;
        }
        this.setRowColor();
        //        mouseOutOnEntry(event, "", rowId);
        //        this.row.style.borderBottom =  "1px #fff  solid";
        this.row.style.border =  "1px #fff  solid";
    }
}


function hideEntryPopup() {
    hideObject(util.getDomObject("tooltipdiv"));
}

function findEntryRow(rowId) {
    for(i=0;i<groupList.length;i++) {
        var entryRow = groupList[i].findEntryRow(rowId);
        if(entryRow) return entryRow;
    }
    return null;
}


function entryRowOver(rowId) {
    var entryRow = findEntryRow(rowId);
    if(entryRow) entryRow.mouseOver();
}


function entryRowOut(rowId) {
    var entryRow = findEntryRow(rowId);
    if(entryRow) entryRow.mouseOut();
}

function entryRowClick(event,rowId) {
    var entryRow = findEntryRow(rowId);
    if(entryRow) entryRow.mouseClick(event);
}





function indexOf(array,object) {
    for (i = 0; i <= array.length; i++) {
        if(array[i] == object) return i;
    }
    return -1;
}


var lastCbxClicked;

function checkboxClicked(event, cbxPrefix, id) {
    if(!event) return;
    var cbx = util.getDomObject(id);
    if(!cbx) return;
    cbx = cbx.obj;

    var checkBoxes = new Array();
    if(!cbx.form) return;
    var elements = cbx.form.elements;
    for(i=0;i<elements.length;i++) {
        if(elements[i].name.indexOf(cbxPrefix)>=0 || elements[i].id.indexOf(cbxPrefix)>=0) {
            checkBoxes[checkBoxes.length] = elements[i];
        }
    }


    var value = cbx.checked;
    if(event.ctrlKey) {
        for (i = 0; i < checkBoxes.length; i++) {
	    checkBoxes[i].checked = value;
        }
    }


    if(event.shiftKey) {
        if(lastCbxClicked) {
	    var pos1 = util.getTop(cbx);
	    var pos2 = util.getTop(lastCbxClicked);
	    if(pos1>pos2) {
		var tmp = pos1;
		pos1 =pos2;
		pos2=tmp;
	    }
	    for (i = 0; i < checkBoxes.length; i++) {
		var top = util.getTop(checkBoxes[i]);
		if(top>=pos1 && top<=pos2) {
	                checkBoxes[i].checked = value;
		}
            }
        }
        return;
    }
    lastCbxClicked = cbx;
}








function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    var img = util.getDomObject(imgid);
    if(toggleVisibility(id,'block')) {
        if(img) img.obj.src = showimg;
    } else {
        if(img) img.obj.src = hideimg;
    }
}


function toggleInlineVisibility(id, imgid, showimg, hideimg) {
    var img = util.getDomObject(imgid);
    if(toggleVisibility(id,'inline')) {
        if(img) img.obj.src = showimg;
    } else {
        if(img) img.obj.src = hideimg;
    }
}







var originalImages = new Array();
var changeImages = new Array();

function folderClick(uid, url, changeImg) {
    changeImages[uid] = changeImg;
    var block = util.getDomObject('block_'+uid);
    if(!block) {
	block = util.getDomObject(uid);    
    }

    if(!block) {
//        alert("no block " + uid);
	return;
    }
    var img = util.getDomObject("img_" +uid);
    if(!block.obj.isOpen) {
	originalImages[uid] = img.obj.src;
        block.obj.isOpen = 1;
        //        Effect.SlideDown(block.obj.id, {'duration' : 0.4});
        showObject(block);
        if(img) img.obj.src = icon_progress;
	util.loadXML( url, handleFolderList,uid);
    } else {
	if(changeImg && img) {
            if(originalImages[uid]) {
                img.obj.src = originalImages[uid];
            } else 
                img.obj.src = icon_folderclosed;
        }
        block.obj.isOpen = 0;
//        Effect.SlideUp(block.obj.id, {'duration' : 0.5});
        hideObject(block);
    }
}



function  handleFolderList(request, uid) {
    var block = util.getDomObject('block_'+uid);
    if(!block) {
	block = util.getDomObject(uid);    
    }
    var img = util.getDomObject("img_" +uid);
    if(request.responseXML!=null) {
        var xmlDoc=request.responseXML.documentElement;
	var script;
	var html;

	for(i=0;i<xmlDoc.childNodes.length;i++) {
            var childNode = xmlDoc.childNodes[i];
            if(childNode.tagName=="javascript") {
                script =getChildText(childNode);
            } else if(childNode.tagName=="content") {
                html = getChildText(childNode);
            }  else {
            }
	}
        if(!html) {
            html = getChildText(xmlDoc);
        }
	if(html) {
            block.obj.innerHTML = "<div>"+html+"</div>";
	}
	if(script) {
            eval(script);
	}
    }
    
    if(img) {
        if(changeImages[uid]) {
            img.obj.src = icon_folderopen;
        } else {
            img.obj.src = originalImages[uid];
        }
    }

}

function scrollObject(id,cnt,lastHeight) {
    var block = util.getDomObject(id);
    cnt--;
    if(cnt>0) {
          block.style.maxHeight=parseInt(block.style.maxHeight)+20;
          if(lastHeight!= block.obj.clientHeight) {
              setTimeout("scrollObject('" + block.id +"',"+cnt+","+(block.obj.clientHeight)+")",100);
              return;
          }
    } 
    block.style.border = "none";
    block.style.maxHeight=1000;
}



var selectors = new Array();

function Selector(event, id, allEntries, selecttype, localeId) {
    this.id = id;
    this.localeId = localeId;
    this.allEntries = allEntries;
    this.selecttype = selecttype;
    this.textComp = util.getDomObject(id);
    this.hiddenComp = util.getDomObject(id+"_hidden");

    this.clearInput = function() {
	if(this.hiddenComp) {
            this.hiddenComp.obj.value =""
        }
	if(this.textComp) {
            this.textComp.obj.value =""
        }
    }


    if (!this.textComp) {
//	alert("cannot find text comp " + id);
	return false;
    }

    event = util.getEvent(event);
    x = util.getEventX(event);
    y = util.getEventY(event);


    var link = util.getDomObject(id+'.selectlink');
    if(!link) {
	return false;
    }
    this.div = util.getDomObject('selectdiv');
    if(!this.div) {
	return false;
    }

    if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
        x= util.getLeft(link.obj);
        y = link.obj.offsetHeight+util.getTop(link.obj) + 2;
    } else {
        x+=20;
    }

    util.setPosition(this.div, x+10,y);
    showObject(this.div);
    url = "${urlroot}/entry/show?output=selectxml&selecttype=" + this.selecttype+"&allentries=" + this.allEntries+"&target=" + id+"&noredirect=true";
    if(localeId) {
        url = url+"&localeid=" + localeId;
    }
    util.loadXML( url, handleSelect,id);
    return false;
}



function insertText(id,value) {
    var textComp = util.getDomObject(id);
    if(textComp) {
	insertAtCursor(textComp.obj, value);
    }
}

function selectClick(id,entryId,value) {
    selector = selectors[id];
    if (selector.selecttype=="wikilink") {
        insertAtCursor(selector.textComp.obj,"[[" +entryId+"|"+value+"]]");
    } else if (selector.selecttype=="entryid") {
        insertTagsInner(selector.textComp.obj, "{{import " +entryId+" "," }}","importtype");
    } else { 
        if(selector.hiddenComp) {
            selector.hiddenComp.obj.value =entryId;

        }
        selector.textComp.obj.value =value;
	if(selector.textComp.obj.value) {
	        selector.textComp.obj.value =value;
	} else {
	        selector.textComp.obj.innerHtml =value;
	}
    }
    selectCancel();
}

function selectCancel() {
    var div = util.getDomObject('selectdiv');
    if(!div)return false;
    hideObject(div);
}



function selectInitialClick(event, id,allEntries,selecttype, localeId) {
    selectors[id] = new Selector(event,id,allEntries,selecttype,localeId);
    return false;
}


function clearSelect(id) {
    selector = selectors[id];
    if(selector) selector.clearInput();
}


function handleSelect(request, id) {
    selector = selectors[id];
    var xmlDoc=request.responseXML.documentElement;
    text = getChildText(xmlDoc);
    var close = "<a href=\"javascript:selectCancel();\"><img border=0 src=" + icon_close + "></a>";
    selector.div.obj.innerHTML = "<table width=100%><tr><td align=right>" + close +"</table>" +text;
}




function  getChildText(node) {
    var text = '';
    for(childIdx=0;childIdx<node.childNodes.length;childIdx++) {
        text = text  + node.childNodes[childIdx].nodeValue;
    }
    return text;
	
}


function toggleVisibility(id,style) {
    if(!style) style='block';
    var obj = util.getDomObject(id);
    return toggleVisibilityOnObject(obj,style);
}


function hide(id) {
    hideObject(util.getDomObject(id));
}

function setFormValue(id, value) {
    var obj = util.getDomObject(id);
    obj.obj.value   = value;
}


function setHtml(id, html) {
    var obj = util.getDomObject(id);
    obj.obj.innerHTML = html;
}

function showAjaxPopup(event,srcId,url) {
    util.loadXML(url, handleAjaxPopup,srcId);
}

function handleAjaxPopup(request, srcId) {
    var xmlDoc=request.responseXML.documentElement;
    text = getChildText(xmlDoc);
    var srcObj = util.getDomObject(srcId);
    var obj = util.getDomObject("tooltipdiv");
    obj.obj.innerHTML = "<div class=tooltip-inner><div id=\"tooltipwrapper\" ><table><tr valign=top><img width=\"16\" onmousedown=\"tooltip.doHide();\" id=\"tooltipclose\"  src=" + icon_close +"></td><td>&nbsp;</td><td>" + text+"</table></div></div>";
    showObject(obj);
}


function showPopup(event, srcId, popupId, alignLeft) {
    hidePopupObject();
    var popup = util.getDomObject(popupId);
    var srcObj = util.getDomObject(srcId);
    if(!popup || !srcObj) return;
    event = util.getEvent(event);
    x = util.getEventX(event);
    y = util.getEventY(event);
    if(srcObj.obj.offsetLeft && srcObj.obj.offsetWidth) {
        x = util.getLeft(srcObj.obj);
        y = srcObj.obj.offsetHeight+util.getTop(srcObj.obj) + 2;
    } 

    if(alignLeft) {
        x = util.getLeft(srcObj.obj);
        y = srcObj.obj.offsetHeight+util.getTop(srcObj.obj) + 2;
    } else {
        x+=2;
        x+=3;
    }

    popupObject = popup;
    showObject(popup);
    util.setPosition(popup, x,y);
}




function showStickyPopup(event, srcId, popupId, alignLeft) {
    var popup = util.getDomObject(popupId);
    var srcObj = util.getDomObject(srcId);
    if(!popup || !srcObj) return;
    event = util.getEvent(event);
    x = util.getEventX(event);
    y = util.getEventY(event);
    if(srcObj.obj.offsetLeft && srcObj.obj.offsetWidth) {
        x = util.getLeft(srcObj.obj);
        y = srcObj.obj.offsetHeight+util.getTop(srcObj.obj) + 2;
    } 

    if(alignLeft) {
        x = util.getLeft(srcObj.obj);
        y = srcObj.obj.offsetHeight+util.getTop(srcObj.obj) + 2;
    } else {
        x+=2;
        x+=3;
    }

    showObject(popup);
    util.setPosition(popup, x,y);
}


function show(id) {
    showObject(util.getDomObject(id));
}

function hideObject(obj) {
    if(!obj) {
        return 0;
    }

    var style = util.getStyle(obj);
    if(!style) {
        return 0;
    }
    style.visibility = "hidden";
    style.display = "none";
    return 1;
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

    var style = util.getStyle(obj);
    if(!style) {
        alert("no style");
        return 0;
    }
    style.visibility = "visible";
    style.display = display;
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






function findFormElement(form, id) {
    var form = document.forms[form];
    if(form) {
        if(form[id]) return form[id];
    }
    obj = util.getDomObject(id);
    if(obj) return obj.obj;
    return null;
}


function selectDate(div,field,id,fmt) {
    var cal = new CalendarPopup(div);
    cal.select(field,id,fmt);
}


var tabs = new Array();

function tabPress(tabId,ids,what) {
    if(!tabs[tabId]) {
        tabs[tabId] = new Tab(ids);
    }
    tabs[tabId].toggleTab(what);
}



function Tab(ids) {
    this.ids = ids;
    this.toggleTab = toggleTab;
    this.onColor = "#ffffff";
    this.offColor = "#dddddd";

    for(i=0;i<ids.length;i++) {
        var contentId  = 'content_'+ids[i];
        var content = util.getDomObject(contentId);
        var titleId  = 'title_'+ids[i];
        var title = util.getDomObject(titleId);
        if(i==0) {
            this.onStyle = title.style;
            if(title.style.backgroundColor) {
                //this.onColor = title.style.backgroundColor;
            }
        } else {
            this.offStyle = title.style;
            if(title.style.backgroundColor) {
                //this.offColor = title.style.backgroundColor;
            }
        }
    }
    //	this.toggleTab(this.ids[0]);
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
            content.style.backgroundColor=this.onColor;
            title.style.backgroundColor=this.onColor;
            title.style.borderBottom = "2px #ffffff  solid";
	} else {
            content.style.visibility="hidden";
            content.style.display = "none";
            title.style.backgroundColor=this.offColor;
            title.style.borderBottom = "1px #000000 solid";
	}
    }
}


function insertAtCursor(myField, myValue) {
    //IE support
    if (document.selection) {
        myField.focus();
        sel = document.selection.createRange();
        sel.text = myValue;
    }
    //MOZILLA/NETSCAPE support
    else if (myField.selectionStart || myField.selectionStart == '0') {
        var startPos = myField.selectionStart;
        var endPos = myField.selectionEnd;
        myField.value = myField.value.substring(0, startPos)
            + myValue
            + myField.value.substring(endPos, myField.value.length);
    } else {
        myField.value += myValue;
    }
}



function insertTags(id, tagOpen, tagClose, sampleText) {
    var textComp = util.getDomObject(id);
    if(textComp) {
	insertTagsInner(textComp.obj, tagOpen,tagClose,sampleText);
    }
}


// apply tagOpen/tagClose to selection in textarea,
// use sampleText instead of selection if there is none
function insertTagsInner(txtarea, tagOpen, tagClose, sampleText) {
    var selText, isSample = false;

    if (document.selection  && document.selection.createRange) { // IE/Opera

        //save window scroll position
        if (document.documentElement && document.documentElement.scrollTop)
            var winScroll = document.documentElement.scrollTop
            else if (document.body)
                var winScroll = document.body.scrollTop;
        //get current selection  
        txtarea.focus();
        var range = document.selection.createRange();
        selText = range.text;
        //insert tags
        checkSelectedText();
        range.text = tagOpen + selText + tagClose;
        //mark sample text as selected
        if (isSample && range.moveStart) {
            if (window.opera)
                tagClose = tagClose.replace(/\n/g,'');
            range.moveStart('character', - tagClose.length - selText.length); 
            range.moveEnd('character', - tagClose.length); 
        }
        range.select();   
        //restore window scroll position
        if (document.documentElement && document.documentElement.scrollTop)
            document.documentElement.scrollTop = winScroll
            else if (document.body)
                document.body.scrollTop = winScroll;

    } else if (txtarea.selectionStart || txtarea.selectionStart == '0') { // Mozilla

        //save textarea scroll position
        var textScroll = txtarea.scrollTop;
        //get current selection
        txtarea.focus();
        var startPos = txtarea.selectionStart;
        var endPos = txtarea.selectionEnd;
        selText = txtarea.value.substring(startPos, endPos);
        //insert tags
        checkSelectedText();
        txtarea.value = txtarea.value.substring(0, startPos)
            + tagOpen + selText + tagClose
            + txtarea.value.substring(endPos, txtarea.value.length);
        //set new selection
        if (isSample) {
            txtarea.selectionStart = startPos + tagOpen.length;
            txtarea.selectionEnd = startPos + tagOpen.length + selText.length;
        } else {
            txtarea.selectionStart = startPos + tagOpen.length + selText.length + tagClose.length;
            txtarea.selectionEnd = txtarea.selectionStart;
        }
        //restore textarea scroll position
        txtarea.scrollTop = textScroll;
    } 

    function checkSelectedText(){
        if (!selText) {
            selText = sampleText;
            isSample = true;
        } else if (selText.charAt(selText.length - 1) == ' ') { //exclude ending space char
            selText = selText.substring(0, selText.length - 1);
            tagClose += ' '
		} 
    }

}


var imageDoFirst = 1;
function editImageClick(event, imgId, pt1x,pt1y,pt2x,pt2y) {
    var obj = util.getDomObject(imgId);

    if(obj) {
	var ex  = util.getEventX(event);
	var ey  = util.getEventY(event);
        var  idx =  util.getLeft(obj.obj);
        var  idy =  util.getTop(obj.obj);
	var ix = ex-idx;
	var iy = ey-idy;

	var    fldx1= util.getDomObject(pt1x);
	var    fldy1= util.getDomObject(pt1y);
	var    fldx2= util.getDomObject(pt2x);
	var    fldy2= util.getDomObject(pt2y);
        var fldx;
	var fldy;
	if(imageDoFirst) {
            imageDoFirst	 =0;
	    fldx= fldx1;
	    fldy= fldy1;
	    fldx2.obj.value	 = ""+0
            fldy2.obj.value	 = ""+0;
        } else {
  	     imageDoFirst = 1;
	     fldx= fldx2;
	     fldy= fldy2;
        }
	if(fldx) {
		fldx.obj.value	 = ""+ix;
		fldy.obj.value	 = ""+iy;
	}
	var box = util.getDomObject("image_edit_box");
	if(box) {
		var style = util.getStyle(box);
		style.visibility =  "visible";
                style.left = idx+parseInt(fldx1.obj.value);
		style.top =  idy+parseInt(fldy1.obj.value);
		var x2 = parseInt(fldx2.obj.value);
		var y2 = parseInt(fldy2.obj.value);
	        if(x2>0) {
 	            style.width = x2-parseInt(fldx1.obj.value);
	            style.height = y2-parseInt(fldy1.obj.value);
		}  else {
 	            style.width = 0;
	            style.height = 0;
		}
//		alert(style.top + " " + style.left + " w:" + style.width + " " + style.height);
	}
    }
}



function MapSelector (imgId, argBase, absolute) {
    this.latLonX1=0;
    this.latLonY1=0;
    this.latLonX2=-1;
    this.latLonY2=-1;
    this.latLonBoxOffset = -1;
    this.doit = function() {
        return "hello";
    }


    this.doFirst = 1;
    this.imgId = imgId;
    this.argBase = argBase;
    this.absolute = absolute;
    this.image = util.getDomObject(imgId);



    this.mapIdx=0;



    this.fldNorth= util.getDomObject(argBase+"_north");
    this.fldSouth= util.getDomObject(argBase+"_south");
    this.fldEast= util.getDomObject(argBase+"_east");
    this.fldWest= util.getDomObject(argBase+"_west");
    this.fldLat= util.getDomObject(argBase+"_lat");
    this.fldLon= util.getDomObject(argBase+"_lon");

    this.box = util.getDomObject(argBase+ "_bbox_div");


    this.cycleMap = function() {
        initMaps();
        this.mapIdx++;
        if(this.mapIdx >= mapImages.length) this.mapIdx=0;
        this.image.obj.src = mapImages[this.mapIdx].url;






        this.init();
    }

    this.clear = function() {
        this.doFirst = 1;
        if(this.fldNorth)
            this.fldNorth.obj.value = "";
        if(this.fldSouth)
            this.fldSouth.obj.value = "";
        if(this.fldEast)
            this.fldEast.obj.value = "";
        if(this.fldWest)
            this.fldWest.obj.value = "";
        if(this.fldLat)
            this.fldLat.obj.value = "";
        if(this.fldLon)
            this.fldLon.obj.value = "";
        if(this.box) {
            var style = util.getStyle(this.box);
            style.visibility =  "hidden";
        }
    }


    this.init = function() {


       if(this.latLonBoxOffset<0) 
            this.latLonBoxOffset = this.box.obj.offsetTop;

        var valuesOk = true;
       if(this.fldNorth) {
           if(this.fldNorth.obj.value == "") valuesOk = false;
           if(this.fldSouth.obj.value == "") valuesOk = false;
           if(this.fldEast.obj.value == "") valuesOk = false;
           if(this.fldWest.obj.value == "") valuesOk = false;
       } else if(this.fldLat) {
           if(this.fldLat.obj.value == "") valuesOk = false;
           if(this.fldLon.obj.value == "") valuesOk = false;
       }

        if(!valuesOk) {
            if(this.box) {
                var style = util.getStyle(this.box);
                style.visibility =  "hidden";
            }
            return;
        }

       if(this.fldNorth) {
           lat1 = parseFloat(this.fldNorth.obj.value);
           lon1 = parseFloat(this.fldWest.obj.value);

           lat2 = parseFloat(this.fldSouth.obj.value);
           lon2 = parseFloat(this.fldEast.obj.value);
       } else if(this.fldLat) {
           lat1 = parseFloat(this.fldLat.obj.value);
           lon1 = parseFloat(this.fldLon.obj.value);

           lat2 = lat1;
           lon2 = lon1;
       }

        initMaps();
        var map = mapImages[this.mapIdx];


       imageWidth = this.image.obj.width;
       imageHeight = this.image.obj.height;
       

        iy1 = imageHeight*(map.originLat - lat1)/map.height;
        iy2 = imageHeight*(map.originLat - lat2)/map.height;

        ix1 = imageWidth*(lon1-map.originLon)/map.width;
        ix2 = imageWidth*(lon2-map.originLon)/map.width;

        var  imageLeft =  util.getLeft(this.image.obj);
        var  imageTop =  util.getTop(this.image.obj);

        if(this.box) {
            var style = util.getStyle(this.box);
            style.visibility =  "visible";
            if(absolute) {
                style.left = ix1;
                style.top =  iy1+this.latLonBoxOffset;
            } else {
                style.left = imageLeft+ix1;
                style.top =  imageTop+iy1;
            }

            if(ix2>0) {
                style.width = ix2-ix1;
                style.height = iy2-iy1;
            }  else {
                style.width = 0;
                style.height = 0;
            }
        }
    }


    this.update  =function() {
        this.init();
    }

    this.click = function(event) {
        var ex  = util.getEventX(event);
        var ey  = util.getEventY(event);

        var ulX =  util.getLeft(this.image.obj);
        var ulY =  util.getTop(this.image.obj);


        var imageX = ex - ulX;
        var imageY = ey - ulY;



        if(this.doFirst) {
            this.doFirst =0;
            this.latLonX1 = imageX;
            this.latLonY1 = imageY;
            this.latLonX2 = this.latLonX1;
            this.latLonY2 = this.latLonY1;
            if(this.fldLat) this.doFirst = 1;
        } else {
            this.doFirst = 1;
            this.latLonX2 = imageX;
            this.latLonY2 = imageY;
        }

        var x1 = Math.min(this.latLonX1,this.latLonX2);
        var y1 = Math.min(this.latLonY1,this.latLonY2);

        var x2 = Math.max(this.latLonX1,this.latLonX2);
        var y2 = Math.max(this.latLonY1,this.latLonY2);

        initMaps();

        var map = mapImages[this.mapIdx];



       imageWidth = this.image.obj.width;
       imageHeight = this.image.obj.height;
        //        alert("height: " + imageHeight);


        var north =  map.originLat-y1/imageHeight*map.height;
        var south =  map.originLat-y2/imageHeight*map.height;
        var west =  map.originLon  + x1/imageWidth*map.width;
        var east =  map.originLon + x2/imageWidth*map.width;


        if(this.fldNorth) {
            this.fldNorth.obj.value = ""+north;
            this.fldSouth.obj.value = ""+south;
            this.fldWest.obj.value = ""+west;
            this.fldEast.obj.value = ""+east;
        } else if(this.fldLat) {
            this.fldLat.obj.value = ""+north;
            this.fldLon.obj.value = ""+west;
        }

        if(this.box) {
            var style = util.getStyle(this.box);
            style.visibility =  "visible";
            if(this.absolute) {
                style.left = x1;
                style.top =  17+y1;
                //	    style.top =  latLonY1+image.obj.offsetTop;
            } else {
                style.left = ulX+x1;
                style.top =  ulY+y1;
            }
            style.width = x2-x1;
            style.height = y2-y1;
            //	alert(style.left+"/" +style.top + "  " + style.width+"/" + style.height);
        }
    }

}



var mapImages;

function Map(url, ulLon, ulLat, lrLon, lrLat) {
    this.url=url;
    this.originLon = ulLon;
    this.originLat = ulLat;
    this.width = lrLon-ulLon;
    this.height  =ulLat-lrLat;
}



function initMaps() {
    if(mapImages) return;
    mapImages= new Array();
    mapImages[mapImages.length] = new Map("${urlroot}/images/maps/caida.jpg",-180,90,180,-90);
    mapImages[mapImages.length] = new Map("${urlroot}/images/maps/conus.png",-126.57781982421875,51.126121520996094,-65.26223754882812,18.237728118896484);
    mapImages[mapImages.length] = new Map("${urlroot}/images/maps/usgstopo.jpg",-180,90,180,-90);
    mapImages[mapImages.length] = new Map("${urlroot}/images/maps/worldday.jpg",-180,90,180,-90);
}




var http_request = false;
function makePOSTRequest(url, parameters) {
    http_request = false;

    if (window.XMLHttpRequest) { // Mozilla, Safari,...
        http_request = new XMLHttpRequest();
        if (http_request.overrideMimeType) {
            // set type accordingly to anticipated content type
            //http_request.overrideMimeType('text/xml');
            http_request.overrideMimeType('text/html');
        }
    } else if (window.ActiveXObject) { // IE
        try {
            http_request = new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {
                http_request = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e) {}
        }
    }
    if (!http_request) {
        alert('Cannot create XMLHTTP instance');
        return false;
    }
      
    http_request.onreadystatechange = alertContents;
    http_request.open('POST', url, true);
    http_request.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    http_request.setRequestHeader("Content-length", parameters.length);
    http_request.setRequestHeader("Connection", "close");
    http_request.send(parameters);
}

function alertContents() {
    if (http_request.readyState == 4) {
        if (http_request.status == 200) {
            result = http_request.responseText;
        } else {
            alert('There was a problem with the request.');
        }
    }
}

