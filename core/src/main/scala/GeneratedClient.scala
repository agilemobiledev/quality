package quality.models {
  case class Error(
    code: String,
    message: String
  )

  /**
   * Represents something that has happened - e.g. a team was created, an incident
   * created, a plan updated, etc.
   */
  case class Event(
    model: Model,
    action: Action,
    timestamp: org.joda.time.DateTime,
    url: scala.Option[String] = None,
    data: EventData
  )

  /**
   * Generic, descriptive data about a specific event
   */
  case class EventData(
    modelId: Long,
    summary: String
  )

  case class Healthcheck(
    status: String
  )

  /**
   * A bug or error that affected public or internal users in a negative way
   */
  case class Incident(
    id: Long,
    summary: String,
    description: scala.Option[String] = None,
    team: scala.Option[Team] = None,
    severity: Severity,
    tags: scala.collection.Seq[String] = Nil,
    plan: scala.Option[Plan] = None,
    createdAt: org.joda.time.DateTime
  )

  /**
   * Details for how an incident will be resolved
   */
  case class Plan(
    id: Long,
    incidentId: Long,
    body: String,
    grade: scala.Option[Int] = None,
    createdAt: org.joda.time.DateTime
  )

  /**
   * Statistics on each team's quality metrics, number of issues
   */
  case class Statistic(
    team: Team,
    totalGrades: Long,
    averageGrade: scala.Option[Int] = None,
    totalOpenIncidents: Long,
    totalIncidents: Long,
    totalPlans: Long,
    plans: scala.collection.Seq[Plan] = Nil
  )

  /**
   * A team is the main actor in the system. Teams have a unique key and own
   * incidents
   */
  case class Team(
    key: String
  )

  /**
   * Used in the event system to indicate what happened.
   */
  sealed trait Action

  object Action {

    /**
     * Indicates that an instance of this model was created
     */
    case object Created extends Action { override def toString = "created" }
    /**
     * Indicates that an instance of this model was updated
     */
    case object Updated extends Action { override def toString = "updated" }
    /**
     * Indicates that an instance of this model was deleted
     */
    case object Deleted extends Action { override def toString = "deleted" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Action

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Created, Updated, Deleted)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Action = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Action] = byName.get(value)

  }

  /**
   * The name of the model that was the subject of the event
   */
  sealed trait Model

  object Model {

    case object Incident extends Model { override def toString = "incident" }
    case object Plan extends Model { override def toString = "plan" }
    case object Rating extends Model { override def toString = "rating" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Model

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Incident, Plan, Rating)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Model = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Model] = byName.get(value)

  }

  sealed trait Severity

  object Severity {

    case object Low extends Severity { override def toString = "low" }
    case object High extends Severity { override def toString = "high" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Severity

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Low, High)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Severity = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Severity] = byName.get(value)

  }
}

package quality.models {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[quality] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[quality] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[quality] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[quality] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsQualityEnum_Action = __.read[String].map(Action.apply)
    implicit val jsonWritesQualityEnum_Action = new Writes[Action] {
      def writes(x: Action) = JsString(x.toString)
    }

    implicit val jsonReadsQualityEnum_Model = __.read[String].map(Model.apply)
    implicit val jsonWritesQualityEnum_Model = new Writes[Model] {
      def writes(x: Model) = JsString(x.toString)
    }

    implicit val jsonReadsQualityEnum_Severity = __.read[String].map(Severity.apply)
    implicit val jsonWritesQualityEnum_Severity = new Writes[Severity] {
      def writes(x: Severity) = JsString(x.toString)
    }
    implicit def jsonReadsQualityError: play.api.libs.json.Reads[Error] = {
      (
        (__ \ "code").read[String] and
        (__ \ "message").read[String]
      )(Error.apply _)
    }

    implicit def jsonWritesQualityError: play.api.libs.json.Writes[Error] = {
      (
        (__ \ "code").write[String] and
        (__ \ "message").write[String]
      )(unlift(Error.unapply _))
    }

    implicit def jsonReadsQualityEvent: play.api.libs.json.Reads[Event] = {
      (
        (__ \ "model").read[Model] and
        (__ \ "action").read[Action] and
        (__ \ "timestamp").read[org.joda.time.DateTime] and
        (__ \ "url").readNullable[String] and
        (__ \ "data").read[EventData]
      )(Event.apply _)
    }

    implicit def jsonWritesQualityEvent: play.api.libs.json.Writes[Event] = {
      (
        (__ \ "model").write[Model] and
        (__ \ "action").write[Action] and
        (__ \ "timestamp").write[org.joda.time.DateTime] and
        (__ \ "url").write[scala.Option[String]] and
        (__ \ "data").write[EventData]
      )(unlift(Event.unapply _))
    }

    implicit def jsonReadsQualityEventData: play.api.libs.json.Reads[EventData] = {
      (
        (__ \ "model_id").read[Long] and
        (__ \ "summary").read[String]
      )(EventData.apply _)
    }

    implicit def jsonWritesQualityEventData: play.api.libs.json.Writes[EventData] = {
      (
        (__ \ "model_id").write[Long] and
        (__ \ "summary").write[String]
      )(unlift(EventData.unapply _))
    }

    implicit def jsonReadsQualityHealthcheck: play.api.libs.json.Reads[Healthcheck] = {
      (__ \ "status").read[String].map { x => new Healthcheck(status = x) }
    }

    implicit def jsonWritesQualityHealthcheck: play.api.libs.json.Writes[Healthcheck] = new play.api.libs.json.Writes[Healthcheck] {
      def writes(x: Healthcheck) = play.api.libs.json.Json.obj(
        "status" -> play.api.libs.json.Json.toJson(x.status)
      )
    }

    implicit def jsonReadsQualityIncident: play.api.libs.json.Reads[Incident] = {
      (
        (__ \ "id").read[Long] and
        (__ \ "summary").read[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "team").readNullable[Team] and
        (__ \ "severity").read[Severity] and
        (__ \ "tags").readNullable[scala.collection.Seq[String]].map(_.getOrElse(Nil)) and
        (__ \ "plan").readNullable[Plan] and
        (__ \ "created_at").read[org.joda.time.DateTime]
      )(Incident.apply _)
    }

    implicit def jsonWritesQualityIncident: play.api.libs.json.Writes[Incident] = {
      (
        (__ \ "id").write[Long] and
        (__ \ "summary").write[String] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "team").write[scala.Option[Team]] and
        (__ \ "severity").write[Severity] and
        (__ \ "tags").write[scala.collection.Seq[String]] and
        (__ \ "plan").write[scala.Option[Plan]] and
        (__ \ "created_at").write[org.joda.time.DateTime]
      )(unlift(Incident.unapply _))
    }

    implicit def jsonReadsQualityPlan: play.api.libs.json.Reads[Plan] = {
      (
        (__ \ "id").read[Long] and
        (__ \ "incident_id").read[Long] and
        (__ \ "body").read[String] and
        (__ \ "grade").readNullable[Int] and
        (__ \ "created_at").read[org.joda.time.DateTime]
      )(Plan.apply _)
    }

    implicit def jsonWritesQualityPlan: play.api.libs.json.Writes[Plan] = {
      (
        (__ \ "id").write[Long] and
        (__ \ "incident_id").write[Long] and
        (__ \ "body").write[String] and
        (__ \ "grade").write[scala.Option[Int]] and
        (__ \ "created_at").write[org.joda.time.DateTime]
      )(unlift(Plan.unapply _))
    }

    implicit def jsonReadsQualityStatistic: play.api.libs.json.Reads[Statistic] = {
      (
        (__ \ "team").read[Team] and
        (__ \ "total_grades").read[Long] and
        (__ \ "average_grade").readNullable[Int] and
        (__ \ "total_open_incidents").read[Long] and
        (__ \ "total_incidents").read[Long] and
        (__ \ "total_plans").read[Long] and
        (__ \ "plans").readNullable[scala.collection.Seq[Plan]].map(_.getOrElse(Nil))
      )(Statistic.apply _)
    }

    implicit def jsonWritesQualityStatistic: play.api.libs.json.Writes[Statistic] = {
      (
        (__ \ "team").write[Team] and
        (__ \ "total_grades").write[Long] and
        (__ \ "average_grade").write[scala.Option[Int]] and
        (__ \ "total_open_incidents").write[Long] and
        (__ \ "total_incidents").write[Long] and
        (__ \ "total_plans").write[Long] and
        (__ \ "plans").write[scala.collection.Seq[Plan]]
      )(unlift(Statistic.unapply _))
    }

    implicit def jsonReadsQualityTeam: play.api.libs.json.Reads[Team] = {
      (__ \ "key").read[String].map { x => new Team(key = x) }
    }

    implicit def jsonWritesQualityTeam: play.api.libs.json.Writes[Team] = new play.api.libs.json.Writes[Team] {
      def writes(x: Team) = play.api.libs.json.Json.obj(
        "key" -> play.api.libs.json.Json.toJson(x.key)
      )
    }
  }
}

package quality {
  object helpers {
    import org.joda.time.DateTime
    import org.joda.time.format.ISODateTimeFormat
    import play.api.mvc.QueryStringBindable

    import scala.util.{ Failure, Success, Try }

    private[helpers] val dateTimeISOParser = ISODateTimeFormat.dateTimeParser()
    private[helpers] val dateTimeISOFormatter = ISODateTimeFormat.dateTime()

    private[helpers] def parseDateTimeISO(s: String): Either[String, DateTime] = {
      Try(dateTimeISOParser.parseDateTime(s)) match {
        case Success(dt) => Right(dt)
        case Failure(f) => Left("Could not parse DateTime: " + f.getMessage)
      }
    }

    implicit object DateTimeISOQueryStringBinder extends QueryStringBindable[DateTime] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DateTime]] = {
        for {
          values <- params.get(key)
          s <- values.headOption
        } yield parseDateTimeISO(s)
      }

      override def unbind(key: String, time: DateTime): String = key + "=" + dateTimeISOFormatter.print(time)
    }
  }

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import quality.models._
    import quality.models.json._

    private val logger = play.api.Logger("quality.client")

    logger.info(s"Initializing quality.client for url $apiUrl")

    def events: Events = Events

    def healthchecks: Healthchecks = Healthchecks

    def incidents: Incidents = Incidents

    def plans: Plans = Plans

    def statistics: Statistics = Statistics

    def teams: Teams = Teams

    trait Events {
      /**
       * Search all events. Results are always paginated. Events are sorted in time order
       * - the first record is the most recent event.
       */
      def get(
        model: scala.Option[Model] = None,
        action: scala.Option[Action] = None,
        numberHours: scala.Option[Int] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Event]]
    }

    object Events extends Events {
      override def get(
        model: scala.Option[Model] = None,
        action: scala.Option[Action] = None,
        numberHours: scala.Option[Int] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Event]] = {
        val query = Seq(
          model.map("model" -> _.toString),
          action.map("action" -> _.toString),
          numberHours.map("number_hours" -> _.toString),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        GET(s"/events", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[quality.models.Event]]
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Healthchecks {
      def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Healthcheck]]
    }

    object Healthchecks extends Healthchecks {
      override def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Healthcheck]] = {
        GET(s"/_internal_/healthcheck").map {
          case r if r.status == 200 => Some(r.json.as[quality.models.Healthcheck])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Incidents {
      /**
       * Search all incidents. Results are always paginated.
       */
      def get(
        id: scala.Option[Long] = None,
        teamKey: scala.Option[String] = None,
        hasTeam: scala.Option[Boolean] = None,
        hasPlan: scala.Option[Boolean] = None,
        hasGrade: scala.Option[Boolean] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Incident]]

      /**
       * Returns information about the incident with this specific id.
       */
      def getById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Incident]]

      /**
       * Create a new incident.
       */
      def post(
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Incident]

      /**
       * Updates an incident.
       */
      def putById(
        id: Long,
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Incident]

      def deleteById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
    }

    object Incidents extends Incidents {
      override def get(
        id: scala.Option[Long] = None,
        teamKey: scala.Option[String] = None,
        hasTeam: scala.Option[Boolean] = None,
        hasPlan: scala.Option[Boolean] = None,
        hasGrade: scala.Option[Boolean] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Incident]] = {
        val query = Seq(
          id.map("id" -> _.toString),
          teamKey.map("team_key" -> _),
          hasTeam.map("has_team" -> _.toString),
          hasPlan.map("has_plan" -> _.toString),
          hasGrade.map("has_grade" -> _.toString),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        GET(s"/incidents", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[quality.models.Incident]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Incident]] = {
        GET(s"/incidents/${id}").map {
          case r if r.status == 200 => Some(r.json.as[quality.models.Incident])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Incident] = {
        val payload = play.api.libs.json.Json.obj(
          "team_key" -> play.api.libs.json.Json.toJson(teamKey),
          "severity" -> play.api.libs.json.Json.toJson(severity),
          "summary" -> play.api.libs.json.Json.toJson(summary),
          "description" -> play.api.libs.json.Json.toJson(description),
          "tags" -> play.api.libs.json.Json.toJson(tags)
        )

        POST(s"/incidents", payload).map {
          case r if r.status == 201 => r.json.as[quality.models.Incident]
          case r if r.status == 409 => throw new quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def putById(
        id: Long,
        teamKey: scala.Option[String] = None,
        severity: String,
        summary: String,
        description: scala.Option[String] = None,
        tags: scala.collection.Seq[String] = Nil
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Incident] = {
        val payload = play.api.libs.json.Json.obj(
          "team_key" -> play.api.libs.json.Json.toJson(teamKey),
          "severity" -> play.api.libs.json.Json.toJson(severity),
          "summary" -> play.api.libs.json.Json.toJson(summary),
          "description" -> play.api.libs.json.Json.toJson(description),
          "tags" -> play.api.libs.json.Json.toJson(tags)
        )

        PUT(s"/incidents/${id}", payload).map {
          case r if r.status == 201 => r.json.as[quality.models.Incident]
          case r if r.status == 409 => throw new quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        DELETE(s"/incidents/${id}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Plans {
      /**
       * Search all plans. Results are always paginated.
       */
      def get(
        id: scala.Option[Long] = None,
        incidentId: scala.Option[Long] = None,
        teamKey: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Plan]]

      /**
       * Create a plan.
       */
      def post(
        incidentId: Long,
        body: String,
        grade: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Plan]

      /**
       * Update a plan.
       */
      def putById(
        id: Long,
        incidentId: Long,
        body: String,
        grade: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Plan]

      /**
       * Update the grade assigned to a plan.
       */
      def putGradeById(
        id: Long,
        grade: Int
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Plan]

      /**
       * Get a single plan.
       */
      def getById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Plan]]

      /**
       * Delete a plan.
       */
      def deleteById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
    }

    object Plans extends Plans {
      override def get(
        id: scala.Option[Long] = None,
        incidentId: scala.Option[Long] = None,
        teamKey: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Plan]] = {
        val query = Seq(
          id.map("id" -> _.toString),
          incidentId.map("incident_id" -> _.toString),
          teamKey.map("team_key" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        GET(s"/plans", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[quality.models.Plan]]
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        incidentId: Long,
        body: String,
        grade: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Plan] = {
        val payload = play.api.libs.json.Json.obj(
          "incident_id" -> play.api.libs.json.Json.toJson(incidentId),
          "body" -> play.api.libs.json.Json.toJson(body),
          "grade" -> play.api.libs.json.Json.toJson(grade)
        )

        POST(s"/plans", payload).map {
          case r if r.status == 201 => r.json.as[quality.models.Plan]
          case r if r.status == 409 => throw new quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def putById(
        id: Long,
        incidentId: Long,
        body: String,
        grade: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Plan] = {
        val payload = play.api.libs.json.Json.obj(
          "incident_id" -> play.api.libs.json.Json.toJson(incidentId),
          "body" -> play.api.libs.json.Json.toJson(body),
          "grade" -> play.api.libs.json.Json.toJson(grade)
        )

        PUT(s"/plans/${id}", payload).map {
          case r if r.status == 200 => r.json.as[quality.models.Plan]
          case r if r.status == 409 => throw new quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def putGradeById(
        id: Long,
        grade: Int
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Plan] = {
        val payload = play.api.libs.json.Json.obj(
          "grade" -> play.api.libs.json.Json.toJson(grade)
        )

        PUT(s"/plans/${id}/grade", payload).map {
          case r if r.status == 200 => r.json.as[quality.models.Plan]
          case r if r.status == 409 => throw new quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def getById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Plan]] = {
        GET(s"/plans/${id}").map {
          case r if r.status == 200 => Some(r.json.as[quality.models.Plan])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteById(
        id: Long
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        DELETE(s"/plans/${id}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Statistics {
      /**
       * Retrieve team statistics for all or one team.
       */
      def get(
        teamKey: scala.Option[String] = None,
        numberHours: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Statistic]]
    }

    object Statistics extends Statistics {
      override def get(
        teamKey: scala.Option[String] = None,
        numberHours: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Statistic]] = {
        val query = Seq(
          teamKey.map("team_key" -> _),
          numberHours.map("number_hours" -> _.toString)
        ).flatten

        GET(s"/statistics", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[quality.models.Statistic]]
          case r => throw new FailedRequest(r)
        }
      }
    }

    trait Teams {
      /**
       * Search all teams. Results are always paginated.
       */
      def get(
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Team]]

      /**
       * Returns information about the team with this specific key.
       */
      def getByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Team]]

      /**
       * Create a new team.
       */
      def post(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Team]

      def deleteByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
    }

    object Teams extends Teams {
      override def get(
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.collection.Seq[quality.models.Team]] = {
        val query = Seq(
          key.map("key" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        GET(s"/teams", query).map {
          case r if r.status == 200 => r.json.as[scala.collection.Seq[quality.models.Team]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[quality.models.Team]] = {
        GET(s"/teams/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[quality.models.Team])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[quality.models.Team] = {
        val payload = play.api.libs.json.Json.obj(
          "key" -> play.api.libs.json.Json.toJson(key)
        )

        POST(s"/teams", payload).map {
          case r if r.status == 201 => r.json.as[quality.models.Team]
          case r if r.status == 409 => throw new quality.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        DELETE(s"/teams/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    private val UserAgent = "apidoc:0.4.72 http://www.apidoc.me/gilt/code/quality/0.0.1-dev/play_2_3_client"

    def _requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path).withHeaders("User-Agent" -> UserAgent)
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield name -> value
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      apiToken.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
      req
    }

    def POST(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("POST", _requestHolder(path).withQueryString(q:_*)).post(data)
    }

    def GET(
      path: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("GET", _requestHolder(path).withQueryString(q:_*)).get()
    }

    def PUT(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PUT", _requestHolder(path).withQueryString(q:_*)).put(data)
    }

    def PATCH(
      path: String,
      data: play.api.libs.json.JsValue = play.api.libs.json.Json.obj(),
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("PATCH", _requestHolder(path).withQueryString(q:_*)).patch(data)
    }

    def DELETE(
      path: String,
      q: Seq[(String, String)] = Seq.empty
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      _logRequest("DELETE", _requestHolder(path).withQueryString(q:_*)).delete()
    }

  }

  case class FailedRequest(response: play.api.libs.ws.WSResponse) extends Exception(response.status + ": " + response.body)

  package error {

    import quality.models.json._

    case class ErrorsResponse(response: play.api.libs.ws.WSResponse) extends Exception(response.status + ": " + response.body) {

      lazy val errors = response.json.as[scala.collection.Seq[quality.models.Error]]

    }
  }


}
