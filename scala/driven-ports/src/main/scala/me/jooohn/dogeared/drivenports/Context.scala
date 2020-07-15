package me.jooohn.dogeared.drivenports

case class Context(
    logger: Logger
) {

  def withAttributes(mapping: (String, Any)*): Context = copy(logger = logger.withContext(mapping: _*))

}
