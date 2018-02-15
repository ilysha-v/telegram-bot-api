import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import model._
import spray.json._

trait TelegramJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val chatInfoFormat = jsonFormat(ChatInfo, "id", "first_name", "last_name")
  implicit val senderFormat = jsonFormat(Sender, "id", "is_bot", "first_name", "last_name", "username", "language_code")
  implicit def apiResponseFormat[A](implicit jf: JsonFormat[A]): JsonFormat[TelegramApiResponse[A]] =
    jsonFormat2(TelegramApiResponse[A])

  implicit val messageFormat = new RootJsonFormat[Message] {
    override def read(json: JsValue): Message = json match {
      case obj: JsObject =>
        val id = obj.fields("message_id").convertTo[Int]
        val sender = obj.fields.get("from").map(_.convertTo[Sender])
        val date = obj.fields("date").convertTo[Int]
        val chatInfo = obj.fields("chat").convertTo[ChatInfo]
        val text = obj.fields.get("text").map(_.convertTo[String])

        // todo other fields
        // todo be careful with options
        // todo Map.get will return default value, should return some kind of error instead

        Message(id, sender, date, chatInfo, text)
      case _ => deserializationError("Value of message field must be an object")
    }

    override def write(obj: Message): JsValue = serializationError("not implemented")
  }

  implicit val updateFormat = new RootJsonFormat[MessageUpdate] {
    override def read(json: JsValue): MessageUpdate = json match {
      case obj: JsObject =>
        val updateId = obj.fields("update_id").convertTo[Int]
        obj.fields match {
          case x if x.contains("message") =>
            val payload = obj.fields("message").convertTo[Message]
            MessageUpdate(updateId, payload)
        }
      case _ => deserializationError("Value of update field must be an object")
    }

    override def write(obj: MessageUpdate): JsValue = serializationError("not implemented")
  }
}

object TelegramJsonProtocol extends TelegramJsonProtocol
