package controllers

import javax.inject._

import akka.actor.ActorSystem
import com.shuttj.actors.LoggingActor
import com.shuttj.actors.LoggingActor.LogFailedFuture
import com.shuttj.repository._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

//noinspection TypeAnnotation
@Singleton
class HomeController @Inject()(ws: WSClient, actorSystem: ActorSystem) extends Controller {

  private val thirdPartyRepo: MyThirdPartyLibraryRepositoryWrapper = new MyThirdPartyLibraryRepositoryWrapper(new ThirdPartyLibraryRepository)
  private val loggingActor = actorSystem.actorOf(LoggingActor.props, "logging-actor")

  private def happyFuture(i: Int = Random.nextInt(100)): Future[String] = Future.successful(i.toString)

  private def bleakFuture: Future[String] = Future.failed(new Exception("The forecast looks bleak!"))

  private def randomFuture: Future[String] = Future.apply {
    val i = Random.nextInt(100)
    if (i % 2 == 0) throw RandomFutureException("Not your lucky day")
    else i.toString
  }

  def index = Action.async {
    Future(Ok("Welcome to the future!"))
  }

  def creation = Action.async {
//    val future: Future[String] = happyFuture()
//    val future: Future[String] = bleakFuture
    val future: Future[String] = randomFuture

    future.map { i => Ok(i) }
  }

  def callbacks = Action.async {
    mark("callbacks")
    val future = randomFuture

    //NOTE: onSuccess and onFailure were deprecated in 2.12, use onComplete or foreach
    future.onComplete {
      case Success(s) => println(s"Received [$s] in onComplete")
      case Failure(t) => loggingActor ! LogFailedFuture(t.getMessage)
    }

    //Note: foreach is only called on a successful future
    future.foreach { s =>
      println(s"Received [$s] in foreach")
    }

    future.map { value =>
      println("going to sleep")
      Thread.sleep(4000)
      println("done sleeping")
      Ok(value)
    }
  }

  def andThen = Action.async {
    mark("andThen")
    val futureInt = randomFuture

    val stillFutureInt: Future[String] = futureInt andThen {
      case Success(int) => println(s"In first andThen success $int")
    } andThen {
      case Success(int) => println(s"In second andThen success $int")
    } andThen {
      case Failure(t) =>
        println("In Failure andThen")
        println(t)
    }

    stillFutureInt.map { i => Ok(i) }
  }

  def firstCompletedOf = Action.async {
    mark("first")

    val readyToGoRequests: List[WSRequest] = getWSRequests(5)
    val executedRequests: List[Future[WSResponse]] = readyToGoRequests.par.map(_.get).toList

    Future.firstCompletedOf(executedRequests).map { response =>
      Ok(response.body)
    }
  }

  def find = Action.async {
    mark("find")

    val readyToGoRequests: List[WSRequest] = getWSRequests(3)
    val executedRequests: List[Future[WSResponse]] = readyToGoRequests.par.map(_.get).toList

    val luckyNumber3000: Future[Option[WSResponse]] = Future.find(executedRequests) { request =>
      request.status == 200 && request.body == "3000"
    }

    luckyNumber3000.map { optRequest =>
      optRequest.fold(BadRequest("Sorry, no 3000 today")) { _ =>
        Ok("Found lucky # 3000!")
      }
    }
  }

  def sequence = Action.async {
    mark("sequence")

    val readyToGoRequests: List[WSRequest] = getWSRequests(3)
    val executedRequests: List[Future[WSResponse]] = readyToGoRequests.par.map(_.get).toList

    val allResponses: Future[List[WSResponse]] = Future.sequence(executedRequests)

    val sum: Future[Long] = allResponses.map { responses =>
      val longs: List[Long] = responses.map { wsResponseToLong }
      longs.sum
    }

    sum.map { l => Ok(l.toString) }
  }

  def traverse = Action.async {
    mark("traverse")

    val readyToGoRequests: List[WSRequest] = getWSRequests(3)
    val executedRequests: List[Future[WSResponse]] = readyToGoRequests.par.map(_.get).toList

    val allResponsesAsLongs: Future[List[Long]] = Future.traverse(executedRequests) { futureResponse =>
      futureResponse.map { wsResponseToLong }
    }

    val sum: Future[Long] = allResponsesAsLongs.map { list => list.sum }

    sum.map { l => Ok(l.toString) }
  }

  def fold = Action.async {
    mark("fold")

    val futureInts: List[Future[Int]] = List.fill(5) { happyFuture().map { _.toInt } }

    //NOTE: fold has been deprecated and replaced with foldLeft in Scala 2.12
    //Also note that "fold" here is not the same as "fold" on other structures, like Option, hence the deprecation and change to foldLeft
    val futureSum: Future[Int] = Future.fold(futureInts)(0){ (i1, i2) => i1 + i2}

    futureSum.map { sum =>
      Ok(sum.toString)
    }
  }

  def transform(i: Int) = Action.async {
    mark("transform")
    val futureLargeCalcResult: Future[LargeCalculationResult] = thirdPartyRepo.largeCalculation(i)

    futureLargeCalcResult.map { result =>
      Ok(result.result)
    }
  }

  def fallbackTo = Action.async {
    mark("fallbackTo")
    val future1: Future[String] = randomFuture
    val future2: Future[String] = randomFuture.transform(identity, _ => new Exception("Still not your lucky day. :("))
    Thread.sleep(100)
    println(future1.value)
    println(future2.value)

    val doubleYourChances: Future[String] = future1 fallbackTo future2

    doubleYourChances.map { i => Ok(s"w00t! Lucky # $i")}
  }

  def kitchenSink(customerId: Long) = Action.async {
    mark("kitchenSink")

    type Formatter = (String, Int, Long) => String

    val futureString: Future[String] = randomFuture
//    val futureString: Future[String] = happyFuture()
    val futureOptionInt: Future[Option[Int]] = Future.successful(Some(14))
    val tryIt: Try[Long] = Try {
      10
    }
        val optionFormat: Option[Formatter] = None
//    val optionFormat: Option[Formatter] = Some((s, i, l) => s"Received String of $s, int of $i, and long of $l")

    val futureMessage: Future[String] = for {
      string <- futureString
      int <- fromFutureOption(futureOptionInt)(IntException("unable to locate int"))
      long <- Future.fromTry(tryIt)
      formatter <- fromOption(optionFormat)(FormatterException("unable to locate formatter"))
    } yield formatter(string, int, long)

    futureMessage.map { message =>
      Ok(message)
    } recover {
      randomFutureExceptionPF orElse formatterExceptionPF
    }
  }

  def delay = Action.async {
    val delay = (Random.nextInt(10) * 1000) + 1000
    println(s"Delay of $delay")
    Thread.sleep(delay)
    Future.successful(Ok(delay.toString))
  }

  private def wsResponseToLong(response: WSResponse): Long = response.body.toLong

  private def mark(name: String) = println(s"\n\n======= $name =======")

  private def getWSRequests(max: Int): List[WSRequest] = List.fill(max) {
    ws.url(s"http://localhost:9000/delay")
  }

  //----------------- Future Conversions -----------------

  private def fromOption[A](option: Option[A])(exception: => Exception): Future[A] =
    option match {
      case None => Future.failed(exception)
      case Some(a) => Future.successful(a)
    }

  private def fromFutureOption[A](futureOption: Future[Option[A]])
                                 (exception: => Exception)
                                 (implicit executionContext: ExecutionContext): Future[A] =
    futureOption.flatMap { option => fromOption(option)(exception) }

  //----------------- Partial Functions -----------------

  private val randomFutureExceptionPF: PartialFunction[Throwable, Result] = {
    case rfe: RandomFutureException => BadRequest(rfe.msg)
  }

  private val formatterExceptionPF: PartialFunction[Throwable, Result] = {
    case fe: FormatterException => InternalServerError(fe.msg)
  }

  private val intExceptionPF: PartialFunction[Throwable, Result] = {
    case ie: IntException => NotFound(ie.msg)
  }

  private val allTogetherNow: PartialFunction[Throwable, Result] =
    randomFutureExceptionPF orElse formatterExceptionPF orElse intExceptionPF
}

case class RandomFutureException(msg: String) extends Exception(msg)
case class FormatterException(msg: String) extends Exception(msg)
case class IntException(msg: String) extends Exception(msg)