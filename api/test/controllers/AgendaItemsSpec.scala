package controllers

import com.gilt.quality.FailedRequest
import com.gilt.quality.models.{AgendaItem, AgendaItemForm, Task}
import com.gilt.quality.error.ErrorsResponse
import org.joda.time.DateTime
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class AgendaItemsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "POST /:org/meetings/:meeting_id/agenda_items" in new WithServer {
    val meeting = createMeeting(org)
    val incident = createIncident(org)
    val item = createAgendaItem(
      org = org,
      meeting = Some(meeting),
      form = Some(
        AgendaItemForm(
          incidentId = incident.id,
          task = Task.ReviewTeam
        )
      )
    )

    item.incident.id must be(incident.id)
    item.task must be(Task.ReviewTeam)
  }

  "POST /:org/meetings/:meeting_id/agenda_items validates task" in new WithServer {
    val meeting = createMeeting(org)
    val incident = createIncident(org)

    intercept[ErrorsResponse] {
      createAgendaItem(
        org = org,
        meeting = Some(meeting),
        form = Some(
          AgendaItemForm(
            incidentId = incident.id,
            task = Task.UNDEFINED("foo")
          )
        )
      )
    }.errors.map(_.message) must be (Seq("Invalid task[foo]"))
  }

  "GET /meetings/:meeting_id/agenda_items filters by meeting, agenda item" in new WithServer {
    val org = createOrganization()
    val meeting = createMeeting(org)
    val item1 = createAgendaItem(org = org, meeting = Some(meeting))
    val item2 = createAgendaItem(org = org, meeting = Some(meeting))

    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id)).map(_.id).sorted must be(Seq(item1.id, item2.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id, id = Some(item1.id))).map(_.id) must be(Seq(item1.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id, id = Some(item2.id))).map(_.id) must be(Seq(item2.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id, id = Some(-1))).map(_.id) must be(Seq.empty)
  }

  "GET /meetings/:meeting_id/agenda_items is 404 if org not found" in new WithServer {
    val meeting = createMeeting(org)
    intercept[FailedRequest] {
      await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(UUID.randomUUID.toString, meetingId = meeting.id))
    }.response.status must be(404)
  }

  "GET /meetings/:meeting_id/agenda_items is 404 if meeting not found" in new WithServer {
    intercept[FailedRequest] {
      await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meetingId = -1))
    }.response.status must be(404)
  }

  "GET /meetings/:meeting_id/agenda_items paginates" in new WithServer {
    val org = createOrganization()
    val meeting = createMeeting(org)
    val item1 = createAgendaItem(org = org, meeting = Some(meeting))
    val item2 = createAgendaItem(org = org, meeting = Some(meeting))

    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id, limit = Some(1))).map(_.id).sorted must be(Seq(item1.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id, limit = Some(1), offset = Some(1))).map(_.id).sorted must be(Seq(item2.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingId(org.key, meeting.id, limit = Some(1), offset = Some(2))).map(_.id).sorted must be(Seq.empty)
  }

  "GET /meetings/:meeting_id/agenda_items/:id" in new WithServer {
    val meeting = createMeeting(org)
    val item = createAgendaItem(org = org, meeting = Some(meeting))

    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingIdAndId(org.key, meeting.id, item.id)).map(_.id) must be(Some(item.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingIdAndId(org.key, meeting.id, -1)).map(_.id) must be(None)
  }

  "DELETE /meetings/:meeting_id/agenda_items/:id" in new WithServer {
    val meeting = createMeeting(org)
    val item = createAgendaItem(org = org, meeting = Some(meeting))

    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingIdAndId(org.key, meeting.id, item.id)).map(_.id) must be(Some(item.id))
    await(client.agendaItems.deleteMeetingsAndAgendaItemsByOrgAndMeetingIdAndId(org.key, meeting.id, item.id))
    await(client.agendaItems.getMeetingsAndAgendaItemsByOrgAndMeetingIdAndId(org.key, meeting.id, item.id)).map(_.id) must be(None)
  }

}