import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/***
  * Lower level api client - provide full access to telegram responses.
  * For more high-level APIs you should use TelegramConnection
  */
class TelegramConnector(token: String)(implicit as: ActorSystem,
                                       ec: ExecutionContext,
                                       mat: ActorMaterializer) {
  private val baseUrl = s"api.telegram.org"
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
    val link = s"https://$baseUrl/bot$token/getUpdates" // todo create some kind of url builder

    val request = HttpRequest(method = HttpMethods.GET, uri = link)
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
