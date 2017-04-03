package net.glorat.db.test

import org.joda.time.{Instant, LocalDate}

/**
  * Created by kevin on 03/04/2017.
  */
case class TradesDoneEodMilestone(val date: LocalDate, val reason: String, val transactionTime: Instant)
  extends Milestone {

    override def entityTypes: Seq[String] = Seq(Trade(TradeId(""), "", "", new Instant(0)).entityType)
  }
