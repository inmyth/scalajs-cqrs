package me.mbcu.cqrs.query.service.org

import me.mbcu.cqrs.shared.http.Response.Vhashed

import java.util.UUID

private[org] case class OrgView(
    id: UUID,
    version: Int,
    vhash: String,
    name: String,
    location: String,
    description: Option[String]
) extends Vhashed
