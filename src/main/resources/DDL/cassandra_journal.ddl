CREATE KEYSPACE IF NOT EXISTS akka WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

CREATE TABLE IF NOT EXISTS akka.messages (
                                             persistence_id text,
                                             partition_nr bigint,
                                             sequence_nr bigint,
                                             timestamp timeuuid,
                                             timebucket text,
                                             writer_uuid text,
                                             ser_id int,
                                             ser_manifest text,
                                             event_manifest text,
                                             event blob,
                                             meta_ser_id int,
                                             meta_ser_manifest text,
                                             meta blob,
                                             tags set<text>,
                                             PRIMARY KEY ((persistence_id, partition_nr), sequence_nr, timestamp))
    WITH gc_grace_seconds =864000
        AND compaction = {
        'class' : 'SizeTieredCompactionStrategy',
        'enabled' : true,
        'tombstone_compaction_interval' : 86400,
        'tombstone_threshold' : 0.2,
        'unchecked_tombstone_compaction' : false,
        'bucket_high' : 1.5,
        'bucket_low' : 0.5,
        'max_threshold' : 32,
        'min_threshold' : 4,
        'min_sstable_size' : 50
        };

CREATE TABLE IF NOT EXISTS akka.tag_views (
                                              tag_name text,
                                              persistence_id text,
                                              sequence_nr bigint,
                                              timebucket bigint,
                                              timestamp timeuuid,
                                              tag_pid_sequence_nr bigint,
                                              writer_uuid text,
                                              ser_id int,
                                              ser_manifest text,
                                              event_manifest text,
                                              event blob,
                                              meta_ser_id int,
                                              meta_ser_manifest text,
                                              meta blob,
                                              PRIMARY KEY ((tag_name, timebucket), timestamp, persistence_id, tag_pid_sequence_nr))
    WITH gc_grace_seconds =864000
        AND compaction = {
        'class' : 'SizeTieredCompactionStrategy',
        'enabled' : true,
        'tombstone_compaction_interval' : 86400,
        'tombstone_threshold' : 0.2,
        'unchecked_tombstone_compaction' : false,
        'bucket_high' : 1.5,
        'bucket_low' : 0.5,
        'max_threshold' : 32,
        'min_threshold' : 4,
        'min_sstable_size' : 50
        };

CREATE TABLE IF NOT EXISTS akka.tag_write_progress(
                                                      persistence_id text,
                                                      tag text,
                                                      sequence_nr bigint,
                                                      tag_pid_sequence_nr bigint,
                                                      offset timeuuid,
                                                      PRIMARY KEY (persistence_id, tag));

CREATE TABLE IF NOT EXISTS akka.tag_scanning(
                                                persistence_id text,
                                                sequence_nr bigint,
                                                PRIMARY KEY (persistence_id));

CREATE TABLE IF NOT EXISTS akka.metadata(
                                            persistence_id text PRIMARY KEY,
                                            deleted_to bigint,
                                            properties map<text,text>);

CREATE TABLE IF NOT EXISTS akka.all_persistence_ids(
    persistence_id text PRIMARY KEY);