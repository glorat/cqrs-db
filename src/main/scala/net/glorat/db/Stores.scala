package net.glorat.db

import eventstore.InMemoryPersistenceEngine
import slick.jdbc.H2Profile
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.jdbc.H2Profile.api._

/**
  * Created by kevin on 04/04/2017.
  */
trait Stores {
  def writeDb : eventstore.IPersistStreams
  def hdb : H2Profile.backend.Database
  def indexEntries : TableQuery[IndexEntries]
  var blobStore: Map[GUID, MyValue]

  var latestView : Map[GUID, GUID]

}

class MemoryStores() extends Stores {
  // Write stores
  val writeDb = new InMemoryPersistenceEngine
  // val writeDb = new MongoPersistenceEngine(MongoClient("localhost").getDB("test"), null)

  // Read stores
  val indexEntries = TableQuery[IndexEntries]

  var latestView : Map[GUID, GUID] = Map()


  var blobStore: Map[GUID, MyValue] = Map()

  private val setup = DBIO.seq(
    // Create the tables, including primary and foreign keys
    (indexEntries.schema).create
  )

  private val dbcfg:Map[String, String] =  Map(
    "driver"->"org.h2.Driver",
    "connectionPool"-> "disabled",
    "keepAliveConnection"-> "true")

  val hdb: H2Profile.backend.Database = Database.forURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", dbcfg)

  // Constructor logic begins here!

  // Being an in-memory database, we need to set up the table schemas
  require(Await.result(hdb.run(MTable.getTables), Duration.Inf).toList.isEmpty)

  private val setupFuture = hdb.run(setup)
  Await.result(setupFuture, Duration.Inf)


  // A sanity check to ensure the table really got created
  // This can go wrong if DB_CLOSE_DELAY is not specified, leading to a reset mem db every session, instead of run
  require(Await.result(hdb.run(MTable.getTables), Duration.Inf).toList.length == 1)


}
