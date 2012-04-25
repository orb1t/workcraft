package org.workcraft.plugins.petrify

import org.workcraft.services.Exporter
import org.workcraft.services.Format
import org.workcraft.services.ModelServiceProvider
import org.workcraft.services.ServiceNotAvailableException
import org.workcraft.services.ExportJob
import java.io.OutputStream
import org.workcraft.scala.effects.IO
import org.workcraft.scala.effects.IO._
import java.io.PrintWriter
import java.io.BufferedOutputStream
import scalaz._
import Scalaz._
import org.workcraft.plugins.petri2.PetriNetService
import org.workcraft.plugins.petri2.PetriNet
import org.workcraft.plugins.petri2.Transition
import org.workcraft.plugins.petri2.Place
import org.workcraft.plugins.petri2.ConsumerArc
import org.workcraft.plugins.petri2.ProducerArc
import java.io.File
import java.io.FileOutputStream
import org.workcraft.services.ExportError

object DotGExporter extends Exporter {
  val targetFormat = Format.DotG

  def export(model: ModelServiceProvider): Either[ServiceNotAvailableException, ExportJob] = model.implementation(PetriNetService) match {
    case Some(impl) => Right(new PnToDotGExportJob(impl))
    case None => Left(new ServiceNotAvailableException(PetriNetService))
  }
}

class PnToDotGExportJob(snapshot: IO[PetriNet]) extends ExportJob {
  val complete = false

  def job(file: File) = snapshot >>= (net => ioPure.pure {
    var writer: PrintWriter = null
    try {

      writer = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)))
      val (tPostset, _) = net.incidence
      val (_, pPostset) = net.placeIncidence

      writer.println("# File generated by Workcraft (http://workcraft.org)")
      writer.println(".dummy " + net.transitions.map(net.labelling(_)).sorted.mkString(" "))

      writer.println(".graph")

      writer.println((
        tPostset.mapValues(_.map(net.labelling(_)).sorted).toList.map { case (t, x) => (net.labelling(t), x) } ++
        pPostset.mapValues(_.map(net.labelling(_)).sorted).toList.map { case (p, x) => (net.labelling(p), x) }).sortBy(_._1).map { case (from, to) => from + " " + to.mkString(" ")}.mkString(" \n"))

      writer.println(".marking {" + net.places.filter(net.marking(_) > 0).map(p => {
        val mrk = net.marking(p)
        val name = net.labelling(p)
        if (mrk == 1) name else name + "=" + mrk
      }).sorted.mkString(" ") + "}")
      
      writer.println (".end")

      None
    } catch {
      case e => Some(ExportError.Exception(e))
    } finally {
      if (writer != null)
        writer.close()
    }
  })
}