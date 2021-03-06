import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.ilyshav.telegaClient.TelegramConnection
import com.github.ilyshav.telegaClient.model.{MessageUpdate, ResponseMessage, Update}
import com.typesafe.config.ConfigFactory

object Example {
  def main(args: Array[String]): Unit = {
    implicit val as = ActorSystem()
    implicit val ec = as.dispatcher
    implicit val mat = ActorMaterializer()

    val config = ConfigFactory.load()
    val token = config.getString("token")
    val connector = TelegramConnection(token)

    def sendResponses(updates: Seq[Update]) = {
      updates.foreach {
        case u: MessageUpdate =>
          connector.sendMessage(u.message.chatInfo.id, ResponseMessage(Some(u.message.id), "Yep"))
      }
    }

    (for {
      first <- connector.getUpdates(lastUpdate = None)
      _ = sendResponses(first)
      second <- connector.getUpdates(Some(first.map(_.id).max.next))
      _ = println(s"Unprocessed messages: $second")
    } yield ()).onComplete(_ => as.terminate())
  }
}

