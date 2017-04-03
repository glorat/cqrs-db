package net.glorat.db.test

import java.nio.charset.Charset

import net.glorat.db.{MyIndex, MyKey, MyValue}
import org.joda.time.Instant

/**
  * Created by kevin on 03/04/2017.
  */
case class Trade(val tradeId: TradeId, val ticketName: String, val tradeDetails: String, val transactionTime: Instant = new Instant(0))
  extends MyValue {
    def key = tradeId


    override def uniqueKeys : Seq[MyIndex] = {
      // In a more evolved version, one could use reflection to search for annotations on the vals
      Seq(MyIndex("ticketName", ticketName))
    }
  }

case class TradeId(val id: String) extends MyKey {
  private val UTF8: Charset = Charset.forName("UTF-8")

  // Must be stable serialization of the primary key
  def toBytes = s"TradeId:${id}".getBytes(UTF8)

}
