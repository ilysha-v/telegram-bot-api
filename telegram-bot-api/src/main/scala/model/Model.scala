package model

case class MessageId(id: Int) extends AnyVal

case class Message(
    id: MessageId,
    sender: Option[Sender],
    date: Int,
    chatInfo: ChatInfo,
    text: Option[String]
)

case class TelegramApiResponse[T](ok: Boolean, result: T)

case class Sender(
    id: Int,
    isBot: Boolean,
    firstName: String,
    lastName: Option[String],
    username: Option[String],
    languageCode: Option[String]
)

case class ChatInfo(
    id: Int,
    firstName: Option[String],
    lastName: Option[String]
)

sealed trait Update {
  val id: Int
}
case class MessageUpdate(id: Int, message: Message) extends Update

case class ResponseMessage(text: String, replyTo: Option[MessageId])