package controllers

import com.gilt.quality.models.{Team, TeamForm}
import com.gilt.quality.error.ErrorsResponse
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class TeamsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global
  lazy val org = createOrganization()

  "POST /teams" in new WithServer {
    val key = UUID.randomUUID.toString
    val team = createTeam(org, TeamForm(key = key))
    team.key must be(key)
  }

  "POST /teams validates that key cannot be reused" in new WithServer {
    val team = createTeam(org)

    intercept[ErrorsResponse] {
      createTeam(org, TeamForm(key = team.key))
    }.errors.map(_.message) must be (Seq(s"Team with key[${team.key}] already exists"))
  }

  "DELETE /teams/:key" in new WithServer {
    val team = createTeam(org)
    await(client.teams.deleteByOrgAndKey(org.key, team.key)) must be(Some(()))
    await(client.teams.getByOrg(org.key, key = Some(team.key))) must be(Seq.empty)
  }

  "GET /teams" in new WithServer {
    val team1 = createTeam(org)
    val team2 = createTeam(org)

    await(client.teams.getByOrg(org.key, key = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.teams.getByOrg(org.key, key = Some(team1.key))).head must be(team1)
    await(client.teams.getByOrg(org.key, key = Some(team2.key))).head must be(team2)
  }

  "GET /teams/:key" in new WithServer {
    val team = createTeam(org)
    await(client.teams.getByOrgAndKey(org.key, team.key)) must be(Some(team))
    await(client.teams.getByOrgAndKey(org.key, UUID.randomUUID.toString)) must be(None)
  }

}
