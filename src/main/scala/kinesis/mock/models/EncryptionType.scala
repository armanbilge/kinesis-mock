package kinesis.mock.models

import enumeratum._

sealed trait EncryptionType extends EnumEntry

object EncryptionType extends Enum[EncryptionType] {
  override val values = findValues
  case object NONE extends EncryptionType
  case object KMS extends EncryptionType
}