package org.broadinstitute.monster.sbt

import io.circe.Decoder

/**
  * Identifier for a table/column in a Jade dataset.
  *
  * We restrict how this wrapper can be constructed to enforce
  * that the ID string only contains allowed characters.
  */
class JadeIdentifier private (val id: String) extends AnyVal

object JadeIdentifier {
  /** Pattern matching valid Jade IDs. */
  private val idPattern = "^[a-z_][a-z0-9_]*$".r

  implicit val decoder: Decoder[JadeIdentifier] =
    Decoder[String].emap { stringValue =>
      val matcher = idPattern.pattern.matcher(stringValue)
      Either.cond(
        matcher.matches(),
        new JadeIdentifier(stringValue),
        s"String '$stringValue' is not a valid Jade identifier"
      )
    }
}
