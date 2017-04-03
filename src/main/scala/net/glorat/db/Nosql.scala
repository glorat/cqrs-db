package net.glorat.db

import CQRS._
import eventstore._
import org.joda.time.Instant



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

class MyCommandHandler(repository: IRepository) extends CommandHandler {
  def receive: PartialFunction[Command, Unit] = {
    case c: Upsert => handle(c)
  }

  private def handle(c: Upsert) = {
    val key = c.ent.key.toUniqueId
    val item = repository.getById(key, new StoredValue)
    // We don't *really* care if it existed or not before, it's getting saved!
    if (item.alive) {
      // inserting
    }
    else {
      // updating
    }
    item.upsert(c.ent)
    // Let's choose our at-least-once semantics to trigger a resave
    repository.save(item, -1)
  }
}


