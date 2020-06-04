package org.broadinstitute.monster.sbt.model

/**
  * Common interface for resources which can be used to generate Scala case class definitions.
  */
trait ClassSpec {
  /** Jade-compatible name for the resource. */
  def name: JadeIdentifier

  /** Columns in the resource which should be mapped to scalar class fields. */
  def columns: Vector[SimpleColumn]

  /** Columns in the resource which should be mapped to class fields referencing a struct class. */
  def structColumns: Vector[StructColumn]

  /**
    * Names of table-fragment objects included in the resource, which should be mapped to class
    * fields referencing a fragment class.
    */
  def tableFragments: Vector[JadeIdentifier]
}
