package strategygames
package format.pgn
import strategygames.Clock

import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import cats.syntax.option._

case class Tag(name: TagType, value: String) {

  override def toString = s"""[$name "$value"]"""
}

sealed trait TagType {
  lazy val name      = toString
  lazy val lowercase = name.toLowerCase
  val isUnknown      = false
}

case class Tags(value: List[Tag]) extends AnyVal {

  def apply(name: String): Option[String] = {
    val tagType = Tag tagType name
    value.find(_.name == tagType).map(_.value)
  }

  def apply(which: Tag.type => TagType): Option[String] =
    value find (_.name == which(Tag)) map (_.value)

  def clockConfig: Option[Clock.Config] =
    value.collectFirst { case Tag(Tag.TimeControl, str) =>
      str
    } flatMap Clock.readPgnConfig

  def draughtsVariant: Option[strategygames.draughts.variant.Variant] =
    apply(_.GameType).fold(apply(_.Variant) flatMap strategygames.draughts.variant.Variant.byName) {
      case Tags.GameTypeRegex(t, _*) =>
        strategygames.draughts.parseIntOption(t) match {
          case Some(gameType) => strategygames.draughts.variant.Variant byGameType gameType
          case _              => None
        }
      case _ => None
    }

  def chessVariant: Option[strategygames.chess.variant.Variant] =
    apply(_.Variant).map(_.toLowerCase).flatMap {
      case "chess 960" | "fischerandom" | "fischerrandom" => strategygames.chess.variant.Chess960.some
      case name                                           => strategygames.chess.variant.Variant byName name
    }

  def fairysfVariant: Option[strategygames.fairysf.variant.Variant] =
    apply(_.Variant).map(_.toLowerCase).flatMap {
      strategygames.fairysf.variant.Variant byName _
    }

  // TODO: this will need to be tested. We'll want to look at the _actual_ values that
  //       come in via these tags and ensure that the order we look at them is appropriate
  def variant: Option[strategygames.variant.Variant] = chessVariant
    .map(strategygames.variant.Variant.Chess)
    .orElse(draughtsVariant.map(strategygames.variant.Variant.Draughts).orElse(fairysfVariant.map(strategygames.variant.Variant.FairySF)))

  def anyDate: Option[String] = apply(_.UTCDate) orElse apply(_.Date)

  def year: Option[Int] = anyDate flatMap {
    case Tags.DateRegex(y, _, _) => strategygames.draughts.parseIntOption(y)
    case _                       => None
  }

  def chessFen: Option[chess.format.FEN] = apply(_.FEN).map(strategygames.chess.format.FEN.apply)
  def draughtsFen: Option[draughts.format.FEN] = apply(_.FEN).map(strategygames.draughts.format.FEN.apply)

  def fairysfFen: Option[fairysf.format.FEN] = apply(_.FEN).map(strategygames.fairysf.format.FEN.apply)

  def fen: Option[format.FEN] = 
     variant match {
       case Some(strategygames.variant.Variant.Draughts(_)) => draughtsFen.map(format.FEN.Draughts)
       case Some(strategygames.variant.Variant.Chess(_)) => chessFen.map(format.FEN.Chess)
       case Some(strategygames.variant.Variant.FairySF(_)) => fairysfFen.map(format.FEN.FairySF)
     }

  def exists(which: Tag.type => TagType): Boolean =
    value.exists(_.name == which(Tag))

  def resultColor: Option[Option[Color]] =
    apply(_.Result).filter("*" !=).map(v => {strategygames.Color.fromResult(v)})

  def ++(tags: Tags) = tags.value.foldLeft(this)(_ + _)

  def +(tag: Tag) = Tags(value.filterNot(_.name == tag.name) :+ tag)

  def sorted = copy(
    value = value.sortBy { tag =>
      Tags.tagIndex.getOrElse(tag.name, 999)
    }
  )

  override def toString = sorted.value mkString "\n"
}

object Tags {
  val empty = Tags(Nil)

  // according to http://www.saremba.de/chessgml/standards/pgn/pgn-complete.htm#c8.1.1
  val sevenTagRoster = List(
    Tag.Event,
    Tag.Site,
    Tag.Date,
    Tag.Round,
    Tag.White,
    Tag.Black,
    Tag.Result
  )
  val tagIndex: Map[TagType, Int] = sevenTagRoster.zipWithIndex.toMap

  private val DateRegex     = """(\d{4}|\?{4})\.(\d\d|\?\?)\.(\d\d|\?\?)""".r
  private val GameTypeRegex = """([0-9]+)(,[WB],[0-9]+,[0-9]+,[ANS][0123](,[01])?)?""".r
}

object Tag {

  case object Event extends TagType
  case object Site  extends TagType
  case object Date  extends TagType
  case object UTCDate extends TagType {
    val format = DateTimeFormat forPattern "yyyy.MM.dd" withZone DateTimeZone.UTC
  }
  case object UTCTime extends TagType {
    val format = DateTimeFormat forPattern "HH:mm:ss" withZone DateTimeZone.UTC
  }
  case object Round           extends TagType
  case object White           extends TagType
  case object Black           extends TagType
  case object TimeControl     extends TagType
  case object WhiteClock      extends TagType
  case object BlackClock      extends TagType
  case object WhiteElo        extends TagType
  case object BlackElo        extends TagType
  case object WhiteRating     extends TagType
  case object BlackRating     extends TagType
  case object WhiteRatingDiff extends TagType
  case object BlackRatingDiff extends TagType
  case object WhiteTitle      extends TagType
  case object BlackTitle      extends TagType
  case object WhiteTeam       extends TagType
  case object BlackTeam       extends TagType
  case object Result          extends TagType
  case object FEN             extends TagType
  case object Variant         extends TagType
  case object GameType        extends TagType
  case object MicroMatch      extends TagType
  case object ECO             extends TagType
  case object Opening         extends TagType
  case object Termination     extends TagType
  case object Annotator       extends TagType
  case class Unknown(n: String) extends TagType {
    override def toString  = n
    override val isUnknown = true
  }

  val tagTypes = List(
    Event,
    Site,
    Date,
    UTCDate,
    UTCTime,
    Round,
    White,
    Black,
    TimeControl,
    WhiteClock,
    BlackClock,
    WhiteElo,
    BlackElo,
    WhiteRating,
    BlackRating,
    WhiteRatingDiff,
    BlackRatingDiff,
    WhiteTitle,
    BlackTitle,
    WhiteTeam,
    BlackTeam,
    Result,
    FEN,
    Variant,
    GameType,
    MicroMatch,
    ECO,
    Opening,
    Termination,
    Annotator
  )
  val tagTypesByLowercase: Map[String, TagType] =
    tagTypes.map { t => t.lowercase -> t }.to(Map)

  def apply(name: String, value: Any): Tag = new Tag(
    name = tagType(name),
    value = value.toString
  )

  def apply(name: Tag.type => TagType, value: Any): Tag = new Tag(
    name = name(this),
    value = value.toString
  )

  def tagType(name: String) =
    (tagTypesByLowercase get name.toLowerCase) | Unknown(name)

  def timeControl(clock: Option[Clock.Config]) =
    Tag(
      TimeControl,
      clock.fold("-") { c =>
        s"${c.limit.roundSeconds}+${c.increment.roundSeconds}"
      }
    )
}
