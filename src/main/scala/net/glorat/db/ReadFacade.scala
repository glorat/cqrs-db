package net.glorat.db

import scala.concurrent.{ExecutionContext, Future}

/**
  * Decouple all read demand from implementation (dbs) by having the read-contract here
  *
  * @param dbs the storage implementation
  */

class ReadFacade(dbs:Stores)(implicit ec:ExecutionContext) {
  import slick.jdbc.H2Profile.api._

  def upsertIsIndexSafe(entityType:String, indexName:String, indexValue:String, valueId: GUID) : Future[Boolean] = {
    val where = dbs.indexEntries.filter(idx => idx.entityType === entityType && idx.indexName === indexName && idx.indexValue === indexValue)
    val select = where.map(x => x.valueId).result
    val res = dbs.hdb.run(select)
    res.map(x => {
      if (x.length == 0) {
        true // inserts are safe
      }
      else if (x.length > 1) {
        throw new IllegalStateException("Unique key tables are corrupted")
      }
      else {
        val rec = x.head
        rec == valueId
      }

    })
  }
}
