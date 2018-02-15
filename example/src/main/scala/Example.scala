import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

object Example {
  def main(args: Array[String]): Unit = {
    implicit val as = ActorSystem()
    implicit val ec = as.dispatcher
    implicit val mat = ActorMaterializer()

    val config = ConfigFactory.load()
    val token = config.getString("token")
    val connector = TelegramConnection(token)

    for {
      updates <- connector.getNewMessages()
      _ = println(updates)
    } yield as.terminate()
  }
}

