import spray.json._

trait TelegramJsonProtocol extends DefaultJsonProtocol {

  implicit val chatInfo = jsonFormat(ChatInfo, "id", "first_name", "last_name")
}
