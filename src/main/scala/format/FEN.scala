package strategygames.format

import strategygames.{ Color, GameLogic }

abstract sealed class FEN(val value: String) {

  def toChess: strategygames.chess.format.FEN
  def toDraughts: strategygames.draughts.format.FEN
  def toFairySF: strategygames.fairysf.format.FEN

  override def toString = value

  def fullMove: Option[Int]

  def color: Option[Color]

  def ply: Option[Int]

  def initial: Boolean

  def chessFen: Option[strategygames.chess.format.FEN]

}

object FEN {

  final case class Chess(f: strategygames.chess.format.FEN) extends FEN(f.value) {

    def toChess = f
    def toDraughts = sys.error("Can't convert chess to draughts")
    def toFairySF = sys.error("Can't convert chess to fairysf")

    def fullMove: Option[Int] = f.fullMove

    def color: Option[Color] = f.color

    def ply: Option[Int] = f.ply

    def initial: Boolean = f.initial

    def chessFen: Option[strategygames.chess.format.FEN] = Some(f)

  }

  final case class Draughts(f: strategygames.draughts.format.FEN) extends FEN(f.value) {

    def toChess = sys.error("Can't convert draughts to chess")
    def toDraughts = f
    def toFairySF = sys.error("Can't convert draughts to fairysf")

    //need to consider an implementation for draughts?
    def fullMove: Option[Int] = None

    def color: Option[Color] = f.color

    //need to consider an implementation for draughts?
    def ply: Option[Int] = None

    def initial: Boolean = f.initial

    def chessFen: Option[strategygames.chess.format.FEN] = None

  }

  final case class FairySF(f: strategygames.fairysf.format.FEN) extends FEN(f.value) {

    def toChess = sys.error("Can't convert fairysf to chess")
    def toDraughts = sys.error("Can't convert fairysf to draughts")
    def toFairySF = f

    def fullMove: Option[Int] = f.fullMove

    def color: Option[Color] = f.color

    def ply: Option[Int] = f.ply

    def initial: Boolean = f.initial

    def chessFen: Option[strategygames.chess.format.FEN] = None

  }

  def wrap(fen: strategygames.chess.format.FEN) = Chess(fen)
  def wrap(fen: strategygames.draughts.format.FEN) = Draughts(fen)
  def wrap(fen: strategygames.fairysf.format.FEN) = FairySF(fen)

  def apply(lib: GameLogic, value: String): FEN = lib match {
    case GameLogic.Draughts() => FEN.Draughts(strategygames.draughts.format.FEN(value))
    case GameLogic.Chess()    => FEN.Chess(strategygames.chess.format.FEN(value))
    case GameLogic.FairySF()  => FEN.FairySF(strategygames.fairysf.format.FEN(value))
  }

  def clean(lib: GameLogic, source: String): FEN = lib match {
    case GameLogic.Draughts()
      => Draughts(strategygames.draughts.format.FEN(source.replace("_", " ").trim))
    case GameLogic.Chess()
      => Chess(strategygames.chess.format.FEN(source.replace("_", " ").trim))
    case GameLogic.FairySF()
      => FairySF(strategygames.fairysf.format.FEN(source.replace("_", " ").trim))
  }

}
