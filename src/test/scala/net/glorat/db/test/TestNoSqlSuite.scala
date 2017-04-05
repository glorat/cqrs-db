package net.glorat.db.test

import CQRS.{Command, DomainEvent, EventStoreRepository, OnDemandEventBus}
import eventstore.{InMemoryPersistenceEngine, OptimisticEventStore}
import net.glorat.db._
import org.scalatest._

import scala.concurrent.ExecutionContext

/**
  * Created by kevin on 03/04/2017.
  */
class TestNoSqlSuite extends FlatSpec {

  implicit val ec = ExecutionContext.Implicits.global

  val dbs : Stores = new MemoryStores

  val id = java.util.UUID.fromString("9d9814f5-f531-4d80-8722-f61dcc1679b8")
  val persistence = dbs.writeDb
  // val persistence = new MongoPersistenceEngine(MongoClient("localhost").getDB("test"), null)
  // persistence.purge

  val store = new OptimisticEventStore(persistence, Seq())
  val rep = new EventStoreRepository(store)
  val read = new ReadFacade(dbs)
  val cmds = new MyCommandHandler(rep,  read)


  val latestIndex = new NosqlLatestIndex(dbs)
  val latestView = new NosqlLatestView(dbs)
  val blobStoreView = new NosqlBlobStore(dbs)
  val bus = new OnDemandEventBus(Seq(latestView, blobStoreView, latestIndex), ec)

  // Sync to read by default
  val syncSendCommand: Command => Unit = (cmd => { cmds.receive(cmd); bus.pollEventStream(store.advanced) })

  var initialTt = latestView.nowTt

  "Sending an upsert command" should "produce 1 event " in {
    val something = Trade(TradeId("foo"), "aticket", "bar")

    syncSendCommand(Upsert(something))

    val evs = store.advanced.getFrom(0).flatMap(_.events).map(em => em.body.asInstanceOf[DomainEvent])
    //evs.foreach(ev => println(ev))
    assert(1 == evs.size)
  }

  it should "appear in the latest view" in {
    val optValue = dbs.latestView.get(TradeId("foo").toUniqueId)
    assert(optValue.isDefined, "is found")
    val versionedId = optValue.get
    val optObj = dbs.blobStore.get(versionedId)
    assert (optObj.isDefined, s"should be in blob store with vid ${versionedId}")
    val obj = optObj.get.asInstanceOf[Trade]
    assert(obj != null, "is a trade")
    assert("foo" == obj.tradeId.id)
  }

  it should "have a transaction time that is higher than before" in {
    val newTt = latestView.nowTt
    assert(newTt.isAfter(initialTt), s"${newTt} should be newer than initial ${initialTt}")
  }

  it should "not be a bad event in the index view" in {
    assert(latestIndex.badEventCount == 0)
  }

  "Sending an update" should "produce 1 event" in {
    val somethingMore = Trade(TradeId("foo"), "aticket", "bar")
    syncSendCommand(Upsert(somethingMore))
    val evs = store.advanced.getFrom(0).flatMap(_.events).map(em => em.body.asInstanceOf[DomainEvent])
    assert( 2 == evs.size)
  }

  it should "be a valid index update" in {
    assert(latestIndex.badEventCount == 0)
  }

  "Sending a conflicting index update without checking" should "produce 1 event" in {
    val conflict = Trade(TradeId("conflicting"), "aticket", "bar")
    syncSendCommand(Upsert(conflict, UpsertOpts(checkUniqueKeyConflict = false)))
    val evs = store.advanced.getFrom(0).flatMap(_.events).map(em => em.body.asInstanceOf[DomainEvent])
    assert( 3 == evs.size)
  }

  it should "be an invalid index update" in{
    assert(latestIndex.badEventCount == 1)
  }

  "Sending a conflicting index update with checkout" should "fail command" in {
    val conflict = Trade(TradeId("conflicting"), "aticket", "bar")
    assertThrows[IllegalStateException] {
      syncSendCommand(Upsert(conflict, UpsertOpts(checkUniqueKeyConflict = true)))
    }
  }
}
