CREATE
KEYSPACE IF NOT EXISTS akka_snapshot
WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

CREATE TABLE IF NOT EXISTS akka_snapshot.snapshots
(
    persistence_id    text,
    sequence_nr       bigint,
    timestamp         bigint,
    ser_id            int,
    ser_manifest      text,
    snapshot_data     blob,
    snapshot          blob,
    meta_ser_id       int,
    meta_ser_manifest text,
    meta              blob,
    PRIMARY KEY (persistence_id, sequence_nr)
)
WITH CLUSTERING ORDER BY (sequence_nr DESC) AND gc_grace_seconds =864000
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