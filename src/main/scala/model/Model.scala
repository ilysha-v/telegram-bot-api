package model

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

case class Update(id: Int, message: Message)
