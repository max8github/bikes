package akka.sample.bikes

/**
 * Marker trait for serialization with Jackson CBOR. Currently (Mar 2020) unused.
 * Turn this back on and remove Java Serialization in `application.conf` once Cassandra is
 * upgraded to support typed actors and the newest version of Akka.
 */
trait CborSerializable
