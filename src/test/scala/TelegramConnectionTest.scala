import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.scalatest.AsyncFreeSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures._

import scala.concurrent.Future

class TelegramConnectionTest extends AsyncFreeSpec with MockitoSugar {

  "TelegramConnection" - {
    "should return failed future, if telegram respond with" - {
      "Bab request" in {
        implicit val as = ActorSystem() // todo

        val connector = mock[TelegramConnector]
        when(connector.getUpdates()).thenReturn(Future.successful(HttpResponse(StatusCodes.BadRequest)))
        whenReady(new TelegramConnection(connector).getNewMessages().failed) { _ =>
          assert(true)
        }
      }
    }
  }
}
