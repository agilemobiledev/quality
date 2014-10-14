package db

import com.gilt.quality.models.{AgendaItem, AgendaItemForm, Incident, Meeting, MeetingForm, Organization, Task}
import org.joda.time.DateTime
import anorm._
import anorm.ParameterValue._
import db.AnormHelper._
import play.api.db._
import play.api.Play.current
import play.api.libs.json._

case class FullMeetingForm(
  org: Organization,
  form: MeetingForm
)

object MeetingsDao {

  private val BaseQuery = """
    select meetings.id, meetings.scheduled_at,
           organizations.key as organization_key, 
           organizations.name as organization_name
      from meetings
      join organizations on organizations.deleted_at is null and organizations.id = teams.organization_id
     where meetings.deleted_at is null
  """

  private val InsertQuery = """
    insert into meetings
    (organization_id, scheduled_at, created_by_guid)
    values
    ({organization_id}, {scheduled_at}, {user_guid}::uuid)
  """

  def create(user: User, fullForm: FullMeetingForm): Meeting = {
    val orgId = OrganizationsDao.lookupId(fullForm.org.key).getOrElse {
      sys.error(s"Could not find organizations with key[${fullForm.org.key}]")
    }

    val id: Long = DB.withConnection { implicit c =>
      SQL(InsertQuery).on(
        'organization_id -> orgId,
        'scheduled_at -> fullForm.form.scheduledAt,
        'user_guid -> user.guid
      ).executeInsert().getOrElse(sys.error("Missing id"))
    }

    findById(id).getOrElse {
      sys.error("Failed to create meeting")
    }
  }

  def softDelete(deletedBy: User, meeting: Meeting) {
    DB.withTransaction { implicit c =>
      SoftDelete.delete(c, "meetings", deletedBy, meeting.id)
      AgendaItemsDao.softDeleteAllForMeeting(c, deletedBy, meeting)
    }
  }

  def findById(id: Long): Option[Meeting] = {
    findAll(id = Some(id), limit = 1).headOption
  }

  def findByOrganizationAndId(org: Organization, id: Long): Option[Meeting] = {
    findAll(org = Some(org), id = Some(id), limit = 1).headOption
  }

  def upsert(
    org: Organization,
    scheduledAt: DateTime
  ): Meeting = {
    MeetingsDao.findAll(
      org = Some(org),
      scheduledAt = Some(scheduledAt),
      limit = 1
    ).headOption.getOrElse {
      MeetingsDao.create(
        User.Actor,
        FullMeetingForm(
          org,
          MeetingForm(
            scheduledAt = scheduledAt
          )
        )
      )
    }
  }

  // TODO: upsert is on meeting/incident... incident can only be in
  // one meeting at a time, but method signature doesn't indicate
  // this.
  def upsertAgendaItem(
    meeting: Meeting,
    incident: Incident,
    task: Task
  ): AgendaItem = {
    // TODO: Need to use row level locks or move to db
    AgendaItemsDao.findAll(
      meetingId = Some(meeting.id),
      incidentId = Some(incident.id),
      limit = 1
    ).headOption.getOrElse {
      try {
        AgendaItemsDao.create(
          User.Actor,
          FullAgendaItemForm(
            meeting,
            AgendaItemForm(
              incidentId = incident.id,
              task = task
            )
          )
        )
      } catch {
        case e: org.postgresql.util.PSQLException => {
          if (e.getMessage.startsWith("""ERROR: duplicate key value violates unique constraint "agenda_items_meeting_id_incident_id_idx"""")) {
            AgendaItemsDao.findAll(
              meetingId = Some(meeting.id),
              incidentId = Some(incident.id),
              limit = 1
            ).headOption.getOrElse {
              throw e
            }
          } else {
            throw e
          }
        }
        case e: Throwable => throw e
      }
    }
  }

  def findAll(
    org: Option[Organization] = None,
    id: Option[Long] = None,
    incidentId: Option[Long] = None,
    agendaItemId: Option[Long] = None,
    scheduledAt: Option[DateTime] = None,
    scheduledWithinNHours: Option[Int] = None,
    isUpcoming: Option[Boolean] = None,
    limit: Int = 50,
    offset: Int = 0
  ): Seq[Meeting] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      org.map { v => "and meetings.organization_id = (select id from organizations where deleted_at is null and key = {org_key})" },
      id.map { v => "and meetings.id = {id}" },
      incidentId.map { v => "and meetings.id in (select meeting_id from agenda_items where deleted_at is null and incident_id = {incident_id})" },
      agendaItemId.map { v => "and meetings.id in (select meeting_id from agenda_items where deleted_at is null and id = {agenda_item_id})" },
      scheduledAt.map { v => "and date_trunc('minute', meetings.scheduled_at) = date_trunc('minute', {scheduled_at}::timestamptz)" },
      scheduledWithinNHours.map { v => s"and meetings.scheduled_at between now() - interval '${v} hours' and now() + interval '${v} hours'" },
      isUpcoming.map { v =>
        v match {
          case true => "and meetings.scheduled_at > now()"
          case false => "and meetings.scheduled_at <= now()"
        }
      },
      Some("order by meetings.scheduled_at desc"),
      Some(s"limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")


    val bind = Seq(
      org.map { v => NamedParameter("org_key", toParameterValue(v.key)) },
      id.map { v => NamedParameter("id", toParameterValue(v)) },
      incidentId.map { v => NamedParameter("incident_id", toParameterValue(v)) },
      agendaItemId.map { v => NamedParameter("agenda_item_id", toParameterValue(v)) },
      scheduledAt.map { v => NamedParameter("scheduled_at", toParameterValue(v)) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        Meeting(
          id = row[Long]("id"),
          scheduledAt = row[DateTime]("scheduled_at"),
          organization = Organization(
            key = row[String]("organization_key"),
            name = row[String]("organization_name")
          )
        )
      }.toSeq
    }
  }

}
