
--- Initial data base creation


CREATE TABLE  groups (id varchar(500),
                     parent varchar(200),
                     name varchar(200),
		     description varchar(200));

CREATE INDEX GROUPS_INDEX_ID ON groups (ID);



CREATE TABLE tags (name varchar(200),
	           file_id varchar(200));

CREATE INDEX TAGS_INDEX_NAME ON tags (NAME);
CREATE INDEX TAGS_INDEX_FILE_ID ON tags (FILE_ID);


CREATE TABLE  users (id varchar(200),
                     name  varchar(200),
		     admin int);


CREATE TABLE entries (id varchar(200),
	           name varchar(200),
                   description varchar(500),
                   type varchar(200),
                   group_id varchar(200),
   		   user_id varchar(200),
	           file varchar(200),
	           createdate timestamp, 
	           fromdate timestamp, 
	           todate timestamp); 

CREATE INDEX ENTRIES_INDEX_ID ON entries (ID);
CREATE INDEX ENTRIES_INDEX_GROUP ON entries (GROUP_ID);
CREATE INDEX ENTRIES_INDEX_TYPE ON entries (TYPE);
CREATE INDEX ENTRIES_INDEX_USER_ID ON entries (USER_ID);

CREATE TABLE level3radar (
	           id varchar(200),
                   station varchar(50), 
                   product varchar(50));

CREATE INDEX LEVEL3RADAR_INDEX_ID ON level3radar (ID);
CREATE INDEX LEVEL3RADAR_INDEX_STATION ON level3radar (STATION);
CREATE INDEX LEVEL3RADAR_INDEX_PRODUCT ON level3radar (PRODUCT);


CREATE TABLE level2radar (
	           id varchar(200),
                   station varchar(50));

CREATE INDEX LEVEL2RADAR_INDEX_ID ON level2radar (ID);
CREATE INDEX LEVEL2RADAR_INDEX_STATION ON level2radar (STATION);


CREATE TABLE satellite (
	           id varchar(200),
                   platform varchar(100), 
                   resolution varchar(50),
                   product varchar(50));


CREATE INDEX SATELLITE_INDEX_ID ON satellite (ID);
CREATE INDEX SATELLITE_INDEX_STATION ON satellite (STATION);
CREATE INDEX SATELLITE_INDEX_PRODUCT ON satellite (PRODUCT);



CREATE TABLE model (
	           id varchar(200),
                   modelgroup varchar(50),
                   modelrun varchar(50));



CREATE INDEX MODEL_INDEX_ID  ON model (ID);
CREATE INDEX MODEL_INDEX_MODELGROUP ON model (MODELGROUP);




