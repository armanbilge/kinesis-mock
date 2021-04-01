package kinesis.mock.cache

import cats.effect.{Blocker, IO}
import cats.syntax.all._
import org.scalacheck.Test
import org.scalacheck.effect.PropF

import kinesis.mock.LoggingContext
import kinesis.mock.api._
import kinesis.mock.instances.arbitrary._
import kinesis.mock.models._

class AddTagsToStreamTests
    extends munit.CatsEffectSuite
    with munit.ScalaCheckEffectSuite {

  override def scalaCheckTestParameters: Test.Parameters =
    Test.Parameters.default.withMinSuccessfulTests(5)

  test("It should add tags")(PropF.forAllF {
    (
        streamName: StreamName,
        tags: Tags
    ) =>
      Blocker[IO].use(blocker =>
        for {
          cacheConfig <- CacheConfig.read(blocker)
          cache <- Cache(cacheConfig)
          context = LoggingContext.create
          _ <- cache
            .createStream(CreateStreamRequest(1, streamName), context)
            .rethrow
          _ <- cache
            .addTagsToStream(
              AddTagsToStreamRequest(streamName, tags),
              context
            )
            .rethrow
          res <- cache
            .listTagsForStream(
              ListTagsForStreamRequest(None, None, streamName),
              context
            )
            .rethrow
        } yield assert(res.tags == tags)
      )
  })
}