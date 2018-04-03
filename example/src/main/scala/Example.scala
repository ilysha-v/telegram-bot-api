import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import model.{MessageUpdate, ResponseMessage}

object Example {
  def main(args: Array[String]): Unit = {
    implicit val as = ActorSystem()
    implicit val ec = as.dispatcher
    implicit val mat = ActorMaterializer()

    val config = ConfigFactory.load()
    val token = config.getString("token")
    val connector = TelegramConnection(token)

    connector.getUpdates().foreach(_.map {
        case u: MessageUpdate =>
          connector.sendMessage(u.message.chatInfo.id, ResponseMessage(Some(u.message.id), "Yep"))
        case u => ()
      }
    )
  }
}

