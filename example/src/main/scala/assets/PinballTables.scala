package assets

import example.states.*

object PinballTables {
  final case class PinballTableDescription(
      name: String,
      manufacturer: String,
      year: Int
  ) {
    def toPinballTable(
        id: Long,
        maxHealth: Int,
        currentHealth: Int,
        highScores: List[Score]
    ): PinballTable = PinballTable(
      id,
      name,
      manufacturer,
      maxHealth,
      currentHealth,
      highScores
    )
  }

  def table(
      name: String,
      manufacturer: String,
      year: Int
  ): PinballTableDescription =
    new PinballTableDescription(name, manufacturer, year)

  val all =
    table("Dungeons and Dragons", "Stern", 2025) ::
      table("Avatar", "Jersey Jack", 2024) ::
      table("Elton John", "Jersey Jack", 2024) ::
      table("Toy Story 4", "Jersey Jack", 2021) ::
      table("The Mandalorian", "Stern", 2019) ::
      table("Star Wars", "Stern", 2017) ::
      Nil
}
