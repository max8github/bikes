package akka.sample.bikes

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ShaValidationSpec extends AnyWordSpec with Matchers with ScalaFutures {

  "BikeService validation" should {
    "verify sha1 and sha256" in {
      def isValidSHA1(s: String) = s.matches("^[a-fA-F0-9]{40}$")

      def isValidSHA256(s: String) = s.matches("^sha256:([a-fA-F0-9]){64}$")

      val sha1 = "f1b5feb0e6e2061a240f27a7a3815e8fc3ac2f72"
      val sha256 = "sha256:15e5e9c6383cf15101aefee3f345d0b9b8c9e98d5aea1b391a1fe5fc4390ca60"

      isValidSHA1(sha1) shouldBe true
      isValidSHA256(sha256) shouldBe true

      isValidSHA1("master") shouldBe false
      isValidSHA256("myname/dockernameofsort:2.9.14") shouldBe false
    }
  }
}
