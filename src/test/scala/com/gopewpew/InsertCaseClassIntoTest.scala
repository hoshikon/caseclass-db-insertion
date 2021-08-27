package com.gopewpew

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class InsertCaseClassIntoTest extends AnyFreeSpec with Matchers {
  case class TestClass(name: String, value: Int)
  "should compile" in {
    InsertCaseClassInto("table")(TestClass("name 1", 1))
  }
}
