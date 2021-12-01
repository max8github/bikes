package akka.sample.bikes

/**
 * See the README.md for starting each node with sbt.
 */
object Main {

  def main(args: Array[String]): Unit = if (runningLocally) MainLocal.run(args) else MainKubernetes.run(args)

  private def runningLocally = sys.env.getOrElse("RUN_LOCALLY", "true").toBoolean
}
