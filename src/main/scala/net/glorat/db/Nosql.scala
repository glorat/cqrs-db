package net.glorat.db

import CQRS._
import eventstore._
import org.joda.time.Instant

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration



class StoredValue extends AggregateRoot {
  var id: GUID = java.util.UUID.randomUUID()
  var key: MyKey = null
  var tt:Instant = new Instant(0)

  def this(id_ : GUID, key_ : MyKey) = {
    this()
    applyChange(Defined(id_, key_))
  }

  var alive: Boolean = false
  private def invalidated = !alive // The reverse

  def handle: PartialFunction[DomainEvent, Unit] = {
    case e: Upserted => handle(e)
  }

  private def handle(e: Upserted) = {
    alive = true
    tt = e.transactionTime
  }

  private def handle(e: Invalidated) = {
    alive = false
  }

  def upsert(ent: MyValue) : Unit = {
    val newTt = Instant.now()
    require(newTt.isAfter(tt), "Server must generate increasing tt")

    val versionedId = java.util.UUID.randomUUID()
    // No invariants to check on upsert
    applyChange(Upserted(id, versionedId, ent, newTt))
  }

  def invalidate(ent: MyValue) : Unit = {
    require(this.alive, "Can only invalidate alive entities")
    applyChange(Invalidated(id))
  }

}

class MyCommandHandler(repository: IRepository, read:ReadFacade)(implicit ec:ExecutionContext) extends CommandHandler {
  def receive: PartialFunction[Command, Future[Unit]] = {
    case c: Upsert => handle(c)
  }

  private def handle(c: Upsert) = {
    val key = c.ent.key.toUniqueId
    val item = repository.getById(key, new StoredValue)

    val uniqueCheck : Future[Unit] = if (c.opts.checkUniqueKeyConflict) {
      // We could check to see if what we are upserting violates indices
      // This is obviously slower and will reduce throughput as we need to roundtrip the indices
      // TODO: Need to ensure that the index projection is up to date wrt to "item" we just loaded and wait until it is
      val isSafe = read.upsertIsIndexSafe(c.ent.entityType, c.ent.uniqueKeys(0).indexName, c.ent.uniqueKeys(0).indexValue, key)
      isSafe.map(x => if (!x) {
        throw new IllegalStateException("Upsert wasn't safe on unique indicies")
      })
    }
    else {
      Future.successful()
    }

    val doUpsert : Future[Unit] = {
      item.upsert(c.ent)
      // Let's choose our at-least-once semantics to trigger a resave
      repository.save(item, -1)
    }

    // First check, then upsert
    for {
      () <- uniqueCheck
      () <- doUpsert} yield ()

  }
}


