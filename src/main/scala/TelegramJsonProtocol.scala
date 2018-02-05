import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import model.{ChatInfo, Sender, Update}
import spray.json._

trait TelegramJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val chatInfoFormat = jsonFormat(ChatInfo, "id", "first_name", "last_name")
  implicit val senderFormat = jsonFormat(Sender, "id", "is_bot", "first_name", "last_name", "username", "language_code")
  implicit val updateFormat = new RootJsonFormat[Update] {
    override def read(json: JsValue): Update = json match {
      case obj: JsObject => ???
      case _ => ??? // todo
    }

    override def write(obj: Update): JsValue = ???
  }
}

object TelegramJsonProtocol extends TelegramJsonProtocol
