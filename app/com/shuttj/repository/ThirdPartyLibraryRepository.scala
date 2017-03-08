package com.shuttj.repository

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

case class ThirdPartyLargeCalculationResult(result: Int)
case class ThirdPartyIllegalArgumentException(msg: String) extends Exception(msg)

class ThirdPartyLibraryRepository {

  def largeCalculation(i: Int): Future[ThirdPartyLargeCalculationResult] = Future {
    if (i % 2 == 0) throw ThirdPartyIllegalArgumentException("You should be arrested!")
    else ThirdPartyLargeCalculationResult(i * 100)
  }
}

// Third Party Wrapper
class MyThirdPartyLibraryRepositoryWrapper(repo: ThirdPartyLibraryRepository) {

  def largeCalculation(i: Int)(implicit executionContext: ExecutionContext): Future[LargeCalculationResult] =
    repo.largeCalculation(i).transform (
      thirdPartyResult => LargeCalculationResult(thirdPartyResult.result.toString)
        , throwable => LargeCalculationException(throwable.getMessage)
    )
}

case class LargeCalculationResult(result: String)
case class LargeCalculationException(msg: String) extends Exception(msg)