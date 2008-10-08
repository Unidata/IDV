

-----------------------------------------------------------------------
---Note: the ramadda.datetime and ramadda.double are replaced by ramadda
---with the appropriate datatype for the database being used.
---mysql has a datetime type, postgres and derby have timestamp
---we can't use timestamp for mysql because it only goes back to 1970
---derby and mysql have double. postgres has float8
-----------------------------------------------------------------------


-----------------------------------------------------------------------
--- the main entries table
-----------------------------------------------------------------------
CREATE TABLE entries (id varchar(200),
                   type varchar(200),
	           name varchar(200),
                   description varchar(10000),
                   parent_group_id varchar(200),
                   top_group_id varchar(200),
   		   user_id varchar(200),
	           resource varchar(500),	           
                   resource_type varchar(200),
		   datatype varchar(200),
	           createdate ramadda.datetime, 
	           fromdate ramadda.datetime, 
	           todate ramadda.datetime,
	           south ramadda.double,
	           north ramadda.double,
	           east ramadda.double,
	           west ramadda.double); 


--- for mysql
alter table entries modify column resource varchar(500);
alter table entries modify column createdate ramadda.datetime;
alter table entries modify column fromdate ramadda.datetime;
alter table entries modify column todate ramadda.datetime;
--- for derby
alter table entries alter column resource set data type varchar(500);
alter table entries alter column createdate set data type ramadda.datetime;
alter table entries alter column fromdate set data type ramadda.datetime;
alter table entries alter column todate set data type ramadda.datetime;



CREATE INDEX ENTRIES_INDEX_ID ON entries (ID);
CREATE INDEX ENTRIES_INDEX_RESOURCE ON entries (RESOURCE);
CREATE INDEX ENTRIES_INDEX_DATATYPE ON entries (DATATYPE);
CREATE INDEX ENTRIES_INDEX_PARENT_GROUP_ID ON entries (PARENT_GROUP_ID);
CREATE INDEX ENTRIES_INDEX_TOP_GROUP_ID ON entries (TOP_GROUP_ID);
CREATE INDEX ENTRIES_INDEX_TYPE ON entries (TYPE);
CREATE INDEX ENTRIES_INDEX_USER_ID ON entries (USER_ID);
CREATE INDEX ENTRIES_INDEX_FROMDATE ON entries (FROMDATE);
CREATE INDEX ENTRIES_INDEX_TODATE ON entries (TODATE);


-----------------------------------------------------------------------
---Holds metadata 
---Entries can have any number of metadata items
---The MetadataHandler classes handle the semantics. 
-----------------------------------------------------------------------
CREATE TABLE  metadata (id varchar(200),
			entry_id varchar(200),
                        type varchar(200),
                	inherited int,
                        attr1 varchar(10000),
                        attr2 varchar(10000),
                        attr3 varchar(10000),
                        attr4 varchar(10000)
			);


CREATE INDEX METADATA_INDEX_ID ON metadata (ID);
CREATE INDEX METADATA_INDEX_ENTRYID ON metadata (ENTRY_ID);
CREATE INDEX METADATA_INDEX_TYPE ON metadata (TYPE);
CREATE INDEX METADATA_INDEX_ATTR1 ON metadata (ATTR1);

-----------------------------------------------------------------------
--- comments 
-----------------------------------------------------------------------
CREATE TABLE  comments (id varchar(200),
		        entry_id varchar(200),
			user_id  varchar(200),
                        date ramadda.datetime, 
			subject  varchar(200),
                        comment varchar(1000));

CREATE INDEX COMMENTS_INDEX_ID ON comments (ID);
CREATE INDEX COMMENTS_INDEX_ENTRY_ID ON comments (ENTRY_ID);


-----------------------------------------------------------------------
--- associations 
-----------------------------------------------------------------------
CREATE TABLE associations (id varchar(200),
                           name varchar(200),
		           type varchar(200),
			   from_entry_id varchar(200),
		           to_entry_id varchar(200));


-----------------------------------------------------------------------
--- users 
-----------------------------------------------------------------------
CREATE TABLE  users (id varchar(200),
                     name  varchar(200),
                     email varchar(200),
                     question  varchar(200),
                     answer  varchar(200),  
                     password  varchar(200),
		     admin int,
		     language varchar(50));




-----------------------------------------------------------------------
--- roles users have
-----------------------------------------------------------------------
CREATE TABLE  userroles (
        user_id varchar(200),
        role varchar(200));


-----------------------------------------------------------------------
---  permissions on entries
-----------------------------------------------------------------------
CREATE TABLE  permissions (
	entry_id varchar(200),
	action varchar(200),
        role varchar(200));




-----------------------------------------------------------------------
--- the harvesters. content is the xml they encode/decode to store state
-----------------------------------------------------------------------
CREATE TABLE  harvesters (
       	      id varchar(200),
              class varchar(500),
              content varchar(10000));




-----------------------------------------------------------------------
--- global properties
-----------------------------------------------------------------------

CREATE TABLE  globals (name varchar(500),
                       value varchar(10000));



-----------------------------------------------------------------------
--- just here so ramadda knows if the db has been created
-----------------------------------------------------------------------
CREATE TABLE  dummy (name varchar(500));


---CREATE TABLE stats ();