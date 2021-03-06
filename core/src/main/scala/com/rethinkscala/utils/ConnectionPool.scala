package com.rethinkscala.utils

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit
import scala.concurrent.{future, Future}

/** Created by IntelliJ IDEA.
  * User: Keyston
  * Date: 3/24/13
  * Time: 8:33 PM
  *
  */

//https://github.com/jamesgolick/scala-connection-pool/
trait ConnectionFactory[Connection] {
  def create(): Connection

  def validate(connection: Connection): Boolean

  def destroy(connection: Connection): Unit
}

trait ConnectionPool[Connection] {
  def apply[A]()(f: Connection => A): A
}

trait LowLevelConnectionPool[Connection] {
  def borrow(): Connection

  def giveBack(conn: Connection): Unit

  def invalidate(conn: Connection): Unit
}

class TimeoutError(message: String) extends Error(message)

class SimpleConnectionPool[Conn](connectionFactory: ConnectionFactory[Conn],
                                 max: Int = 20,
                                 timeout: Int = 500000)


  extends ConnectionPool[Conn] with LowLevelConnectionPool[Conn] {


  private val size = new AtomicInteger(0)

  private val pool = new ArrayBlockingQueue[Conn](max)

  def apply[A]()(f: Conn => A): A = {
    val connection = borrow

    try {
      val result = f(connection)
      giveBack(connection)
      result
    } catch {
      case t: Throwable =>
        invalidate(connection)
        throw t
    }
  }

  def take(f: (Conn, Conn => Unit) => Unit): Unit = {

    val connection = borrow

    try {
      f(connection, giveBack)

    } catch {
      case t: Throwable =>
        invalidate(connection)
        throw t
    }
  }

  def nonEmpty = size.get() > 0

  def isEmpty = size.get() == 1

  def borrow: Conn = Option(pool.poll()).getOrElse(createOrBlock)


  def giveBack(connection: Conn): Unit = {
    pool.offer(connection)
  }

  def invalidate(connection: Conn): Unit = {
    connectionFactory.destroy(connection)
    size.decrementAndGet
  }

  private def createOrBlock: Conn = {
    size.get match {
      case e: Int if e == max => block
      case _ => create
    }
  }

  private def create: Conn = {
    size.incrementAndGet match {
      case e: Int if e > max => {

        size.decrementAndGet
        borrow()
      }
      case e: Int => {

        connectionFactory.create
      }
    }
  }

  private def block: Conn = {
    Option(pool.poll(timeout, TimeUnit.NANOSECONDS)) getOrElse ({
      throw new TimeoutError("Couldn't acquire a connection in %d nanoseconds.".format(timeout))
    })
  }
}