import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import model.Update

import scala.concurrent.{ExecutionContext, Future}

class TelegramConnection(private val connector: TelegramConnector)(implicit as: ActorSystem) {

  private implicit val mat = ActorMaterializer()
  private implicit val executionContext = as.dispatcher

  def getUpdates(): Future[Seq[Update]] = {
    connector
      .getUpdates()
        .flatMap { r =>
          if (r.ok) Future.successful(r.result)
          else Future.failed(new RuntimeException("problem")) // todo typed exception
        }
  }
}

object TelegramConnection {
  def apply(token: String)(implicit as: ActorSystem, ec: ExecutionContext, mat: ActorMaterializer): TelegramConnection = {
    val connector = new TelegramConnector(token)
    new TelegramConnection(connector)
  }
}
