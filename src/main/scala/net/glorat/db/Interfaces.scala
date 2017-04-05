package net.glorat.db

import CQRS._
import org.joda.time.Instant

case class UpsertOpts (checkUniqueKeyConflict:Boolean = true)

case class Upsert(ent: MyValue, opts:UpsertOpts = UpsertOpts()) extends Command
case class Put(ent: MyValue) extends Command
case class Invalidate(ent: MyValue) extends Command

case class Defined(id: GUID, key: MyKey) extends DomainEvent
case class Upserted(id: GUID, versionedId: GUID, ent: MyValue, transactionTime: Instant) extends DomainEvent
case class Putted(id: GUID, versionedId: GUID, ent: MyValue) extends DomainEvent
case class Invalidated(id: GUID) extends DomainEvent


trait MyKey {
  def toBytes : Array[Byte]
  // TODO: Choose a more sensible ns?
  private val ns = java.util.UUID.fromString("9d9814f5-f531-4d80-8722-f61dcc1679b8")

  // Serializes to something unique, let's say
  def toUniqueId: GUID = {
    UUIDType5.nameUUIDFromNamespaceAndBytes(ns, toBytes)
  }
}

case class MyIndex(val indexName :String, val indexValue: String) // indexType

trait MyValue {
  def key: MyKey
  def entityType: String = this.getClass.getName // Simple for now
  def transactionTime: Instant // one way of getting a monotonically increasing transaction id

  def internalUniqueId = s"${entityType}:{$key.toUniqueId}"

  /** Map from key name to (sortable!?) string reprentation of key-value */
  def uniqueKeys : Seq[MyIndex] = Seq()
}

// For things we store that have a granular bi-temporality we wish to query against
trait GranularValidTime {
  def validTime: Instant
}
