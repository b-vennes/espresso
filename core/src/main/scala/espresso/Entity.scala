package espresso

case class Entity[S, EV](
  id: String,
  entityType: EntityType[S, EV],
) {
  lazy val streamID: String = s"${entityType.name}_$id"
}

extension [S, EV](e: Entity[S, EV]) {
  def emit(event: EV): Saga[Unit] = 
      Saga.emit(event, e)

  def getState(): Saga[S] = Saga.getState(e)
}
