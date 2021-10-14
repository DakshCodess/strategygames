package strategygames.fairysf.format

import cats.implicits._
import strategygames.{ Color, Pocket, Pockets }
import strategygames.fairysf._
import strategygames.fairysf.variant.{ Variant }

/** Transform a game to standard Forsyth Edwards Notation
  * http://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation
  *
  * Crazyhouse & Threecheck extensions:
  * https://github.com/ddugovic/Stockfish/wiki/FEN-extensions
  * http://scidb.sourceforge.net/help/en/FEN.html#ThreeCheck
  */
object Forsyth {

  val initial = FEN("lnsgkgsnl/1r5b1/ppppppppp/9/9/9/PPPPPPPPP/1B5R1/LNSGKGSNL b - 1")

  //stub
  def <<@(variant: Variant, fen: FEN): Option[Situation] = None
    //makeBoard(variant, fen) map { board =>
    //  val splitted    = fen.value split ' '
    //  val colorOption = splitted lift 1 flatMap (_ lift 0) flatMap Color.apply
    //  val situation = colorOption match {
    //    case Some(color)             => Situation(board, color)
    //    case _ if board.check(Black) => Situation(board, Black) // user in check will move first
    //    case _                       => Situation(board, White)
    //  }
    //  splitted
    //    .lift(2)
    //    .fold(situation) { strCastles =>
    //      val (castles, unmovedRooks) = strCastles.foldLeft(Castles.none -> Set.empty[Pos]) {
    //        case ((c, r), ch) =>
    //          val color = Color.fromWhite(ch.isUpper)
    //          val rooks = board
    //            .piecesOf(color)
    //            .collect {
    //              case (pos, piece) if piece.is(Rook) && pos.rank == Rank.backRank(color) => pos
    //            }
    //            .toList
    //            .sortBy(_.file)
    //          (for {
    //            kingPos <- board.kingPosOf(color)
    //            rookPos <- (ch.toLower match {
    //              case 'k'  => rooks.reverse.find(_ ?> kingPos)
    //              case 'q'  => rooks.find(_ ?< kingPos)
    //              case file => rooks.find(_.file.char == file)
    //            })
    //            side <- Side.kingRookSide(kingPos, rookPos)
    //          } yield (c.add(color, side), r + rookPos)).getOrElse((c, r))
    //      }

    //      val fifthRank   = if (situation.color == White) Rank.Fifth else Rank.Fourth
    //      val sixthRank   = if (situation.color == White) Rank.Sixth else Rank.Third
    //      val seventhRank = if (situation.color == White) Rank.Seventh else Rank.Second
    //      val lastMove = for {
    //        pos <- splitted lift 3 flatMap Pos.fromKey
    //        if pos.rank == sixthRank
    //        orig = Pos(pos.file, seventhRank)
    //        dest = Pos(pos.file, fifthRank)
    //        if situation.board(dest).contains(Piece(!situation.color, Pawn)) &&
    //          situation.board(pos.file, sixthRank).isEmpty &&
    //          situation.board(orig).isEmpty
    //      } yield Uci.Move(orig, dest)

    //      situation withHistory {
    //        val history = History(
    //          lastMove = lastMove,
    //          positionHashes = Array.empty,
    //          castles = castles,
    //          unmovedRooks = UnmovedRooks(unmovedRooks)
    //        )
    //        val checkCount =
    //          splitted
    //            .lift(4)
    //            .flatMap(makeCheckCount)
    //            .orElse(splitted.lift(6).flatMap(makeCheckCount))
    //        checkCount.fold(history)(history.withCheckCount)
    //      }
    //    } fixCastles
    //}

  def <<(fen: FEN): Option[Situation] = <<@(Variant.default, fen)

  case class SituationPlus(situation: Situation, fullMoveNumber: Int) {

    def turns = fullMoveNumber * 2 - situation.color.fold(2, 1)
  }

  def <<<@(variant: Variant, fen: FEN): Option[SituationPlus] = None
    //<<@(variant, fen) map { sit =>
    //  val splitted       = fen.value.split(' ').drop(4).dropWhile(_.contains('+'))
    //  val fullMoveNumber = splitted lift 1 flatMap (_.toIntOption) map (_ max 1 min 500)
    //  val halfMoveClock  = splitted lift 0 flatMap (_.toIntOption) map (_ max 0 min 100)
    //  SituationPlus(
    //    halfMoveClock.map(sit.history.setHalfMoveClock).fold(sit)(sit.withHistory),
    //    fullMoveNumber | 1
    //  )
    //}

  def <<<(fen: FEN): Option[SituationPlus] = <<<@(Variant.default, fen)

  def makeCheckCount(str: String): Option[CheckCount] =
    str.toList match {
      case '+' :: w :: '+' :: b :: Nil =>
        for {
          white <- w.toString.toIntOption if white <= 3
          black <- b.toString.toIntOption if black <= 3
        } yield CheckCount(black, white)
      case w :: '+' :: b :: Nil =>
        for {
          white <- w.toString.toIntOption if white <= 3
          black <- b.toString.toIntOption if black <= 3
        } yield CheckCount(3 - black, 3 - white)
      case _ => None
    }

  // only cares about pieces positions on the board (first part of FEN string)
  //private def makeBoard(variant: Variant, fen: FEN): Option[Board] = {
  //  val (position, pockets) = fen.value.takeWhile(' ' !=) match {
  //    case word if word.count('/' ==) == 8 =>
  //      val splitted = word.split('/')
  //      splitted.take(8).mkString("/") -> splitted.lift(8)
  //    case word if word.contains('[') && word.endsWith("]") =>
  //      word.span('[' !=) match {
  //        case (position, pockets) => position -> pockets.stripPrefix("[").stripSuffix("]").some
  //      }
  //    case word => word -> None
  //  }
  //  makePiecesWithCrazyPromoted(position.toList, 0, 7) map { case (pieces, promoted) =>
  //    val board = Board(pieces, variant = variant)
  //    if (promoted.isEmpty) board else board.withCrazyData(_.copy(promoted = promoted))
  //  } map { board =>
  //    pockets.fold(board) { str =>
  //      val (white, black) = str.toList.flatMap(Piece.fromChar).partition(_ is White)
  //      board.withCrazyData(
  //        _.copy(
  //          pockets = Pockets(
  //            white = Pocket(white.map(_.role).map(strategygames.Role.FairySFRole)),
  //            black = Pocket(black.map(_.role).map(strategygames.Role.FairySFRole))
  //          )
  //        )
  //      )
  //    }
  //  }
  //}

  private def makePiecesWithCrazyPromoted(
      chars: List[Char],
      x: Int,
      y: Int
  ): Option[(List[(Pos, Piece)], Set[Pos])] =
    chars match {
      case Nil                               => Option((Nil, Set.empty))
      case '/' :: rest                       => makePiecesWithCrazyPromoted(rest, 0, y - 1)
      case c :: rest if '1' <= c && c <= '8' => makePiecesWithCrazyPromoted(rest, x + (c - '0').toInt, y)
      case c :: '~' :: rest =>
        for {
          pos                        <- Pos.at(x, y)
          piece                      <- Piece.fromChar(c)
          (nextPieces, nextPromoted) <- makePiecesWithCrazyPromoted(rest, x + 1, y)
        } yield (pos -> piece :: nextPieces, nextPromoted + pos)
      case c :: rest =>
        for {
          pos                        <- Pos.at(x, y)
          piece                      <- Piece.fromChar(c)
          (nextPieces, nextPromoted) <- makePiecesWithCrazyPromoted(rest, x + 1, y)
        } yield (pos -> piece :: nextPieces, nextPromoted)
    }

  def >>(situation: Situation): FEN = >>(SituationPlus(situation, 1))

  def >>(parsed: SituationPlus): FEN =
    parsed match {
      case SituationPlus(situation, _) => >>(Game(situation, turns = parsed.turns))
    }

  def >>(game: Game): FEN = FEN {
    {
      List(
        exportBoard(game.board) + exportCrazyPocket(game.board),
        game.player.letter,
        exportCastles(game.board),
        game.situation.enPassantSquare.map(_.toString).getOrElse("-"),
        game.halfMoveClock,
        game.fullMoveNumber
      )
    } mkString " "
  }

  def exportStandardPositionTurnCastlingEp(situation: Situation): String =
    List(
      exportBoard(situation.board),
      situation.color.letter,
      exportCastles(situation.board),
      situation.enPassantSquare.map(_.toString).getOrElse("-")
    ) mkString " "

  //private def exportCheckCount(board: Board) =
  //  board.history.checkCount match {
  //    case CheckCount(white, black) => s"+$black+$white"
  //  }

  private def exportCrazyPocket(board: Board) =
    board.crazyData match {
      case Some(PocketData(pockets, _)) =>
        "/" +
          pockets.white.roles.map(_.forsyth).map(_.toUpper).mkString +
          pockets.black.roles.map(_.forsyth).mkString
      case _ => ""
    }

  implicit private val posOrdering = Ordering.by[Pos, File](_.file)

  //stub
  private[fairysf] def exportCastles(board: Board): String = "-"//{

  //  lazy val wr = board.pieces.collect {
  //    case (pos, piece) if pos.rank == Rank.backRank(White) && piece == Piece(White, Rook) => pos
  //  }
  //  lazy val br = board.pieces.collect {
  //    case (pos, piece) if pos.rank == Rank.backRank(Black) && piece == Piece(Black, Rook) => pos
  //  }

  //  lazy val wur = board.unmovedRooks.pos.filter(_.rank == Rank.backRank(White))
  //  lazy val bur = board.unmovedRooks.pos.filter(_.rank == Rank.backRank(Black))

  //  {
  //    // castling rights with inner rooks are represented by their file name
  //    (if (board.castles.whiteKingSide && wr.nonEmpty && wur.nonEmpty)
  //       (if (wur contains wr.max) "K" else wur.max.file.toUpperCaseString)
  //     else "") +
  //      (if (board.castles.whiteQueenSide && wr.nonEmpty && wur.nonEmpty)
  //         (if (wur contains wr.min) "Q" else wur.min.file.toUpperCaseString)
  //       else "") +
  //      (if (board.castles.blackKingSide && br.nonEmpty && bur.nonEmpty)
  //         (if (bur contains br.max) "k" else bur.max.file)
  //       else "") +
  //      (if (board.castles.blackQueenSide && br.nonEmpty && bur.nonEmpty)
  //         (if (bur contains br.min) "q" else bur.min.file)
  //       else "")
  //  } match {
  //    case "" => "-"
  //    case n  => n
  //  }
  //}

  def exportBoard(board: Board): String = {
    val fen   = new scala.collection.mutable.StringBuilder(70)
    var empty = 0
    for (y <- Rank.allReversed) {
      empty = 0
      for (x <- File.all) {
        board(x, y) match {
          case None => empty = empty + 1
          case Some(piece) =>
            if (empty == 0) fen append piece.forsyth.toString
            else {
              fen append (empty.toString + piece.forsyth)
              empty = 0
            }
            if (piece.role != Pawn && board.crazyData.fold(false)(_.promoted.contains(Pos(x, y))))
              fen append '~'
        }
      }
      if (empty > 0) fen append empty
      if (y > Rank.First) fen append '/'
    }
    fen.toString
  }

  def boardAndColor(situation: Situation): String =
    boardAndColor(situation.board, situation.color)

  def boardAndColor(board: Board, turnColor: Color): String =
    s"${exportBoard(board)} ${turnColor.letter}"
}
