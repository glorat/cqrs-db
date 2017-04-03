package net.glorat.db

import CQRS.EventStreamReceiver
import eventstore.CommitedEvent
import org.joda.time.Instant
import slick.jdbc.H2Profile.api._

import scala.concurrent.ExecutionContext.Implicits.global

object SampleDatabase {
  /** An immediately available but stale and non-RT view of what is latest */
  // ValueId -> VersionedId
  var latest : Map[GUID, GUID] = Map()
  /** The current replica tt of the latest projection */
  def latestTransactionTime : Instant = NosqlLatestView.nowTt
  // var list = List[InventoryItemListDto]()


}

/**
  * Represents a blob stores of any state changes. A persistent kv store is a good choice!
  */
object NosqlBlobStore extends EventStreamReceiver {
  // VersionedId -> Value
  var blobStore: Map[GUID, MyValue] = Map()
  var nowTt : Instant = new Instant(0)

  def handle(ce: CommitedEvent): Unit = {
    ce.event match {
      case a: Upserted => handle(a, ce.streamRevision)
      case _ => ()
    }
  }

  private def handle(message: Upserted, version: Int) = {
    blobStore = blobStore + (message.versionedId -> message.ent)
    nowTt = message.transactionTime
  }
}

object NosqlLatestView extends EventStreamReceiver //: Handles<InventoryItemCreated>, Handles<InventoryItemRenamed>, Handles<InventoryItemDeactivated>
{
  var nowTt : Instant = new Instant(0)

  def handle(ce: CommitedEvent): Unit = {
    ce.event match {
      case a: Upserted => handle(a, ce.streamRevision)
      case _ => ()
    }
  }

  private def handle(message: Upserted, version: Int) = {
    SampleDatabase.latest = SampleDatabase.latest + (message.ent.key.toUniqueId -> message.versionedId)

    nowTt = message.transactionTime
  }
}

object NosqlLatestIndex extends EventStreamReceiver //: Handles<InventoryItemCreated>, Handles<InventoryItemRenamed>, Handles<InventoryItemDeactivated>
{
  // entityType -> indexName -> indexValue -> MyValue
  val indices : Map[String, Map[String, Map[String, MyValue]]] = Map()

  def handle(ce: CommitedEvent): Unit = {
    ce.event match {
      case a: Upserted => handle(a, ce.streamRevision)
      case _ => ()
    }
  }

  private def handle(message: Upserted, version: Int) = {
    val myValue = message.ent
    val entityType = myValue.entityType
    myValue.uniqueKeys.foreach( idx => {
      updateIndex(entityType, idx.indexName, idx.indexValue, myValue)
    })
    SampleDatabase.latest = SampleDatabase.latest + (message.ent.key.toUniqueId -> message.versionedId)
  }

  private def updateIndex(entityType:String, indexName:String, indexValue:String, value:MyValue) :  Unit = {
    ???
  }
}
