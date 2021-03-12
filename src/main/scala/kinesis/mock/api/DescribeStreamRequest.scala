package kinesis.mock
package api

import cats.data.Validated._
import cats.data._
import cats.syntax.all._
import io.circe._

import kinesis.mock.models._

// https://docs.aws.amazon.com/kinesis/latest/APIReference/API_DescribeStream.html
final case class DescribeStreamRequest(
    exclusiveStartShardId: Option[String],
    limit: Option[Int],
    streamName: String
) {
  def describeStream(
      streams: Streams
  ): ValidatedNel[KinesisMockException, DescribeStreamResponse] =
    CommonValidations
      .findStream(streamName, streams)
      .andThen(stream =>
        (
          CommonValidations.validateStreamName(streamName),
          exclusiveStartShardId match {
            case Some(shardId) => CommonValidations.validateShardId(shardId)
            case None          => Valid(())
          },
          limit match {
            case Some(l) if (l < 1 || l > 10000) =>
              InvalidArgumentException(
                s"Limit must be between 1 and 10000"
              ).invalidNel
            case _ => Valid(())
          }
        ).mapN((_, _, _) =>
          DescribeStreamResponse(
            StreamDescription
              .fromStreamData(stream, exclusiveStartShardId, limit)
          )
        )
      )
}

object DescribeStreamRequest {
  implicit val DescribeStreamRequestEncoder: Encoder[DescribeStreamRequest] =
    Encoder.forProduct3("ExclusiveStartShardId", "Limit", "StreamName")(x =>
      (x.exclusiveStartShardId, x.limit, x.streamName)
    )
  implicit val DescribeStreamRequestDecoder: Decoder[DescribeStreamRequest] = {
    x =>
      for {
        exclusiveStartShardId <- x
          .downField("ExclusiveStartShardId")
          .as[Option[String]]
        limit <- x.downField("Limit").as[Option[Int]]
        streamName <- x.downField("StreamName").as[String]
      } yield DescribeStreamRequest(exclusiveStartShardId, limit, streamName)
  }
}