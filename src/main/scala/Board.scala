package strategygames

import chess.variant.Crazyhouse
import variant.Variant

abstract sealed class Board(
  val pieces: PieceMap,
  val history: History,
  val variant: Variant,
  val crazyData: Option[Crazyhouse.Data] = None
) {

  def apply(at: Pos): Option[Piece] = pieces get at

  val actors: Map[Pos, Actor]

  val actorsOf: Color.Map[Seq[Actor]] = {
    val (w, b) = actors.values.toSeq.partition { _.color.white }
    Color.Map(w, b)
  }

  def rolesOf(c: Color): List[Role] =
    pieces.values
      .collect {
        case piece if piece.color == c => piece.role
      }
      .to(List)

  def actorAt(at: Pos): Option[Actor] = actors get at

  def piecesOf(c: Color): Map[Pos, Piece] = pieces filter (_._2 is c)

  val kingPos: Map[Color, Pos]

  def kingPosOf(c: Color): Option[Pos] = kingPos get c

  def seq(actions: Board => Option[Board]*): Option[Board] =
    actions.foldLeft(Option(this): Option[Board])(_ flatMap _)

  def place(piece: Piece, at: Pos): Option[Board]

  def take(at: Pos): Option[Board]

  def move(orig: Pos, dest: Pos): Option[Board]

  def hasPiece(p: Piece) = pieces.values exists (p ==)

  def promote(pos: Pos): Option[Board]

  def withHistory(h: History): Board

  def withPieces(newPieces: PieceMap): Board

  def withVariant(v: Variant): Board

  //def updateHistory(f: History => History): Board

  def count(p: Piece): Int = pieces.values count (_ == p)
  def count(c: Color): Int = pieces.values count (_.color == c)

  def autoDraw: Boolean

  def situationOf(color: Color): Situation

  def valid(strict: Boolean) = variant.valid(this, strict)

  def materialImbalance: Int

  override def toString: String

  // TODO: Yup, still not type safe. :D
  def toChess: chess.Board
  def toDraughts: draughts.Board
}

object Board {

  case class Chess(b: chess.Board) extends Board(
    b.pieces.map{case(pos, piece) => (Pos.Chess(pos), Piece.Chess(piece))},
    History.Chess(b.history),
    Variant.Chess(b.variant),
    b.crazyData
  ) {

    val actors: Map[Pos, Actor] = b.actors.map{case(p, a) => (Pos.Chess(p), Actor.Chess(a))}

    val kingPos: Map[Color, Pos] = b.kingPos.map{case(c, p) => (c, Pos.Chess(p))}

    def place(piece: Piece, at: Pos): Option[Board] = (piece, at) match {
      case (Piece.Chess(piece), Pos.Chess(at)) => b.place(piece, at).map(Chess)
      case _ => sys.error("Not passed Chess objects")
    }

    def take(at: Pos): Option[Board] = at match {
      case Pos.Chess(at) => b.take(at).map(Chess)
      case _ => sys.error("Not passed Chess objects")
    }

    def move(orig: Pos, dest: Pos): Option[Board] = (orig, dest) match {
      case (Pos.Chess(orig), Pos.Chess(dest)) => b.move(orig, dest).map(Chess)
      case _ => sys.error("Not passed Chess objects")
    }

    def promote(pos: Pos): Option[Board] = pos match {
      case Pos.Chess(pos) => b.promote(pos).map(Chess)
      case _ => sys.error("Not passed Chess objects")
    }

    def withHistory(h: History): Board = h match {
      case History.Chess(h) => Chess(b.withHistory(h))
      case _ => sys.error("Not passed Chess objects")
    }

    def withPieces(newPieces: PieceMap): Board = Chess(b.withPieces(newPieces.map{
      case (Pos.Chess(pos), Piece.Chess(piece)) => (pos, piece)
      case _ => sys.error("Not passed Chess objects")
    }))

    def withVariant(v: Variant): Board = v match {
      case Variant.Chess(v) => Chess(b.withVariant(v))
      case _ => sys.error("Not passed Chess objects")
    }

    //This isn't correct, but it is unused by lila.
    //If we wanted to make this work we should create a sealed class in this file
    //e.g. UpdateHistory and then have case class for ChessUpdateHistory etc
    //which have this function in them so then we can match on ChessUpdateHistory(f)
    //def updateHistory(f: History => History): Board = f match {
    //  case History.Chess(f) => Chess(b.updateHistory(f))
    //  case _ => sys.error("Not passed Chess objects")
    //}

    def autoDraw: Boolean = b.autoDraw

    def situationOf(color: Color): Situation = Situation.Chess(b.situationOf(color))

    def materialImbalance: Int = b.materialImbalance

    override def toString: String = b.toString

    def toChess = b
    def toDraughts = sys.error("Can't make a draughts board from a chess board")

  }

  case class Draughts(b: draughts.Board) extends Board(
    b.pieces.map{case(pos, piece) => (Pos.Draughts(pos), Piece.Draughts(piece))},
    History.Draughts(b.history),
    Variant.Draughts(b.variant)
  ) {

    val actors: Map[Pos, Actor] = b.actors.map{case(p, a) => (Pos.Draughts(p), Actor.Draughts(a))}

    val kingPos: Map[Color, Pos] = b.kingPos.map{case(c, p) => (c, Pos.Draughts(p))}

    def place(piece: Piece, at: Pos): Option[Board] = (piece, at) match {
      case (Piece.Draughts(piece), Pos.Draughts(at)) => b.place(piece, at).map(Draughts)
      case _ => sys.error("Not passed Draughts objects")
    }

    def take(at: Pos): Option[Board] = at match {
      case Pos.Draughts(at) => b.take(at).map(Draughts)
      case _ => sys.error("Not passed Draughts objects")
    }

    def move(orig: Pos, dest: Pos): Option[Board] = (orig, dest) match {
      case (Pos.Draughts(orig), Pos.Draughts(dest)) => b.move(orig, dest).map(Draughts)
      case _ => sys.error("Not passed Draughts objects")
    }

    def promote(pos: Pos): Option[Board] = pos match {
      case Pos.Draughts(pos) => b.promote(pos).map(Draughts)
      case _ => sys.error("Not passed Draughts objects")
    }

    def withHistory(h: History): Board = h match {
      case History.Draughts(h) => Draughts(b.withHistory(h))
      case _ => sys.error("Not passed Draughts objects")
    }

    def withPieces(newPieces: PieceMap): Board = Draughts(b.withPieces(newPieces.map{
      case (Pos.Draughts(pos), Piece.Draughts(piece)) => (pos, piece)
      case _ => sys.error("Not passed Draughts objects")
    }))

    def withVariant(v: Variant): Board = v match {
      case Variant.Draughts(v) => Draughts(b.withVariant(v))
      case _ => sys.error("Not passed Draughts objects")
    }

    //This isn't correct, but it is unused by lila.
    //If we wanted to make this work we should create a sealed class in this file
    //e.g. UpdateHistory and then have case class for ChessUpdateHistory etc
    //which have this function in them so then we can match on ChessUpdateHistory(f)
    //def updateHistory(f: History => History): Board = f match {
    //  case History.Draughts(f) => Draughts(b.updateHistory(f))
    //  case _ => sys.error("Not passed Draughts objects")
    //}

    def autoDraw: Boolean = b.autoDraw

    def situationOf(color: Color): Situation = Situation.Draughts(b.situationOf(color))

    def materialImbalance: Int = b.materialImbalance

    override def toString: String = b.toString
    def toDraughts = b
    def toChess = sys.error("Can't make a chess board from a draughts board")

  }

  //added this and then didnt use it
  //def apply(lib: GameLib, pieces: PieceMap, history: History, variant: Variant, crazyData: Option[Crazyhouse.Data] = None): Board =
  //  (lib, history, variant) match {
  //    case (GameLib.Draughts(), History.Draughts(history), Variant.Draughts(variant))
  //      => Draughts(draughts.Board(Piece.draughtsPieceMap(pieces), history, variant))
  //    case (GameLib.Chess(), History.Chess(history), Variant.Chess(variant))
  //      => Chess(chess.Board(Piece.chessPieceMap(pieces), history, variant, crazyData))
  //    case _ => sys.error("Mismatched gamelib types")
  //  }

  def apply(lib: GameLib, pieces: Iterable[(Pos, Piece)], variant: Variant): Board =
    (lib, variant) match {
      case (GameLib.Draughts(), Variant.Draughts(variant))
        => Draughts(draughts.Board.apply(
          pieces.map{case(Pos.Draughts(pos), Piece.Draughts(piece)) => (pos, piece)},
          variant
        ))
      case (GameLib.Chess(), Variant.Chess(variant))
        => Chess(chess.Board.apply(
          pieces.map{case(Pos.Chess(pos), Piece.Chess(piece)) => (pos, piece)},
          variant
        ))
      case _ => sys.error("Mismatched gamelib types")
    }


  implicit def chessBoard(b: chess.Board) = Board.Chess(b)
  implicit def draughtsBoard(b: draughts.Board) = Board.Draughts(b)

  def init(lib: GameLib, variant: Variant): Board = (lib, variant) match {
    case (GameLib.Draughts(), Variant.Draughts(variant)) => Draughts(draughts.Board.init(variant))
    case (GameLib.Chess(), Variant.Chess(variant))       => Chess(chess.Board.init(variant))
    case _ => sys.error("Mismatched gamelib types")
  }

  def empty(lib: GameLib, variant: Variant): Board = (lib, variant) match {
    case (GameLib.Draughts(), Variant.Draughts(variant)) => Draughts(draughts.Board.empty(variant))
    case (GameLib.Chess(), Variant.Chess(variant))       => Chess(chess.Board.empty(variant))
    case _ => sys.error("Mismatched gamelib types")
  }

}
