package controllers

import play.api._
import libs.iteratee.{Iteratee, Concurrent}
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  val (aacStream, aacChannel) = Concurrent.broadcast[Array[Byte]]
  val uploadParser = BodyParser((header: RequestHeader) => {
    Iteratee.fold[Array[Byte], Int](0) {
      (length, bytes) => {
        aacChannel.push(bytes)
        println("Processed: " + bytes.length + " bytes")
        length + bytes.size
      }
    } mapDone(size => Right(size))
  })

  def feed = Action(uploadParser) { length =>
    Logger.info("feed: " + length)
    Ok("YES:" + length)
  }

  def listen = Action {
    Ok.feed(aacStream &> Concurrent.dropInputIfNotReady(100)).withHeaders("Content-Type" -> "audio/mpeg", "Cache-Control" -> "no-cache")
  }
  
}