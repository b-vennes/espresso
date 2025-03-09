$version: "2"

namespace example.events

structure Played {
  @required
  score: Long

  @required
  name: String
}

union PinballTableEvent {
  played: Played
}
