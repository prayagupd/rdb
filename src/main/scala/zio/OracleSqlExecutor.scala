package zio

import java.sql.{Connection, DriverManager, ResultSet, Statement}

import scalaz.zio.{App, IO}
import scalaz.zio.console._

import scala.collection.immutable
import scala.collection.mutable.ListBuffer
import reflect.runtime.universe._

object OracleSqlExecutor extends App {

  object Db {
    val url = "jdbc:oracle:thin:@updupd.com:1521/order-api"
    val driver = "oracle.jdbc.driver.OracleDriver"
    val username = "updupd"
    val password = ""
  }

  import scalaz._
  import scalaz.zio._

  import Db._

  def connection(url: String, username: String, password: String): IO[Throwable, Connection] =
    IO.sync(DriverManager.getConnection(url, username, password))

  def statement(connection: Connection): IO[Throwable, Statement] =
    IO.sync(connection.createStatement())

  def selectAll(query: String)(statement: Statement): IO[Throwable, ResultSet] =
    IO.sync(statement.executeQuery(query))

  def run(args: List[String]): IO[Nothing, ExitStatus] =
    program.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))

  def asResult[a: TypeTag](rs: ResultSet): IO[Throwable, List[List[String]]] =
    IO.sync {
      val numberOfCols = rs.getMetaData.getColumnCount

      var results = ListBuffer[List[String]]()

      while (rs.next()) {
        val x = typeOf[a]
        val y = typeOf[String]

        val row: Seq[String] = //x match {
//          case y =>
            for (index <- 1 to numberOfCols) yield rs.getString(index)
//          case _ =>
//            for (index <- 1 to numberOfCols) yield rs.getInt(index)
        //}
        //val row: Seq[String] = for (index <- 1 to numberOfCols) yield rs.getString(index)
        results += row.toList
      }

      results.toList
    }

  def program[a: TypeTag]: IO[Throwable, List[List[String]]] =
    connection(url, username, password).flatMap { conn =>
      statement(conn).flatMap { st =>
        selectAll("select active from orders")(st).flatMap { rs =>
          asResult[a](rs).flatMap { r =>
            IO.sync {
              println(r)
              r
            }
          }
        }
      }
    }

  //  def program: IO[Throwable, ResultSet] = for {
  //    connection: Connection <- connections
  //    statement: Statement <- statement(IO(connection))
  //    result: ResultSet <- read("select active from chat_queue")(statement)
  //  } yield result

}
