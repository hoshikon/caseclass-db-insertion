package com.gopewpew

import cats.instances.vector._
import cats.syntax.traverse._
import doobie.free.connection.ConnectionIO
import shapeless.ops.coproduct.Unifier
import shapeless.ops.hlist
import shapeless.ops.record._
import shapeless.{Coproduct, HList, LabelledGeneric}
import doobie.implicits._
import doobie.util.fragment.Fragment

import java.time.Instant

object InsertCaseClassInto {
  /**
   * @param tableName the name of the table
   * @param values case class representation of the data to be inserted
   * @param lbl converts a case class into a generic representation
   * @param keysAsHList returns an HList of parameter names (of given case class)
   * @param keysToList converts an HList into a List
   * @param toCoproduct converts a generic representation of a Product (case class) into a Coproduct
   * @param unifier converts a Coproduct into a typed value
   */

  def apply[T <: Product, Repr <: HList, K <: HList, C <: Coproduct](tableName: String)(values: T*)(
    implicit lbl: LabelledGeneric.Aux[T, Repr],
    keysAsHList: Keys.Aux[Repr, K],
    keysToList: hlist.ToList[K, Symbol],
    toCoproduct: hlist.ToCoproductTraversable.Aux[Repr, List, C],
    unifier: Unifier[C]
  ): ConnectionIO[Vector[T]] = {
    val keyList: List[String] = keysToList(keysAsHList()).map(_.name)
    values.map { v =>
      val x: Repr = lbl.to(v)
      val (_, keyValuePairs) = x.toCoproduct[List].foldLeft((keyList, List.empty[(String, String)])) {
        case ((klh :: klt, keyValues), option) =>
          option.unify(unifier) match {
            case Some(str: String)       => (klt, keyValues :+ (klh, s"'$str'"))
            case Some(instant: Instant)  => (klt, keyValues :+ (klh, s"'$instant'"))
            case Some(other)             => (klt, keyValues :+ (klh, other.toString))
            case _: None.type            => (klt, keyValues)
            case str: String             => (klt, keyValues :+ (klh, s"'$str'"))
            case instant: Instant        => (klt, keyValues :+ (klh, s"'$instant'"))
            case other                   => (klt, keyValues :+ (klh, other.toString))
          }
        case _ => throw new RuntimeException("We will never hit this!")
      }

      val (keys, valuesAsString) = keyValuePairs.unzip

      Fragment.const(
        s"""INSERT IGNORE INTO $tableName ${keys.mkString("(", ", ", ")")} VALUES ${valuesAsString.mkString("(", ", ", ")")}"""
      ).update.run.map(_ => v)
    }.toVector.sequence
  }
}
