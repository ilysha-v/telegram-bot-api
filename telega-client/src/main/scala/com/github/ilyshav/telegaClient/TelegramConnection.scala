package com.github.ilyshav.telegaClient

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.ilyshav.telegaClient.model.{Message, ResponseMessage, TelegramApiResponse, Update, UpdateId}

import scala.concurrent.{ExecutionContext, Future}

class TelegramConnection(private val connector: TelegramConnector)(implicit as: ActorSystem) {
  private implicit val executionContext = as.dispatcher

  def getUpdates(lastUpdate: Option[UpdateId]): Future[Seq[Update]] = {
    connector
      .getUpdates(lastUpdate)
      .flatMap(extractResponse)
  }

  def sendMessage(chatId: Int, response: ResponseMessage): Future[Message] = {
    connector.sendMessage(chatId, response).flatMap(extractResponse)
  }

  private def extractResponse[A](r: TelegramApiResponse[A]): Future[A] = {
    if (r.ok) Future.successful(r.result)
    else Future.failed(new RuntimeException("problem")) // todo typed exception and log
  }
}

object TelegramConnection {
  def apply(token: String)(implicit as: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer): TelegramConnection = {
    val connector = new TelegramConnector(token)
    new TelegramConnection(connector)
  }
}
