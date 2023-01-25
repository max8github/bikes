Starting Cassandra on 10.1.0.12
CASSANDRA_CONF_DIR /etc/cassandra
CASSANDRA_CFG /etc/cassandra/cassandra.yaml
CASSANDRA_AUTO_BOOTSTRAP true
CASSANDRA_BROADCAST_ADDRESS 10.1.0.12
CASSANDRA_BROADCAST_RPC_ADDRESS 10.1.0.12
CASSANDRA_CLUSTER_NAME K8Demo
CASSANDRA_COMPACTION_THROUGHPUT_MB_PER_SEC
CASSANDRA_CONCURRENT_COMPACTORS
CASSANDRA_CONCURRENT_READS
CASSANDRA_CONCURRENT_WRITES
CASSANDRA_COUNTER_CACHE_SIZE_IN_MB
CASSANDRA_DC DC1-K8Demo
CASSANDRA_DISK_OPTIMIZATION_STRATEGY ssd
CASSANDRA_ENDPOINT_SNITCH SimpleSnitch
CASSANDRA_GC_WARN_THRESHOLD_IN_MS
CASSANDRA_INTERNODE_COMPRESSION
CASSANDRA_KEY_CACHE_SIZE_IN_MB
CASSANDRA_LISTEN_ADDRESS 10.1.0.12
CASSANDRA_LISTEN_INTERFACE
CASSANDRA_MEMTABLE_ALLOCATION_TYPE
CASSANDRA_MEMTABLE_CLEANUP_THRESHOLD
CASSANDRA_MEMTABLE_FLUSH_WRITERS
CASSANDRA_MIGRATION_WAIT 1
CASSANDRA_NUM_TOKENS 32
CASSANDRA_RACK Rack1-K8Demo
CASSANDRA_RING_DELAY 30000
CASSANDRA_RPC_ADDRESS 0.0.0.0
CASSANDRA_RPC_INTERFACE
CASSANDRA_SEEDS cassandra-0.cassandra.bikes-cluster-1.svc.cluster.local
CASSANDRA_SEED_PROVIDER org.apache.cassandra.locator.SimpleSeedProvider
changed ownership of '/etc/cassandra/cassandra-env.sh' from root to cassandra
changed ownership of '/etc/cassandra/jvm.options' from root to cassandra
changed ownership of '/etc/cassandra/cassandra.yaml' from root to cassandra
changed ownership of '/etc/cassandra/logback.xml' from root to cassandra
changed ownership of '/etc/cassandra/cassandra-rackdc.properties' from root to cassandra
changed ownership of '/etc/cassandra' from root to cassandra
OpenJDK 64-Bit Server VM warning: Cannot open file /usr/local/apache-cassandra-3.11.2/logs/gc.log due to No such file or directory

INFO  09:38:11 Configuration location: file:/etc/cassandra/cassandra.yaml
INFO  09:38:12 Node configuration:[allocate_tokens_for_keyspace=null; authenticator=AllowAllAuthenticator; authorizer=AllowAllAuthorizer; auto_bootstrap=true; auto_snapshot=true; back_pressure_enabled=false; back_pressure_strategy=null; batch_size_fail_threshold_in_kb=50; batch_size_warn_threshold_in_kb=5; batchlog_replay_throttle_in_kb=1024; broadcast_address=10.1.0.12; broadcast_rpc_address=10.1.0.12; buffer_pool_use_heap_if_exhausted=true; cas_contention_timeout_in_ms=1000; cdc_enabled=false; cdc_free_space_check_interval_ms=250; cdc_raw_directory=null; cdc_total_space_in_mb=0; client_encryption_options=<REDACTED>; cluster_name=K8Demo; column_index_cache_size_in_kb=2; column_index_size_in_kb=64; commit_failure_policy=stop; commitlog_compression=null; commitlog_directory=/cassandra_data/commitlog; commitlog_max_compression_buffers_in_pool=3; commitlog_periodic_queue_size=-1; commitlog_segment_size_in_mb=32; commitlog_sync=periodic; commitlog_sync_batch_window_in_ms=NaN; commitlog_sync_period_in_ms=10000; commitlog_total_space_in_mb=null; compaction_large_partition_warning_threshold_mb=100; compaction_throughput_mb_per_sec=16; concurrent_compactors=null; concurrent_counter_writes=32; concurrent_materialized_view_writes=32; concurrent_reads=32; concurrent_replicates=null; concurrent_writes=32; counter_cache_keys_to_save=2147483647; counter_cache_save_period=7200; counter_cache_size_in_mb=null; counter_write_request_timeout_in_ms=5000; credentials_cache_max_entries=1000; credentials_update_interval_in_ms=-1; credentials_validity_in_ms=2000; cross_node_timeout=false; data_file_directories=[Ljava.lang.String;@275710fc; disk_access_mode=auto; disk_failure_policy=stop; disk_optimization_estimate_percentile=0.95; disk_optimization_page_cross_chance=0.1; disk_optimization_strategy=ssd; dynamic_snitch=true; dynamic_snitch_badness_threshold=0.1; dynamic_snitch_reset_interval_in_ms=600000; dynamic_snitch_update_interval_in_ms=100; enable_materialized_views=true; enable_scripted_user_defined_functions=false; enable_user_defined_functions=false; enable_user_defined_functions_threads=true; encryption_options=null; endpoint_snitch=GossipingPropertyFileSnitch; file_cache_round_up=null; file_cache_size_in_mb=null; gc_log_threshold_in_ms=200; gc_warn_threshold_in_ms=1000; hinted_handoff_disabled_datacenters=[]; hinted_handoff_enabled=true; hinted_handoff_throttle_in_kb=1024; hints_compression=null; hints_directory=/cassandra_data/hints; hints_flush_period_in_ms=10000; incremental_backups=false; index_interval=null; index_summary_capacity_in_mb=null; index_summary_resize_interval_in_minutes=60; initial_token=null; inter_dc_stream_throughput_outbound_megabits_per_sec=200; inter_dc_tcp_nodelay=false; internode_authenticator=null; internode_compression=all; internode_recv_buff_size_in_bytes=0; internode_send_buff_size_in_bytes=0; key_cache_keys_to_save=2147483647; key_cache_save_period=14400; key_cache_size_in_mb=null; listen_address=10.1.0.12; listen_interface=null; listen_interface_prefer_ipv6=false; listen_on_broadcast_address=false; max_hint_window_in_ms=10800000; max_hints_delivery_threads=2; max_hints_file_size_in_mb=128; max_mutation_size_in_kb=null; max_streaming_retries=3; max_value_size_in_mb=256; memtable_allocation_type=heap_buffers; memtable_cleanup_threshold=null; memtable_flush_writers=0; memtable_heap_space_in_mb=null; memtable_offheap_space_in_mb=null; min_free_space_per_drive_in_mb=50; native_transport_max_concurrent_connections=-1; native_transport_max_concurrent_connections_per_ip=-1; native_transport_max_frame_size_in_mb=256; native_transport_max_threads=128; native_transport_port=9042; native_transport_port_ssl=null; num_tokens=32; otc_backlog_expiration_interval_ms=200; otc_coalescing_enough_coalesced_messages=8; otc_coalescing_strategy=DISABLED; otc_coalescing_window_us=200; partitioner=org.apache.cassandra.dht.Murmur3Partitioner; permissions_cache_max_entries=1000; permissions_update_interval_in_ms=-1; permissions_validity_in_ms=2000; phi_convict_threshold=8.0; prepared_statements_cache_size_mb=null; range_request_timeout_in_ms=10000; read_request_timeout_in_ms=5000; request_scheduler=org.apache.cassandra.scheduler.NoScheduler; request_scheduler_id=null; request_scheduler_options=null; request_timeout_in_ms=10000; role_manager=CassandraRoleManager; roles_cache_max_entries=1000; roles_update_interval_in_ms=-1; roles_validity_in_ms=2000; row_cache_class_name=org.apache.cassandra.cache.OHCProvider; row_cache_keys_to_save=2147483647; row_cache_save_period=0; row_cache_size_in_mb=0; rpc_address=0.0.0.0; rpc_interface=null; rpc_interface_prefer_ipv6=false; rpc_keepalive=true; rpc_listen_backlog=50; rpc_max_threads=2147483647; rpc_min_threads=16; rpc_port=9160; rpc_recv_buff_size_in_bytes=null; rpc_send_buff_size_in_bytes=null; rpc_server_type=sync; saved_caches_directory=/cassandra_data/saved_caches; seed_provider=org.apache.cassandra.locator.SimpleSeedProvider{seeds=cassandra-0.cassandra.bikes-cluster-1.svc.cluster.local}; server_encryption_options=<REDACTED>; slow_query_log_timeout_in_ms=500; snapshot_before_compaction=false; ssl_storage_port=7001; sstable_preemptive_open_interval_in_mb=50; start_native_transport=true; start_rpc=false; storage_port=7000; stream_throughput_outbound_megabits_per_sec=200; streaming_keep_alive_period_in_secs=300; streaming_socket_timeout_in_ms=86400000; thrift_framed_transport_size_in_mb=15; thrift_max_message_length_in_mb=16; thrift_prepared_statements_cache_size_mb=null; tombstone_failure_threshold=100000; tombstone_warn_threshold=1000; tracetype_query_ttl=86400; tracetype_repair_ttl=604800; transparent_data_encryption_options=org.apache.cassandra.config.TransparentDataEncryptionOptions@525f1e4e; trickle_fsync=false; trickle_fsync_interval_in_kb=10240; truncate_request_timeout_in_ms=60000; unlogged_batch_across_partitions_warn_threshold=10; user_defined_function_fail_timeout=1500; user_defined_function_warn_timeout=500; user_function_timeout_policy=die; windows_timer_interval=1; write_request_timeout_in_ms=2000]
INFO  09:38:12 DiskAccessMode 'auto' determined to be mmap, indexAccessMode is mmap
INFO  09:38:12 Global memtable on-heap threshold is enabled at 128MB
INFO  09:38:12 Global memtable off-heap threshold is enabled at 128MB
WARN  09:38:12 Only 57.986GiB free across all data volumes. Consider adding more capacity to your cluster or removing obsolete snapshots
INFO  09:38:12 Initialized back-pressure with high ratio: 0.9, factor: 5, flow: FAST, window size: 2000.
INFO  09:38:12 Back-pressure is disabled with strategy null.
INFO  09:38:12 Unable to load cassandra-topology.properties; compatibility mode disabled
INFO  09:38:13 Overriding RING_DELAY to 30000ms
INFO  09:38:13 Configured JMX server at: service:jmx:rmi://127.0.0.1/jndi/rmi://127.0.0.1:7199/jmxrmi
INFO  09:38:13 Hostname: cassandra-0.cassandra.bikes-cluster-1.svc.cluster.local
INFO  09:38:13 JVM vendor/version: OpenJDK 64-Bit Server VM/1.8.0_151
INFO  09:38:13 Heap size: 512.000MiB/512.000MiB
INFO  09:38:13 Code Cache Non-heap memory: init = 2555904(2496K) used = 4163648(4066K) committed = 4194304(4096K) max = 251658240(245760K)
INFO  09:38:13 Metaspace Non-heap memory: init = 0(0K) used = 17541872(17130K) committed = 17956864(17536K) max = -1(-1K)
INFO  09:38:13 Compressed Class Space Non-heap memory: init = 0(0K) used = 2117896(2068K) committed = 2228224(2176K) max = 1073741824(1048576K)
INFO  09:38:13 G1 Eden Space Heap memory: init = 28311552(27648K) used = 26214400(25600K) committed = 37748736(36864K) max = -1(-1K)
INFO  09:38:13 G1 Survivor Space Heap memory: init = 0(0K) used = 4194304(4096K) committed = 4194304(4096K) max = -1(-1K)
INFO  09:38:13 G1 Old Gen Heap memory: init = 508559360(496640K) used = 1990144(1943K) committed = 508559360(496640K) max = 536870912(524288K)
INFO  09:38:13 Classpath: /etc/cassandra:/usr/local/apache-cassandra-3.11.2/build/classes/main:/usr/local/apache-cassandra-3.11.2/build/classes/thrift:/usr/local/apache-cassandra-3.11.2/lib/HdrHistogram-2.1.9.jar:/usr/local/apache-cassandra-3.11.2/lib/ST4-4.0.8.jar:/usr/local/apache-cassandra-3.11.2/lib/airline-0.6.jar:/usr/local/apache-cassandra-3.11.2/lib/antlr-runtime-3.5.2.jar:/usr/local/apache-cassandra-3.11.2/lib/apache-cassandra-3.11.2.jar:/usr/local/apache-cassandra-3.11.2/lib/apache-cassandra-thrift-3.11.2.jar:/usr/local/apache-cassandra-3.11.2/lib/asm-5.0.4.jar:/usr/local/apache-cassandra-3.11.2/lib/caffeine-2.2.6.jar:/usr/local/apache-cassandra-3.11.2/lib/cassandra-driver-core-3.0.1-shaded.jar:/usr/local/apache-cassandra-3.11.2/lib/commons-cli-1.1.jar:/usr/local/apache-cassandra-3.11.2/lib/commons-codec-1.9.jar:/usr/local/apache-cassandra-3.11.2/lib/commons-lang3-3.1.jar:/usr/local/apache-cassandra-3.11.2/lib/commons-math3-3.2.jar:/usr/local/apache-cassandra-3.11.2/lib/compress-lzf-0.8.4.jar:/usr/local/apache-cassandra-3.11.2/lib/concurrent-trees-2.4.0.jar:/usr/local/apache-cassandra-3.11.2/lib/concurrentlinkedhashmap-lru-1.4.jar:/usr/local/apache-cassandra-3.11.2/lib/disruptor-3.0.1.jar:/usr/local/apache-cassandra-3.11.2/lib/ecj-4.4.2.jar:/usr/local/apache-cassandra-3.11.2/lib/guava-18.0.jar:/usr/local/apache-cassandra-3.11.2/lib/high-scale-lib-1.0.6.jar:/usr/local/apache-cassandra-3.11.2/lib/hppc-0.5.4.jar:/usr/local/apache-cassandra-3.11.2/lib/jackson-core-asl-1.9.13.jar:/usr/local/apache-cassandra-3.11.2/lib/jackson-mapper-asl-1.9.13.jar:/usr/local/apache-cassandra-3.11.2/lib/jamm-0.3.0.jar:/usr/local/apache-cassandra-3.11.2/lib/javax.inject.jar:/usr/local/apache-cassandra-3.11.2/lib/jbcrypt-0.3m.jar:/usr/local/apache-cassandra-3.11.2/lib/jcl-over-slf4j-1.7.7.jar:/usr/local/apache-cassandra-3.11.2/lib/jctools-core-1.2.1.jar:/usr/local/apache-cassandra-3.11.2/lib/jflex-1.6.0.jar:/usr/local/apache-cassandra-3.11.2/lib/jna-4.2.2.jar:/usr/local/apache-cassandra-3.11.2/lib/joda-time-2.4.jar:/usr/local/apache-cassandra-3.11.2/lib/json-simple-1.1.jar:/usr/local/apache-cassandra-3.11.2/lib/jstackjunit-0.0.1.jar:/usr/local/apache-cassandra-3.11.2/lib/libthrift-0.9.2.jar:/usr/local/apache-cassandra-3.11.2/lib/log4j-over-slf4j-1.7.7.jar:/usr/local/apache-cassandra-3.11.2/lib/logback-classic-1.1.3.jar:/usr/local/apache-cassandra-3.11.2/lib/logback-core-1.1.3.jar:/usr/local/apache-cassandra-3.11.2/lib/lz4-1.3.0.jar:/usr/local/apache-cassandra-3.11.2/lib/metrics-core-3.1.0.jar:/usr/local/apache-cassandra-3.11.2/lib/metrics-jvm-3.1.0.jar:/usr/local/apache-cassandra-3.11.2/lib/metrics-logback-3.1.0.jar:/usr/local/apache-cassandra-3.11.2/lib/netty-all-4.0.44.Final.jar:/usr/local/apache-cassandra-3.11.2/lib/ohc-core-0.4.4.jar:/usr/local/apache-cassandra-3.11.2/lib/ohc-core-j8-0.4.4.jar:/usr/local/apache-cassandra-3.11.2/lib/reporter-config-base-3.0.3.jar:/usr/local/apache-cassandra-3.11.2/lib/reporter-config3-3.0.3.jar:/usr/local/apache-cassandra-3.11.2/lib/sigar-1.6.4.jar:/usr/local/apache-cassandra-3.11.2/lib/slf4j-api-1.7.7.jar:/usr/local/apache-cassandra-3.11.2/lib/snakeyaml-1.11.jar:/usr/local/apache-cassandra-3.11.2/lib/snappy-java-1.1.1.7.jar:/usr/local/apache-cassandra-3.11.2/lib/snowball-stemmer-1.3.0.581.1.jar:/usr/local/apache-cassandra-3.11.2/lib/stream-2.5.2.jar:/usr/local/apache-cassandra-3.11.2/lib/thrift-server-0.3.7.jar:/usr/local/apache-cassandra-3.11.2/lib/jsr223/*/*.jar:/usr/local/apache-cassandra-3.11.2/lib/jamm-0.3.0.jar
INFO  09:38:13 JVM Arguments: [-Xloggc:/usr/local/apache-cassandra-3.11.2/logs/gc.log, -ea, -XX:+UseThreadPriorities, -XX:ThreadPriorityPolicy=42, -XX:+HeapDumpOnOutOfMemoryError, -Xss256k, -XX:StringTableSize=1000003, -XX:+AlwaysPreTouch, -XX:-UseBiasedLocking, -XX:+UseTLAB, -XX:+ResizeTLAB, -XX:+PerfDisableSharedMem, -Djava.net.preferIPv4Stack=true, -XX:+UseG1GC, -XX:G1RSetUpdatingPauseTimePercent=5, -XX:+PrintGCDetails, -XX:+PrintGCDateStamps, -XX:+PrintHeapAtGC, -XX:+PrintTenuringDistribution, -XX:+PrintGCApplicationStoppedTime, -XX:+PrintPromotionFailure, -XX:+UseGCLogFileRotation, -XX:NumberOfGCLogFiles=10, -XX:GCLogFileSize=10M, -Dcassandra.migration_task_wait_in_seconds=1, -Dcassandra.ring_delay_ms=30000, -Xms512M, -Xmx512M, -XX:CompileCommandFile=/etc/cassandra/hotspot_compiler, -javaagent:/usr/local/apache-cassandra-3.11.2/lib/jamm-0.3.0.jar, -Dcassandra.jmx.local.port=7199, -Dcom.sun.management.jmxremote.authenticate=false, -Dcom.sun.management.jmxremote.password.file=/etc/cassandra/jmxremote.password, -Djava.library.path=/usr/local/apache-cassandra-3.11.2/lib/sigar-bin, -Djava.rmi.server.hostname=10.1.0.12, -Dcassandra.libjemalloc=/usr/lib/x86_64-linux-gnu/libjemalloc.so.1, -XX:OnOutOfMemoryError=kill -9 %p, -Dlogback.configurationFile=logback.xml, -Dcassandra.logdir=/usr/local/apache-cassandra-3.11.2/logs, -Dcassandra.storagedir=/usr/local/apache-cassandra-3.11.2/data, -Dcassandra-foreground=yes]
WARN  09:38:14 Unable to lock JVM memory (ENOMEM). This can result in part of the JVM being swapped out, especially with mmapped I/O enabled. Increase RLIMIT_MEMLOCK or run Cassandra as root.
INFO  09:38:14 jemalloc seems to be preloaded from /usr/lib/x86_64-linux-gnu/libjemalloc.so.1
WARN  09:38:14 JMX is not enabled to receive remote connections. Please see cassandra-env.sh for more info.
INFO  09:38:14 Initializing SIGAR library
WARN  09:38:14 Cassandra server running in degraded mode. Is swap disabled? : false,  Address space adequate? : true,  nofile limit adequate? : true, nproc limit adequate? : true 
WARN  09:38:14 Maximum number of memory map areas per process (vm.max_map_count) 262144 is too low, recommended value: 1048575, you can change it with sysctl.
INFO  09:38:14 Initialized prepared statement caches with 10 MB (native) and 10 MB (Thrift)
INFO  09:38:16 Initializing system.IndexInfo
INFO  09:38:20 Initializing system.batches
INFO  09:38:20 Initializing system.paxos
INFO  09:38:20 Initializing system.local
INFO  09:38:20 Global buffer pool is enabled, when pool is exhausted (max is 128.000MiB) it will allocate on heap
INFO  09:38:22 Initializing key cache with capacity of 25 MBs.
INFO  09:38:22 Initializing row cache with capacity of 0 MBs
INFO  09:38:22 Initializing counter cache with capacity of 12 MBs
INFO  09:38:22 Scheduling counter cache save to every 7200 seconds (going to save all keys).
INFO  09:38:22 Initializing system.peers
INFO  09:38:22 Initializing system.peer_events
INFO  09:38:22 Initializing system.range_xfers
INFO  09:38:22 Initializing system.compaction_history
INFO  09:38:22 Initializing system.sstable_activity
INFO  09:38:22 Initializing system.size_estimates
INFO  09:38:22 Initializing system.available_ranges
INFO  09:38:22 Initializing system.transferred_ranges
INFO  09:38:22 Initializing system.views_builds_in_progress
INFO  09:38:22 Initializing system.built_views
INFO  09:38:22 Initializing system.hints
INFO  09:38:22 Initializing system.batchlog
INFO  09:38:22 Initializing system.prepared_statements
INFO  09:38:22 Initializing system.schema_keyspaces
INFO  09:38:22 Initializing system.schema_columnfamilies
INFO  09:38:22 Initializing system.schema_columns
INFO  09:38:22 Initializing system.schema_triggers
INFO  09:38:22 Initializing system.schema_usertypes
INFO  09:38:23 Initializing system.schema_functions
