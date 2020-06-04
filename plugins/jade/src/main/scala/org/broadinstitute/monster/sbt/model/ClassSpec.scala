package org.broadinstitute.monster.sbt.model

trait ClassSpec {
  def name: JadeIdentifier
  def columns: Vector[SimpleColumn]
  def structColumns: Vector[StructColumn]
  def tableFragments: Vector[JadeIdentifier]
}
