import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.util.ByteString
import model.{Message, ResponseMessage, TelegramApiResponse, Update}

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
    val methodName = "getUpdates"

    val request = HttpRequest(method = HttpMethods.GET, uri = buildUri(methodName))
    sendRequest(request).flatMap { response =>
      Unmarshal(response).to[TelegramApiResponse[Seq[Update]]]
    }
  }

  def sendMessage(chatId: Int, message: ResponseMessage): Future[TelegramApiResponse[Message]] = {
    val methodName = "sendMessage"

    val payload = HttpEntity.Strict(
      contentType = ContentTypes.`application/json`,
      data = ByteString(message.toJson.compactPrint)
    )

    val request = HttpRequest(
      uri = buildUri(methodName),
      method = HttpMethods.POST,
      entity = payload
    )

    sendRequest(request).flatMap { response =>
      Unmarshal(response).to[TelegramApiResponse[Message]]
    }
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

  private def buildUri(methodName: String): String = s"https://$baseUrl/bot$token/$methodName"
}
