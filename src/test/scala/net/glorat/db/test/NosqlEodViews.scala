package net.glorat.db.test

import CQRS.EventStreamReceiver
import eventstore.CommitedEvent
import net.glorat.db.Upserted

import scala.concurrent.Future

/**
  * Created by kevin on 03/04/2017.
  */
object NosqlEodViews extends EventStreamReceiver
{
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

    if (myValue.isInstanceOf[Milestone]) {

    }
???
  }


}
