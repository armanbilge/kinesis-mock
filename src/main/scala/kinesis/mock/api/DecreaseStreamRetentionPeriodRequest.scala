package kinesis.mock
package api

import scala.concurrent.duration._

import cats.Eq
import cats.effect.{IO, Ref}
import cats.syntax.all._
import io.circe

import kinesis.mock.models._
import kinesis.mock.syntax.either._
import kinesis.mock.validations.CommonValidations

// https://docs.aws.amazon.com/kinesis/latest/APIReference/API_DecreaseStreamRetention.html
final case class DecreaseStreamRetentionPeriodRequest(
    retentionPeriodHours: Int,
    streamName: Option[StreamName],
    streamArn: Option[StreamArn]
) {
  def decreaseStreamRetention(
      streamsRef: Ref[IO, Streams],
      awsRegion: AwsRegion,
      awsAccountId: AwsAccountId
  ): IO[Response[Unit]] = streamsRef.modify { streams =>
    CommonValidations
      .getStreamNameArn(streamName, streamArn, awsRegion, awsAccountId)
      .flatMap { case (name, arn) =>
        CommonValidations
          .validateStreamName(name)
          .flatMap(_ =>
            CommonValidations
              .findStream(arn, streams)
              .flatMap(stream =>
                (
                  CommonValidations
                    .validateRetentionPeriodHours(retentionPeriodHours),
                  CommonValidations.isStreamActive(arn, streams),
                  if (stream.retentionPeriod.toHours < retentionPeriodHours)
                    InvalidArgumentException(
                      s"Provided RetentionPeriodHours $retentionPeriodHours is greater than the currently defined retention period ${stream.retentionPeriod.toHours}"
                    ).asLeft
                  else Right(())
                ).mapN((_, _, _) => stream)
              )
          )
          .map(stream =>
            (
              streams.updateStream(
                stream.copy(retentionPeriod = retentionPeriodHours.hours)
              ),
              ()
            )
          )
      }
      .sequenceWithDefault(streams)
  }
}

object DecreaseStreamRetentionPeriodRequest {
  implicit val decreaseStreamRetentionPeriodRequestCirceEncoder
      : circe.Encoder[DecreaseStreamRetentionPeriodRequest] =
    circe.Encoder.forProduct3(
      "RetentionPeriodHours",
      "StreamName",
      "StreamARN"
    )(x => (x.retentionPeriodHours, x.streamName, x.streamArn))
  implicit val decreaseStreamRetentionPeriodRequestCirceDecoder
      : circe.Decoder[DecreaseStreamRetentionPeriodRequest] = { x =>
    for {
      retentionPeriodHours <- x.downField("RetentionPeriodHours").as[Int]
      streamName <- x.downField("StreamName").as[Option[StreamName]]
      streamArn <- x.downField("StreamARN").as[Option[StreamArn]]
    } yield DecreaseStreamRetentionPeriodRequest(
      retentionPeriodHours,
      streamName,
      streamArn
    )
  }
  implicit val decreaseStreamRetentionPeriodRequestEncoder
      : Encoder[DecreaseStreamRetentionPeriodRequest] =
    Encoder.derive
  implicit val decreaseStreamRetentionPeriodRequestDecoder
      : Decoder[DecreaseStreamRetentionPeriodRequest] =
    Decoder.derive
  implicit val decreaseStreamRetentionEq
      : Eq[DecreaseStreamRetentionPeriodRequest] =
    Eq.fromUniversalEquals
}
