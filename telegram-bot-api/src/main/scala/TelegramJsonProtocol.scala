import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import model._
import spray.json._

trait TelegramJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val chatInfoFormat = jsonFormat(ChatInfo, "id", "first_name", "last_name")
  implicit val senderFormat = jsonFormat(Sender, "id", "is_bot", "first_name", "last_name", "username", "language_code")
  implicit def apiResponseFormat[A](implicit jf: JsonFormat[A]): JsonFormat[TelegramApiResponse[A]] =
    jsonFormat2(TelegramApiResponse[A])

  implicit val updateFormat = new RootJsonFormat[MessageUpdate] {
    override def read(json: JsValue): MessageUpdate = json match {
      case obj: JsObject =>
        val updateId = obj.fields("update_id").convertTo[Int]
        obj.fields match {
          case x if x.contains("message") =>
            val payload = obj.fields("message").convertTo[Message]
            MessageUpdate(updateId, payload)
        }
      case _ => ??? // todo
    }

    override def write(obj: MessageUpdate): JsValue = ???
  }
}

object TelegramJsonProtocol extends TelegramJsonProtocol
