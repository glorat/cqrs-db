package net.glorat.db

import CQRS.EventStreamReceiver
import eventstore.CommitedEvent
import org.joda.time.Instant
import slick.jdbc.H2Profile
import slick.jdbc.meta.MTable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import slick.jdbc.H2Profile.api._

class IndexEntries(tag: Tag) extends Table[(String, String, String, GUID, GUID)](tag, "LATEST_INDEX") {
  def entityType = column[String]("ENTITY_TYPE")
  def indexName = column[String]("INDEX_NAME")
  def indexValue = column[String]("INDEX_VALUE")
  def valueId = column[GUID]("VALUE_ID")
  def versionedId = column[GUID]("VERSIONED_ID")

  def * = (entityType, indexName, indexValue, valueId, versionedId)

  def pk = primaryKey("pk_a", (entityType, indexName, indexValue))
}

/**
  * Represents a blob stores of any state changes. A persistent kv store is a good choice!
  */
class NosqlBlobStore(dbs : Stores) extends EventStreamReceiver {
  // VersionedId -> Value
  //var blobStore: Map[GUID, MyValue] = Map()
  var nowTt : Instant = new Instant(0)

  def handle(ce: CommitedEvent): Future[Unit] = {
    ce.event match {
      case a: Upserted => handle(a, ce.streamRevision)
      case _ => ()
    }
    Future.successful()
  }

  private def handle(message: Upserted, version: Int) = {
    dbs.blobStore = dbs.blobStore + (message.versionedId -> message.ent)
    nowTt = message.transactionTime
  }
}

class NosqlLatestView(dbs:Stores) extends EventStreamReceiver
{
  var nowTt : Instant = new Instant(0)

  def handle(ce: CommitedEvent): Future[Unit] = {

    ce.event match {
      case a: Upserted => handle(a, ce.streamRevision)
      case _ => ()
    }
    Future.successful()
  }

  private def handle(message: Upserted, version: Int) = {
    dbs.latestView = dbs.latestView + (message.ent.key.toUniqueId -> message.versionedId)

    nowTt = message.transactionTime
  }
}

class NosqlLatestIndex(readDatabase: Stores) extends EventStreamReceiver
{
  import slick.jdbc.H2Profile.api._
  //var badEvents : Seq[CommitedEvent] = Seq()
  var badEventCount:Int = 0

  val db = readDatabase.hdb
  val indexEntries = readDatabase.indexEntries

  def handle(ce: CommitedEvent): Future[Unit] = {
    ce.event match {
      case a: Upserted => handle(a, ce.streamRevision)
      case _ => ()
    }
    Future.successful()
  }

  private def handle(message: Upserted, version: Int) = {
    val myValue = message.ent
    val entityType = myValue.entityType
    myValue.uniqueKeys.foreach( idx => {
      val f = updateIndex(entityType, idx.indexName, idx.indexValue, myValue.key.toUniqueId, message.versionedId)

      val res = Await.result(f, Duration.Inf)


    })
  }

  private def updateIndex(entityType:String, indexName:String, indexValue:String, valueId: GUID, versionedId: GUID) :  Future[Unit] = {

    require(Await.result(db.run(MTable.getTables), Duration.Inf).toList.length == 1)

    val q = indexEntries.filter(idx => idx.entityType === entityType && idx.indexName === indexName && idx.indexValue === indexValue)
    val previous = q.to[Set].result

    def doUpdate(res: Set[(String, String, String, GUID, GUID)]): Future[_] = {
      if (res.size == 0) {
        println("It was an insert")
        val insert = indexEntries += (entityType, indexName, indexValue, valueId, versionedId)
        db.run(insert)
      }
      else if (res.size == 1) {
        println("It is an update")
        val oldRow = res.head
        if (oldRow._4 == valueId) {
          println("On an existing value")
          val upQ = q.map(x => (x.valueId, x.versionedId)).update(valueId, versionedId)
          db.run(upQ)
        }
        else {
          println("But is in CONFLICT. Ignoring upsert")
          badEventCount += 1
          Future.successful()
        }
      }
      else {
        // This can't happen because we just queried against a unique key
        println("Database is already corrupted")
        Future.failed(new IllegalStateException("NosqlLatestIndex tables have been corrupted"))
      }
    }

    val readCheck = db.run(previous).andThen({case r => {
      r.map(doUpdate)
      ()

    }}).map(x => ())

    readCheck

  }
}
