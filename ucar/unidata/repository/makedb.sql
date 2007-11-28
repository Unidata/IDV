

CREATE TABLE groups (id varchar(200),
	  		parent varchar(200),
			name varchar(200),
			description varchar(200));


CREATE TABLE files (id varchar(200),
                   type varchar(200),
                   group_id varchar(200),
	           file varchar(200),
	           fromdate timestamp, 
	           todate timestamp); 

CREATE INDEX GROUPINDEX ON files (GROUP_ID);

CREATE TABLE level3radar (
	           id varchar(200),
                   station varchar(50), 
                   product varchar(50));


CREATE TABLE level2radar (
	           id varchar(200),
                   station varchar(50));



CREATE INDEX STATIONINDEX ON level3radar (STATION);

CREATE INDEX PRODUCTINDEX ON level3radar (PRODUCT);

