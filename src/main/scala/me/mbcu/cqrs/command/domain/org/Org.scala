package me.mbcu.cqrs.command.domain.org

import me.mbcu.cqrs.command.app.CommandError.ValidationError
import me.mbcu.cqrs.command.domain.Aggregate
import me.mbcu.cqrs.command.domain.org.Org.{Description, Location, Name}

import java.util.UUID

private[org] class Org(orgId: UUID, val name: Name, val location: Location, val description: Description)
    extends Aggregate(orgId)

private[org] object Org {

  case object NoOrgId            extends ValidationError("No org id")
  case object NameTooLong        extends ValidationError("Name too long")
  case object NameEmpty          extends ValidationError("Name is empty")
  case object LocationEmpty      extends ValidationError("Location is empty")
  case object LocationTooLong    extends ValidationError("Location too long")
  case object DescriptionTooLong extends ValidationError("Description too long")

  case class Name(value: String) extends AnyVal
  object Name {
    def create(value: String): Either[ValidationError, Name] = {
      if (value.isEmpty) return Left(NameEmpty)
      if (value.length > 120) return Left(NameTooLong)
      Right(Name(value))
    }
  }

  case class Location(value: String) extends AnyVal
  object Location {
    def create(value: String): Either[ValidationError, Location] = {
      if (value.isEmpty) return Left(LocationEmpty)
      if (value.length > 150) return Left(LocationTooLong)
      Right(Location(value))
    }
  }

  case class Description(value: Option[String]) extends AnyVal
  object Description {
    def create(value: Option[String]): Either[ValidationError, Description] = {
      if (value.isEmpty) return Right(Description(None))
      if (value.get.length > 1200) return Left(DescriptionTooLong)
      Right(Description(value))
    }
  }

}
