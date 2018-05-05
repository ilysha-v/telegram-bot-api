package com.github.ilyshav.telegaClient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, QueueOfferResult}
import akka.util.ByteString
import com.github.ilyshav.telegaClient.model.{Message, MessageUpdate, ResponseMessage, TelegramApiResponse, Update, UpdateId}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/***
  * Lower level api client - provide full access to telegram responses.
  * For more high-level APIs you should use TelegramConnection
  */
class TelegramConnector(token: String)(
  implicit as: ActorSystem,
   ec: ExecutionContext,
   mat: ActorMaterializer) extends StrictLogging {
  import TelegramJsonProtocol._
  import spray.json._

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

  def getUpdates(lastUpdate: Option[UpdateId]): Future[TelegramApiResponse[Seq[Update]]] = {
    val methodName = "getUpdates"

    val uri = lastUpdate.fold(buildUri(methodName)) { lastUpdateId =>
      buildUri(methodName).withQuery(Query("offset" -> lastUpdateId.id.toString))
    }

    val request = HttpRequest(method = HttpMethods.GET, uri = uri)
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
      uri = buildUri(methodName).withQuery(Query("chat_id" -> chatId.toString)),
      method = HttpMethods.POST,
      entity = payload
    )

    sendRequest(request).flatMap { response =>
      Unmarshal(response).to[TelegramApiResponse[Message]]
    }
  }

  private def sendRequest(request: HttpRequest): Future[HttpResponse] = {
    logger.debug(s"Sending request to Telegram API: $request")

    val responsePromise = Promise[HttpResponse]()
    queue.offer(request -> responsePromise).flatMap { // todo error handling
      case QueueOfferResult.Enqueued =>
        val result = responsePromise.future
        result.onComplete {
          case Success(r) => logger.debug(s"Request was successfully completed. Result: $r")
          case Failure(ex) => logger.error("Got error while sending request to Telegram API", ex)
        }
        result
      case QueueOfferResult.Dropped =>
        Future.failed(
          new RuntimeException("Queue overflowed. Try again later."))
      case QueueOfferResult.Failure(ex) => Future.failed(ex)
      case QueueOfferResult.QueueClosed =>
        Future.failed(new RuntimeException(
          "Queue was closed (pool shut down) while running the request. Try again later."))
    }
  }

  private def buildUri(methodName: String): Uri =
    Uri(s"https://$baseUrl/bot$token/$methodName")
}
