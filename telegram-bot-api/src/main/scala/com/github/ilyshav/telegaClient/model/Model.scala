package com.github.ilyshav.telegaClient.model

case class MessageId(id: Int) extends AnyVal
case class UpdateId(id: Int) extends Comparable[UpdateId] {
  override def compareTo(o: UpdateId): Int = id.compareTo(o.id)
  def next: UpdateId = UpdateId(id + 1)
}
case class Message(
  id: MessageId,
  sender: Option[User],
  date: Int,
  chatInfo: ChatInfo,
  text: Option[String],
  location: Option[Location]
)

case class TelegramApiResponse[T](ok: Boolean, result: T)

case class User(
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

case class Location (
  lon: Double,
  lat: Double
)

sealed trait Update {
  val id: UpdateId
}

case class MessageUpdate(id: UpdateId, message: Message) extends Update
case class ResponseMessage(replyTo: Option[MessageId], text: String)