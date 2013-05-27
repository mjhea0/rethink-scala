package com.rethinkdb.ast

import com.rethinkdb.{Term, TermMessage, MethodTerm}

import ql2.Term.TermType


/**
 * Created with IntelliJ IDEA.
 * User: keyston
 * Date: 4/2/13
 * Time: 7:36 PM
 * To change this template use File | Settings | File Templates.
 */

case class Append(array: Array[Any], other: Term) extends MethodTerm {
  override lazy val args = buildArgs(array, other)

  def termType = TermType.APPEND
}

case class Slice(target: Term, left: Int, right: Int) extends TermMessage {
  override lazy val args = buildArgs(target, left, right)

  def termType = TermType.SLICE
}

case class Skip(target: Term, amount: Int) extends TermMessage with MethodTerm {
  override lazy val args = buildArgs(target, amount)

  def termType = TermType.SKIP
}


case class Limit(target: Term, amount: Int) extends TermMessage with MethodTerm {
  override lazy val args = buildArgs(target, amount)

  def termType = TermType.LIMIT
}

case class GetAttr(target: Term, name: String) extends TermMessage {
  override lazy val args = buildArgs(target, name)

  def termType = TermType.GETATTR
}


case class Contains(target: Term, attribute: Iterable[String]) extends TermMessage {
  override lazy val args = buildArgs(target, attribute)

  def termType = TermType.CONTAINS
}

case class Pluck(target: Term, attributes: Iterable[String]) extends MethodTerm {
  override lazy val args = buildArgs(target, attributes)

  def termType = TermType.PLUCK
}

case class Without(target: Term, attributes: Iterable[String]) extends MethodTerm {
  override lazy val args = buildArgs(target, attributes)

  def termType = TermType.WITHOUT
}

case class Merge(other: Term) extends MethodTerm {
  override lazy val args = buildArgs(other)

  def termType = TermType.MERGE
}


case class Between(start: Int, end: Int) extends MethodTerm {
  override lazy val args = buildArgs(start, end)

  def termType = TermType.BETWEEN
}