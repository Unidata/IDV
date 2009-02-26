
var root = "${urlroot}";
var icon_close = "${urlroot}/icons/close.gif";
var icon_rightarrow = "${urlroot}/icons/grayrightarrow.gif";

var icon_downdart ="${urlroot}/icons/downdart.gif";
var icon_rightdart ="${urlroot}/icons/rightdart.gif";

var icon_progress = "${urlroot}/icons/progress.gif";
var icon_information = "${urlroot}/icons/information.png"
var icon_folderclosed = "${urlroot}/icons/folderclosed.png";
var icon_folderopen = "${urlroot}/icons/togglearrowdown.gif";



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






var popupObject;
document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup   = mouseUp;


var mouseIsDown = 0;
var draggedEntry;
var draggedEntryName;
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




function mouseDownOnEntry(event, entryId, name) {
    event = util.getEvent(event);
    draggedEntry = entryId;
    draggedEntryName=name;
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
    if(entryId == draggedEntry) return;
    var obj = util.getDomObject(targetId);
    if(!obj)  return;
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
    var showDelay = 500;

    this.debug = function(msg) {
        util.print(msg);
    }
    this.keyPressed = function (event) {
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
        obj.obj.innerHTML = "<div class=tooltip-inner><div id=\"tooltipwrapper\" ><table><tr valign=top><img width=\"16\" onmousedown=\"tooltip.doHide();\" id=\"tooltipclose\"  src=" + icon_close +"></td><td>&nbsp;</td><td>" + text+"</table></div></div>";
        showObject(obj);
    }

}

tooltip = new Tooltip();

document.onkeypress = tooltip.keyPressed;
var keyEvent;


function handleKeyPress(event) {
    keyEvent = event;
    c =util.getKeyChar(event);
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

        if(event.ctrlKey) {
            var value = entryRow.getCheckboxValue();
            for (i = 0; i < this.entryRows.length; i++) {
                this.entryRows[i].setCheckbox(value);
            }
        }

        if(event.shiftKey) {
            if(this.lastEntryRowClicked) {
                var idx1 = indexOf(this.entryRows, this.lastEntryRowClicked);
                var idx2 = indexOf(this.entryRows, entryRow);
                var value = entryRow.getCheckboxValue();
                if(idx1>idx2) {
                    var tmp = idx1;
                    idx1=idx2;
                    idx2=tmp;
                }

                for(i=idx1;i<=idx2;i++) {
                    this.entryRows[i].setCheckbox(value);
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


function EntryRow (rowId, cbxId,cbxWrapperId) {
    this.onColor = "#FFFFCC";
    this.overColor = "#f5f5f5";
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

    this.mouseOver = function() {
        this.row.style.backgroundColor = this.overColor;
    }

    this.mouseOut = function() {
        this.setRowColor();
    }
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
            var idx1 = indexOf(checkBoxes, lastCbxClicked);
            var idx2 = indexOf(checkBoxes, cbx);
            if(idx1>idx2) {
                var tmp = idx1;
                idx1=idx2;
                idx2=tmp;
            }

            for(i=idx1;i<=idx2;i++) {
                checkBoxes[i].checked = value;
            }
        }
        return;
    }
    lastCbxClicked = cbx;
}








function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    var img = util.getDomObject(imgid);
    if(toggleVisibility(id)) {
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
        alert("no block " + uid);
	return;
    }
    var img = util.getDomObject("img_" +uid);
    if(!block.obj.isOpen) {
	originalImages[uid] = img.obj.src;
        block.obj.isOpen = 1;
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
            block.obj.innerHTML = html;
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



var selectors = new Array();

function Selector(event, id, allEntries, selecttype) {
    this.id = id;
    this.allEntries = allEntries;
    this.selecttype = selecttype;
    this.textComp = util.getDomObject(id);
    this.hiddenComp = util.getDomObject(id+".hidden");

    if (!this.textComp) {
        //        alert("cannot find text comp " + id);
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
    url = "${urlroot}/entry/show?output=selectxml&selecttype=" + this.selecttype+"&allentries=" + this.allEntries+"&target=" + id;
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
    }
    selectCancel();
}

function selectCancel() {
    var div = util.getDomObject('selectdiv');
    if(!div)return false;
    hideObject(div);
}



function selectInitialClick(event, id,allEntries,selecttype) {
    selectors[id] = new Selector(event,id,allEntries,selecttype);
    return false;
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



function toggleVisibility(id) {
    var obj = util.getDomObject(id);
    return toggleVisibilityOnObject(obj,'block');
}


function hide(id) {
    hideObject(util.getDomObject(id));
}


function showAjaxPopup(event,srcId,url) {
    util.loadXML( url, handleAjaxPopup,srcId);
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


function show(id) {
    showObject(util.getDomObject(id));
}

function hideObject(obj) {
    if(!obj) {
        return 0;
    }

    var style = obj.style;
    if(!style) {
        style = util.getStyle(obj);
    }
    if(!style) {
        //        alert("no style " + obj);

        return 0;
    }


    style.visibility = "hidden";
    style.display = "none";
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

    var style = obj.style;
    if(!style) {
        style = util.getStyle(obj);
    }
    if(!style) {
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
            title.style.borderBottom = "1px #dddddd solid";
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
