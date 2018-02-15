package model

case class Message(
    id: Int,
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

sealed trait Update
case class MessageUpdate(id: Int, message: Message) extends Update
