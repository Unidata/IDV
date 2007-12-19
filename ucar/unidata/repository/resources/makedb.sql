
--- Initial data base creation


CREATE TABLE  groups (id varchar(500),
                     parent varchar(200),
                     name varchar(200),
		     description varchar(200));

CREATE INDEX GROUPS_INDEX_ID ON groups (ID);



CREATE TABLE  metadata (id varchar(500),
                        id_type varchar(100),
                        type varchar(200),
                        name varchar(200),
                        content varchar(1000));

CREATE INDEX METADATA_INDEX_ID ON metadata (ID);
CREATE INDEX METADATA_INDEX_ID_TYPE ON metadata (ID_TYPE);
CREATE INDEX METADATA_INDEX_TYPE ON metadata (TYPE);
	



CREATE TABLE tags (name varchar(200),
	           entry_id varchar(200));

CREATE TABLE associations (name varchar(200),
			   from_entry_id varchar(200),
		           to_entry_id varchar(200));


CREATE INDEX TAGS_INDEX_NAME ON tags (NAME);
CREATE INDEX TAGS_INDEX_ENTRY_ID ON tags (ENTRY_ID);


CREATE TABLE  users (id varchar(200),
                     name  varchar(200),
		     admin int);


CREATE TABLE entries (id varchar(200),
                   type varchar(200),
	           name varchar(200),
                   description varchar(1000),
                   group_id varchar(200),
   		   user_id varchar(200),
	           resource varchar(200),
	           createdate timestamp, 
	           fromdate timestamp, 
	           todate timestamp,
	           minlat double,
	           maxlat double,
	           minlon double,
	           maxlon double); 

CREATE INDEX ENTRIES_INDEX_ID ON entries (ID);
CREATE INDEX ENTRIES_INDEX_RESOURCE ON entries (RESOURCE);
CREATE INDEX ENTRIES_INDEX_GROUP ON entries (GROUP_ID);
CREATE INDEX ENTRIES_INDEX_TYPE ON entries (TYPE);
CREATE INDEX ENTRIES_INDEX_USER_ID ON entries (USER_ID);

