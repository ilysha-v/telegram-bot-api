import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}
import spray.json.{DefaultJsonProtocol, JsonFormat}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

class TelegramConnector(token: String)(
  implicit as: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer) {
  private val baseUrl = s"https://api.telegram.org/bot$token/"
  private lazy val queue = {
    val connectionPool =
      Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](baseUrl)

    Source
      .queue[(HttpRequest, Promise[HttpResponse])](20, OverflowStrategy.dropNew) // todo size to config
      .via(connectionPool)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p))    => p.failure(e)
      }))(Keep.left)
      .run()
  }

  def getUpdates(): Future[HttpResponse] = { // todo telegram response wrapper should be returned from here
    val methodName = "getUpdates"

    val request = HttpRequest(method = HttpMethods.GET, uri = methodName)
    sendRequest(request)
  }

  private def sendRequest(request: HttpRequest): Future[HttpResponse] = {
    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap { // todo error handling
      case QueueOfferResult.Enqueued => responsePromise.future
      case QueueOfferResult.Dropped =>
        Future.failed(
          new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed =>
        Future.failed(new RuntimeException(
          "Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }
}

private object TelegramConnector {
  case class TelegramApiResponse[T](ok: Boolean, result: T)

  object JsonProtocol extends DefaultJsonProtocol with TelegramJsonProtocol { // todo move to main protocol
    implicit def apiResponseFormat[A](
                                       implicit jf: JsonFormat[A]): JsonFormat[TelegramApiResponse[A]] =
      jsonFormat2(TelegramApiResponse[A])
  }
}