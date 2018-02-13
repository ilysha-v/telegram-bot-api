package model

sealed trait Message {
  val id: Int
  val sender: Sender
  val date: Int
  val chatInfo: ChatInfo
}

case class TextMessage(
    id: Int,
    sender: Sender,
    date: Int,
    chatInfo: ChatInfo,
    text: Option[String]
) extends Message

case class TelegramApiResponse[T](ok: Boolean, result: T)


