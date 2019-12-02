package org.broadinstitute.monster.sbt

import io.circe.Decoder

/** TODO */
class JadeIdentifier(val id: String) extends AnyVal

object JadeIdentifier {
  private val illegalChars = "[^a-z0-9_]".r

  implicit val decoder: Decoder[JadeIdentifier] =
    Decoder[String].emap { stringValue =>
      illegalChars
        .findFirstIn(stringValue)
        .toLeft(new JadeIdentifier(stringValue))
        .left
        .map { illegal =>
          s"Illegal character '$illegal' in ID '$stringValue': Jade identifiers must be lower-snake-case"
        }
    }
}
