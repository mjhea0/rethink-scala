package com.rethinkscala.ast

import com.rethinkscala.{DatumOrFunction, DatumAssocPair, DatumMessage, AssocPair}
import ql2.{Ql2 => ql2}
import ql2.Datum.DatumType
import com.rethinkscala.net.RethinkDriverError
import org.joda.time.{DateTimeZone, DateTime}


sealed trait Datum extends DatumMessage with DatumOrFunction {

  override def optArgsBuilder(key: String, value: Any): AssocPair = DatumAssocPair(key, value)

}

object Datum {

  import ql2.Datum.DatumType.{R_NULL, R_BOOL, R_NUM, R_STR, R_ARRAY, R_OBJECT}


  private val KEY_REQL_TYPE = "$reql_type$"
  private val REQL_TYPE_TIME = "TIME"

  def wrap(datum: ql2.Datum): (Any, String) = {
    val buf = new StringBuilder
    (wrap(datum, buf), buf.toString())

  }


  def toDateTime(m: Map[String, Any]): DateTime = {
    m.get("epoch_time").map(_.asInstanceOf[Long] * 1000).map {
      epoch =>
        val tz = m.get("timezone").map(t => DateTimeZone.forID(t.asInstanceOf[String])).getOrElse(DateTimeZone.getDefault)
        new DateTime(epoch, tz)

    }.getOrElse(throw RethinkDriverError(
      "psudo-type TIME object %s does not have expected field \"epoch_time\"".format(m)
    ))
  }


  def wrap(datum: ql2.Datum, buf: StringBuilder): Any = {
    import scala.collection.JavaConverters._
    datum.getType match {
      case R_NULL => {
        buf ++= "null"
        None
      }
      case R_BOOL => {
        buf ++= datum.getRBool.toString
        datum.getRBool
      }
      case R_NUM => {
        buf ++= datum.getRNum.toLong.toString
        datum.getRNum.toLong
      }
      case R_STR => {
        buf ++= "\"" + datum.getRStr + "\""

        datum.getRStr
      }
      case R_ARRAY => {
        buf ++= "["
        val array = datum.getRArrayList.asScala
        val len = array.size
        val unwraped = array.zipWithIndex map {
          case (value: ql2.Datum, index: Int) => {
            val rtn = wrap(value, buf)
            if (index < len - 1) buf ++= ","
            rtn
          }
        }
        buf ++= "]"
        unwraped

      }
      case R_OBJECT => {
        buf ++= "{"
        val obj = datum.getRObjectList.asScala
        val len = obj.size

        var index = 0 // FixMe couldn't get .zipWithIndex to work
        val unwrapped = obj.map {
            ap => {
              buf ++= "\"" + ap.getKey + "\":"
              val rtn = (ap.getKey, wrap(ap.getVal, buf))
              index += 1
              if (index < len) buf ++= ","

              rtn
            }

          }
        buf ++= "}"
        val m = unwrapped.toMap

        m.get(KEY_REQL_TYPE) match {
          case Some(x) => x match {
            case REQL_TYPE_TIME => toDateTime(m)
            case _ => throw RethinkDriverError(s"Unknown psudo-type $x")
          }
          case _ => m

        }

      }
      case _ => None
    }
  }

  def apply(a: Any): Datum = a match {

    case Some(v) => Datum(v)
    case None => NoneDatum()
    case s: String => StringDatum(s)
    case i: Int => NumberDatum(i)
    case f: Float => NumberDatum(f)
    case l: Long => NumberDatum(l)
    case b: Boolean => BooleanDatum(b)
    case d: Double => NumberDatum(d)
    case _ => NoneDatum()

  }
}

object NoneDatum {
  def apply() = new NoneDatum()
}

class NoneDatum extends Datum with Produce[Null] {

  def datumType = DatumType.R_NULL

  def build(d: ql2.Datum.Builder) = d

  def defaultValue = None
}

case class BooleanDatum(value: Boolean) extends Datum with ProduceBinary {

  override val extractArgs = false

  def datumType = DatumType.R_BOOL

  def build(d: ql2.Datum.Builder) = d.setRBool(value)

}

case class NumberDatum(value: Double) extends Datum with ProduceNumeric {

  override val extractArgs = false

  def datumType = DatumType.R_NUM

  def build(d: ql2.Datum.Builder) = d.setRNum(value)

}

case class StringDatum(value: String) extends Datum with ProduceString {

  override val extractArgs = false

  def datumType = DatumType.R_STR

  def build(d: ql2.Datum.Builder) = d.setRStr(value)

}