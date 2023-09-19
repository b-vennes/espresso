package espresso

import smithy4s.http.json
import smithy4s.schema.Schema

case class EntityType[S, EV](
  name: String,
  default: S,
  merge: (S, EV) => S,
  encode: EV => String,
  decode: String => EV,
) {
  def withId(id: String): Entity[S, EV] =
    Entity(id, this)
}

object EntityType {

  def withJsonEncoding[S, EV](
    name: String,
    default: S,
    merge: (S, EV) => S
  )(using EV: Schema[EV]): EntityType[S, EV] =
    val jsonCodec = json.codecs()
    val codec = jsonCodec.compileCodec(EV)
    EntityType(
      name,
      default,
      merge,
      value => new String(jsonCodec.writeToArray(codec, value)),
      data => jsonCodec.decodeFromByteArray(codec, data.getBytes()).toOption.get
    )

  def assemble[S, EV](
    entityType: EntityType[S, EV],
    events: List[Event],
  ): S =
    events
      .map(e => entityType.decode(e.data))
      .foldLeft(entityType.default)(entityType.merge)
}
