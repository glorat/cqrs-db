package net.glorat.db.test

import CQRS.{Command, DomainEvent, EventStoreRepository, OnDemandEventBus}
import eventstore.{InMemoryPersistenceEngine, OptimisticEventStore}
import net.glorat.db._
import org.scalatest._

/**
  * Created by kevin on 03/04/2017.
  */
class TestNoSqlSuite extends FlatSpec {

  val id = java.util.UUID.fromString("9d9814f5-f531-4d80-8722-f61dcc1679b8")
  val persistence = new InMemoryPersistenceEngine
  // val persistence = new MongoPersistenceEngine(MongoClient("localhost").getDB("test"), null)
  // persistence.purge

  val store = new OptimisticEventStore(persistence, Seq())
  val rep = new EventStoreRepository(store)
  val cmds = new MyCommandHandler(rep)

  val bus = new OnDemandEventBus(Seq(NosqlLatestView, NosqlBlobStore))

  // Sync to read by default
  val syncSendCommand: Command => Unit = (cmd => { cmds.receive(cmd); bus.pollEventStream(store.advanced) })

  var initialTt = SampleDatabase.latestTransactionTime

  "Sending an upsert command" should "produce 1 event " in {
    val something = Trade(TradeId("foo"), "aticket", "bar")

    //val viewActor = InventoryItemDetailView
    syncSendCommand(Upsert(something))

    val evs = store.advanced.getFrom(0).flatMap(_.events).map(em => em.body.asInstanceOf[DomainEvent])
    //evs.foreach(ev => println(ev))
    assert(1 == evs.size)
  }

  it should "appear in the latest view" in {
    val optValue = SampleDatabase.latest.get(TradeId("foo").toUniqueId)
    assert(optValue.isDefined, "is found")
    val versionedId = optValue.get
    val optObj = NosqlBlobStore.blobStore.get(versionedId)
    assert (optObj.isDefined, s"should be in blob store with vid ${versionedId}")
    val obj = optObj.get.asInstanceOf[Trade]
    assert(obj != null, "is a trade")
    assert("foo" == obj.tradeId.id)
  }

  it should "have a transaction time that is higher than before" in {
    val newTt = SampleDatabase.latestTransactionTime
    assert(newTt.isAfter(initialTt), s"${newTt} should be newer than initial ${initialTt}")
  }

}
