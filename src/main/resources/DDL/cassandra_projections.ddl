CREATE KEYSPACE IF NOT EXISTS bike_projection WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

CREATE TABLE IF NOT EXISTS bike_projection.offset_store (
                                                            projection_name text,
                                                            partition int,
                                                            projection_key text,
                                                            offset text,
                                                            manifest text,
                                                            last_updated timestamp,
                                                            PRIMARY KEY ((projection_name, partition), projection_key));

CREATE TABLE IF NOT EXISTS bike_projection.projection_management (
                                                                     projection_name text,
                                                                     partition int,
                                                                     projection_key text,
                                                                     paused boolean,
                                                                     last_updated timestamp,
                                                                     PRIMARY KEY ((projection_name, partition), projection_key));