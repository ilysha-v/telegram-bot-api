import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.unmarshalling.Unmarshaller
import model._
import spray.json._

import scala.language.implicitConversions

trait TelegramJsonProtocol extends DefaultJsonProtocol with SprayJsonSupport {
  implicit def createUnmarshaller[A](implicit jf: JsonReader[A]) = {
    Unmarshaller
      .stringUnmarshaller
      .forContentTypes(ContentTypes.`application/json`)
      .map(_.parseJson.convertTo[A])
  }

  implicit def anyValJsonFormat[A, B](fWrite: A => B)(fRead: B => A)(
    implicit jf: JsonFormat[B]
  ) = new RootJsonFormat[A] {
    override def write(obj: A): JsValue = jf.write(fWrite(obj))
    override def read(json: JsValue): A = fRead(jf.read(json))
  }

  implicit val chatInfoFormat = jsonFormat(ChatInfo, "id", "first_name", "last_name")
  implicit val senderFormat = jsonFormat(Sender, "id", "is_bot", "first_name", "last_name", "username", "language_code")
  implicit def apiResponseFormat[A](implicit jf: JsonFormat[A]): JsonFormat[TelegramApiResponse[A]] =
    jsonFormat2(TelegramApiResponse[A])

  implicit val messageIdFormat = anyValJsonFormat[MessageId, Int](_.id)(MessageId.apply)
  implicit val updateIdFormat = anyValJsonFormat[UpdateId, Int](_.id)(UpdateId.apply)

  implicit val messageFormat = new RootJsonFormat[Message] {
    override def read(json: JsValue): Message = json match {
      case obj: JsObject =>
        val id = obj.fields("message_id").convertTo[MessageId]
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

  implicit val messageUpdateFormat = new RootJsonFormat[MessageUpdate] {
    override def read(json: JsValue): MessageUpdate = json match {
      case obj: JsObject =>
        val updateId = obj.fields("update_id").convertTo[UpdateId]
        obj.fields match {
          case x if x.contains("message") =>
            val payload = obj.fields("message").convertTo[Message]
            MessageUpdate(updateId, payload)
        }
      case _ => deserializationError("Value of update field must be an object")
    }

    override def write(obj: MessageUpdate): JsValue = serializationError("not implemented")
  }

  implicit val updateFormat = new RootJsonFormat[Update] {
    override def read(json: JsValue): Update = json match {
      case obj: JsObject if obj.fields.contains("message") => obj.convertTo[MessageUpdate]
      case _ => deserializationError("Value of update field must be an object")
    }

    override def write(obj: Update): JsValue = serializationError("Update serialization is not supported")
  }

  implicit val responseFormat = jsonFormat(ResponseMessage, "reply_to_message_id", "text")
}

object TelegramJsonProtocol extends TelegramJsonProtocol
