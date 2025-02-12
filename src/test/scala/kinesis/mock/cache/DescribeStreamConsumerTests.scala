package kinesis.mock.cache

import scala.concurrent.duration._

import cats.effect.IO
import cats.syntax.all._
import enumeratum.scalacheck._
import org.scalacheck.Test
import org.scalacheck.effect.PropF

import kinesis.mock.LoggingContext
import kinesis.mock.api._
import kinesis.mock.instances.arbitrary._
import kinesis.mock.models._

class DescribeStreamConsumerTests
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite {

  override def scalaCheckTestParameters: Test.Parameters =
    Test.Parameters.default.withMinSuccessfulTests(5)

  test("It should describe a stream consumer")(PropF.forAllF {
    (
        streamName: StreamName,
        consumerName: ConsumerName,
        awsRegion: AwsRegion
    ) =>
      for {
        cacheConfig <- CacheConfig.read
        cache <- Cache(cacheConfig)
        context = LoggingContext.create
        streamArn = StreamArn(awsRegion, streamName, cacheConfig.awsAccountId)
        _ <- cache
          .createStream(
            CreateStreamRequest(Some(1), None, streamName),
            context,
            false,
            Some(awsRegion)
          )
          .rethrow
        _ <- IO.sleep(cacheConfig.createStreamDuration.plus(400.millis))
        registerRes <- cache
          .registerStreamConsumer(
            RegisterStreamConsumerRequest(
              consumerName,
              streamArn
            ),
            context,
            false
          )
          .rethrow

        res <- cache
          .describeStreamConsumer(
            DescribeStreamConsumerRequest(
              None,
              Some(consumerName),
              Some(streamArn)
            ),
            context,
            false
          )
          .rethrow
      } yield assert(
        ConsumerSummary.fromConsumer(
          res.consumerDescription
        ) === registerRes.consumer && res.consumerDescription.streamArn == streamArn,
        s"$registerRes\n$res"
      )
  })
}
