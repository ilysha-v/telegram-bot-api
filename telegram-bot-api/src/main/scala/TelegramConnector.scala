import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}
import model.{TelegramApiResponse, Update}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/***
  * Lower level api client - provide full access to telegram responses.
  * For more high-level APIs you should use TelegramConnection
  */
class TelegramConnector(token: String)(implicit as: ActorSystem,
                                       ec: ExecutionContext,
                                       mat: ActorMaterializer) {
  import spray.json._
  import TelegramJsonProtocol._

  private val baseUrl = s"api.telegram.org"
  private lazy val queue = {
    val connectionPool = Http().cachedHostConnectionPoolHttps[Promise[HttpResponse]](baseUrl)

    Source
      .queue[(HttpRequest, Promise[HttpResponse])](20, OverflowStrategy.dropNew) // todo size to config
      .via(connectionPool)
      .toMat(Sink.foreach({
        case ((Success(resp), p)) => p.success(resp)
        case ((Failure(e), p))    => p.failure(e)
      }))(Keep.left)
      .run()
  }

  def getUpdates(): Future[TelegramApiResponse[Seq[Update]]] = {
    val link = s"https://$baseUrl/bot$token/getUpdates" // todo create some kind of url builder

    val f = TelegramJsonProtocol.updateFormat

    val request = HttpRequest(method = HttpMethods.GET, uri = link)
    sendRequest(request).flatMap { response =>
      Unmarshal(response).to[String] // todo unmarshallers?
    }.map(_.parseJson.convertTo[TelegramApiResponse[Seq[Update]]])
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
