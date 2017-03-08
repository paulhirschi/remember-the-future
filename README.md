
# Remember the Future

## Disclaimer
Many of the examples used in this project are very contrived to illustrate the usage of `Future`s within
[Scala](http://www.scala-lang.org/) and the [Play! Framework](https://www.playframework.com/). Additionally, many
of the definitions were taken from the [current API](http://www.scala-lang.org/api/current/scala/concurrent/Future.html)
and the [Scala documentation](http://docs.scala-lang.org/overviews/core/futures.html).

 * ExecutionContext - For now we will just use the Global context `import scala.concurrent.ExecutionContext.Implicits.global`
 * Creation - `Future.apply`, `Future.successful` and `Future.failed`
 * Callbacks
   * onComplete
   * foreach
   * andThen
 * map - `Future[A] => Future[B]`
 * firstCompletedOf - `List[Future[A]] => Future[A]`
 * find - `List[Future[A]] => Future[Option[A]]`
 * sequence - `List[Future[A]] => Future[List[A]]`
 * traverse - `List[Future[A]] => Future[List[B]]`
 * fold (2.11), foldLeft (2.12)
 * transform
 * fallbackTo
 * recover and recoverWith
 * all together now!

### Postman Collection
In the test resources directory, there is a Postman collection to call each of the endpoints