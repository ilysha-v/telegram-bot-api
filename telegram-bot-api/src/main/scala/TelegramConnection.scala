import akka.actor.ActorSystem
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import model.{Message, TelegramApiResponse, MessageUpdate}

import scala.concurrent.{ExecutionContext, Future}

class TelegramConnection(private val connector: TelegramConnector)(
    implicit as: ActorSystem) {
  import spray.json._
  import TelegramJsonProtocol._

  private implicit val mat = ActorMaterializer()
  private implicit val executionContext = as.dispatcher

  def getNewMessages(): Future[Seq[Message]] = {
    connector
      .getUpdates()
      .flatMap { response =>
        Unmarshal(response).to[String] // todo unmarshallers?
      }.map { jsonString =>
        val response =
          jsonString.parseJson.convertTo[TelegramApiResponse[Seq[MessageUpdate]]]
        if (response.ok) response.result.map(_.message)
        else throw new RuntimeException("Telegram error") // todo
      }
  }
}

object TelegramConnection {
  def apply(token: String)(implicit as: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer): TelegramConnection = {
    val connector = new TelegramConnector(token)
    new TelegramConnection(connector)
  }
}
