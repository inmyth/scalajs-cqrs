package me.mbcu.cqrs.query.service

import me.mbcu.cqrs.shared.event.QueryTable

private[query] trait Table {

  def tableName: QueryTable

}
