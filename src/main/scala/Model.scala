sealed trait TelegramMessage

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

case class Message(
    id: Int,
    sender: Sender,
    chatInfo: ChatInfo,
    date: Int,
    text: Option[String]
) extends TelegramMessage

case class ApiResponse[A <: TelegramMessage](isSuccess: Boolean, body: A)
