package controllers

import client.Api
import quality.models.Incident
import lib.{ Pagination, PaginatedCollection }
import java.util.UUID
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

object Incidents extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global

  def show(guid: UUID) = Action.async { implicit request =>
    for {
      incidentResponse <- Api.instance.Incidents.get(guid = Some(guid))
    } yield {
      incidentResponse.entity.headOption match {
        case None => NotFound
        case Some(i: Incident) => {
          Ok(views.html.incidents.show(i))
        }
      }
    }
  }

  def create() = Action { implicit request =>
    Ok(views.html.incidents.create(incidentForm))
  }

  def postCreate() = Action { implicit request =>
    val boundForm = incidentForm.bindFromRequest
    boundForm.fold (

      formWithErrors => {
        Ok(views.html.incidents.create(formWithErrors))
      },

      incidentForm => {
        val incident = Await.result(Api.instance.Incidents.post(
          summary = incidentForm.summary,
          description = incidentForm.description,
          teamKey = incidentForm.teamKey,
          severity = incidentForm.severity
        ), 1000.millis).entity

        println("Incident: " + incident)
        Redirect(routes.Incidents.show(incident.guid)).flashing("success" -> "Incident created")

      }

    )
  }

  case class IncidentForm(
    summary: String,
    description: String,
    teamKey: String,
    severity: String
  )

  private val incidentForm = Form(
    mapping(
      "summary" -> nonEmptyText,
      "description" -> nonEmptyText,
      "teamKey" -> nonEmptyText,
      "severity" -> nonEmptyText
    )(IncidentForm.apply)(IncidentForm.unapply)
  )

}
