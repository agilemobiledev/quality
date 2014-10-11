package db

import com.gilt.quality.models.{AgendaItem, AgendaItemForm, Error, Incident, Meeting, Organization, Plan, Severity, Task, Team}
import lib.Validation
import anorm._
import anorm.ParameterValue._
import AnormHelper._
import play.api.db._
import play.api.Play.current
import org.joda.time.DateTime

case class FullAgendaItemForm(
  meeting: Meeting,
  form: AgendaItemForm
)

object AgendaItemsDao {

  private val BaseQuery = """
    select agenda_items.id, agenda_items,task,
           incidents.id as incident_id,
           organizations.key as organization_key, 
           organizations.name as organization_name,
           teams.key as team_key,
           incidents.severity as incident_severity,
           incidents.summary as incident_summary,
           incidents.description as incident_description,
           incidents.created_at as incident_created_at,
           plans.id as plan_id,
           plans.body as plan_body,
           plans.created_at as plan_created_at,
           grades.score as grade
      from agenda_items
      join incidents on incidents.id = agenda_items.incident_id and incidents.deleted_at is null
      join organizations on organizations.deleted_at is null and organizations.id = incidents.organization_id
      left join teams on teams.deleted_at is null and teams.id = incidents.team_id
      left join plans on plans.deleted_at is null and plans.incident_id = incidents.id
      left join grades on grades.deleted_at is null and grades.plan_id = plans.id
     where agenda_items.deleted_at is null
  """

  private val InsertQuery = """
    insert into agenda_items
    (meeting_id, task, incident_id, created_by_guid)
    values
    ({meeting_id}, {task}, {incident_id}, {user_guid}::uuid)
  """

  private val SoftDeleteAgendaItemsQuery = """
    update agenda_items
       set deleted_by_guid = {deleted_by_guid}::uuid, deleted_at = now(), updated_at = now()
     where meeting_id = {meeting_id}
       and deleted_at is null
  """

  def validate(fullForm: FullAgendaItemForm): Seq[Error] = {
    fullForm.form.task match {
      case Task.UNDEFINED(key) => Validation.error(s"Invalid task[$key]")
      case _ => Seq.empty
    }
  }

  def create(user: User, fullForm: FullAgendaItemForm): AgendaItem = {
    val errors = validate(fullForm)
    assert(errors.isEmpty, errors.map(_.message).mkString("\n"))

    val id: Long = DB.withTransaction { implicit c =>
      SQL(InsertQuery).on(
        'meeting_id -> fullForm.meeting.id,
        'task -> fullForm.form.task.toString,
        'incident_id -> fullForm.form.incidentId,
        'user_guid -> user.guid
      ).executeInsert().getOrElse(sys.error("Missing id"))
    }

    findById(id).getOrElse {
      sys.error("Failed to create agenda item")
    }
  }

  def softDelete(deletedBy: User, agendaItem: AgendaItem) {
    SoftDelete.delete("agenda_items", deletedBy, agendaItem.id)
  }

  private[db] def softDeleteAllForMeeting(
    implicit c: java.sql.Connection,
    deletedBy: User,
    meeting: Meeting
  ) {
    SQL(SoftDeleteAgendaItemsQuery).on('deleted_by_guid -> deletedBy.guid, 'meeting_id -> meeting.id).execute()
  }

  def findById(id: Long): Option[AgendaItem] = {
    findAll(id = Some(id), limit = 1).headOption
  }

  def findByMeetingIdAndId(
    meetingId: Long,
    id: Long
  ): Option[AgendaItem] = {
    findAll(id = Some(id), meetingId = Some(meetingId), limit = 1).headOption
  }

  def findAll(
    id: Option[Long] = None,
    meetingId: Option[Long] = None,
    incidentId: Option[Long] = None,
    task: Option[Task] = None,
    limit: Int = 50,
    offset: Int = 0
  ): Seq[AgendaItem] = {
    val sql = Seq(
      Some(BaseQuery.trim),
      id.map { v => "and agenda_items.id = {id}" },
      meetingId.map { v => "and agenda_items.meeting_id = {meeting_id}" },
      incidentId.map { v => "and agenda_items.incident_id = {incident_id}" },
      task.map { v => "and agenda_items.task = {task}" },
      Some("order by agenda_items.incident_id"),
      Some(s"limit ${limit} offset ${offset}")
    ).flatten.mkString("\n   ")

    val bind = Seq(
      id.map { v => NamedParameter("id", toParameterValue(v)) },
      meetingId.map { v => NamedParameter("meeting_id", toParameterValue(v)) },
      incidentId.map { v => NamedParameter("incident_id", toParameterValue(v)) },
      task.map { v => NamedParameter("task", toParameterValue(v.toString)) }
    ).flatten

    DB.withConnection { implicit c =>
      SQL(sql).on(bind: _*)().toList.map { row =>
        val incidentId = row[Long]("incident_id")

        val plan = row[Option[Long]]("plan_id").map { planId =>
          Plan(
            id = planId,
            incidentId = incidentId,
            body = row[String]("plan_body"),
            grade = row[Option[Int]]("grade"),
            createdAt = row[DateTime]("plan_created_at")
          )
        }

        AgendaItem(
          id = row[Long]("id"),
          task = Task(row[String]("task")),
          incident = Incident(
            id = incidentId,
            organization = Organization(
              key = row[String]("organization_key"),
              name = row[String]("organization_name")
            ),
            team = row[Option[String]]("team_key").map { teamKey =>
              Team(
                key = teamKey,
                organization = Organization(
                  key = row[String]("organization_key"),
                  name = row[String]("organization_name")
                )
              )
            },
            severity = Severity(row[String]("incident_severity")),
            summary = row[String]("incident_summary"),
            description = row[Option[String]]("incident_description"),
            tags = Seq.empty,
            plan = plan,
            createdAt = row[DateTime]("incident_created_at")
          )
        )
      }.toSeq
    }
  }

}