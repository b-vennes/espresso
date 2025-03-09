$version: "2"

namespace example.states

structure Score {
  @required
  name: String

  @required
  value: Long
}

list Scores {
  member: Score
}

structure PinballTable {
  @required
  name: String

  @required
  manufacturer: String

  @required
  maxHealth: Integer

  @required
  currentHealth: Integer

  @required
  highScores: Scores
}
