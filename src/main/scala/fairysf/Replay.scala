package strategygames.fairysf

import cats.data.Validated
import cats.data.Validated.{ invalid, valid }
import cats.implicits._

import strategygames.Color
import strategygames.format.pgn.San
import strategygames.fairysf.format.pgn.{ Parser, Reader }
import strategygames.format.pgn.{ Tag, Tags }
import strategygames.fairysf.format.{ FEN, Forsyth, Uci }
import strategygames.{ Game => StratGame, Situation => StratSituation }

case class Replay(setup: Game, moves: List[MoveOrDrop], state: Game) {

  lazy val chronoMoves = moves.reverse

  def addMove(moveOrDrop: MoveOrDrop) =
    copy(
      moves = moveOrDrop.left.map(_.applyVariantEffect) :: moves,
      state = moveOrDrop.fold(state.apply, state.applyDrop)
    )

  def moveAtPly(ply: Int): Option[MoveOrDrop] =
    chronoMoves lift (ply - 1 - setup.startedAtTurn)
}

object Replay {

  def apply(game: Game) = new Replay(game, Nil, game)

  def apply(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Validated[String, Reader.Result] =
    moveStrs.some.filter(_.nonEmpty) toValid "[replay] pgn is empty" andThen { nonEmptyMoves =>
      Reader.moves(
        nonEmptyMoves,
        Tags(
          List(
            initialFen map { fen =>
              Tag(_.FEN, fen.value)
            },
            variant.some.filterNot(_.shogi) map { v =>
              Tag(_.Variant, v.name)
            }
          ).flatten
        )
      )
    }

  def replayMove(
    before: Game,
    orig: Pos,
    dest: Pos,
    promotion: String,
    apiPosition: Api.Position,
    uciMoves: List[String]
  ): Move =
    Move(
      piece = before.situation.board.pieces(orig),
      orig = orig,
      dest = dest,
      situationBefore = before.situation,
      after = before.situation.board.copy(
        pieces = apiPosition.pieceMap,
        uciMoves = uciMoves,
        pocketData = apiPosition.pocketData,
        position = apiPosition.some
      ),
      capture = None,
      promotion = promotion match {
        case "" => None
        case _ => Role.promotable(
          before.board.variant.gameFamily,
          before.board.pieces(orig).role.forsyth
        )
      },
      castle = None,
      enpassant = false
    )

  def replayDrop(
    before: Game,
    role: Role,
    dest: Pos,
    apiPosition: Api.Position,
    uciMoves: List[String]
  ): Drop =
    Drop(
      piece = Piece(before.situation.color, role),
      pos = dest,
      situationBefore = before.situation,
      after = before.situation.board.copy(
        pieces = apiPosition.pieceMap,
        uciMoves = uciMoves,
        pocketData = apiPosition.pocketData,
        position = apiPosition.some
      ),
    )


  def gameMoveWhileValid(
      moveStrs: Seq[String],
      initialFen: FEN,
      variant: strategygames.fairysf.variant.Variant
  ): (Game, List[(Game, Uci.WithSan)], Option[String]) = {

    val init = makeGame(variant, initialFen.some)
    var state = init
    var uciMoves = init.situation.board.uciMoves
    var errors = ""

    def getApiPosition(uciMove: String) = state.board.apiPosition.makeMoves(List(uciMove))

    def replayMoveFromUci(orig: Option[Pos], dest: Option[Pos], promotion: String): (Game, Uci.WithSan) =
      (orig, dest) match {
        case (Some(orig), Some(dest)) => {
          val uciMove = s"${orig.key}${dest.key}${promotion}"
          val pgnMove = s"${orig.key}${dest.key}${promotion match {
            case "" => ""
            case _ => state.board.pieces(orig).role.forsyth
          }}"
          uciMoves = uciMoves :+ uciMove
          state = state.apply(
            replayMove(state, orig, dest, promotion, getApiPosition(uciMove), uciMoves)
          )
          (state, Uci.WithSan(Uci.apply(state.board.variant.gameFamily, pgnMove).get, "NOSAN"))
        }
        case (orig, dest) => {
          val uciMove = s"${orig}${dest}${promotion}"
          errors += uciMove + ","
          sys.error(s"Invalid move for replay: ${uciMove}")
        }
      }

    def replayDropFromUci(role: Option[Role], dest: Option[Pos]): (Game, Uci.WithSan) =
      (role, dest) match {
        case (Some(role), Some(dest)) => {
          val uciDrop = s"${role.forsyth}@${dest.key}"
          uciMoves = uciMoves :+ uciDrop
          state = state.applyDrop(
            replayDrop(state, role, dest, getApiPosition(uciDrop), uciMoves)
          )
          (state, Uci.WithSan(Uci.apply(state.board.variant.gameFamily, uciDrop).get, "NOSAN"))
        }
        case (role, dest) => {
          val uciDrop = s"${role}@${dest}"
          errors += uciDrop + ","
          sys.error(s"Invalid drop for replay: ${uciDrop}")
        }
      }

    val moves: List[(Game, Uci.WithSan)] = Parser.pgnMovesToUciMoves(moveStrs)
      .map{
        case Uci.Move.moveR(orig, dest, promotion) => replayMoveFromUci(
          Pos.fromKey(orig),
          Pos.fromKey(dest),
          promotion
        )
        case Uci.Drop.dropR(role, dest) => replayDropFromUci(
          Role.allByForsyth(init.situation.board.variant.gameFamily).get(role(0)),
          Pos.fromKey(dest)
        )
        case moveStr: String => sys.error(s"Invalid moveordrop for replay: $moveStr")
      }

    (init, moves, errors match {case "" => None; case _ => errors.some})
  }

  private def recursiveSituations(sit: Situation, sans: List[San]): Validated[String, List[Situation]] =
    sans match {
      case Nil => valid(Nil)
      case san :: rest =>
        san(StratSituation.wrap(sit)) flatMap { moveOrDrop =>
          val after = Situation(moveOrDrop.fold(m => m.finalizeAfter().toFairySF, d => d.finalizeAfter.toFairySF), !sit.color)
          recursiveSituations(after, rest) map { after :: _ }
        }
    }

  private def recursiveSituationsFromUci(
      sit: Situation,
      ucis: List[Uci]
  ): Validated[String, List[Situation]] =
    ucis match {
      case Nil => valid(Nil)
      case uci :: rest =>
        uci(sit) andThen { moveOrDrop =>
          val after = Situation(moveOrDrop.fold(_.finalizeAfter, _.finalizeAfter), !sit.color)
          recursiveSituationsFromUci(after, rest) map { after :: _ }
        }
    }

  private def recursiveReplayFromUci(replay: Replay, ucis: List[Uci]): Validated[String, Replay] =
    ucis match {
      case Nil => valid(replay)
      case uci :: rest =>
        uci(replay.state.situation) andThen { moveOrDrop =>
          recursiveReplayFromUci(replay addMove moveOrDrop, rest)
        }
    }

  private def initialFenToSituation(
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Situation = {
    initialFen.flatMap(Forsyth.<<) | Situation(strategygames.fairysf.variant.Variant.default)
  } withVariant variant

  def boards(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Validated[String, List[Board]] = situations(moveStrs, initialFen, variant) map (_ map (_.board))

  def situations(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Validated[String, List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    Parser.moves(moveStrs, sit.board.variant) andThen { moves =>
      recursiveSituations(sit, moves.value) map { sit :: _ }
    }
  }

  def boardsFromUci(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Validated[String, List[Board]] = situationsFromUci(moves, initialFen, variant) map (_ map (_.board))

  def situationsFromUci(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Validated[String, List[Situation]] = {
    val sit = initialFenToSituation(initialFen, variant)
    recursiveSituationsFromUci(sit, moves) map { sit :: _ }
  }

  def apply(
      moves: List[Uci],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant
  ): Validated[String, Replay] =
    recursiveReplayFromUci(Replay(makeGame(variant, initialFen)), moves)

  def plyAtFen(
      moveStrs: Iterable[String],
      initialFen: Option[FEN],
      variant: strategygames.fairysf.variant.Variant,
      atFen: FEN
  ): Validated[String, Int] =
    if (Forsyth.<<@(variant, atFen).isEmpty) invalid(s"Invalid FEN $atFen")
    else {

      // we don't want to compare the full move number, to match transpositions
      def truncateFen(fen: FEN) = fen.value.split(' ').take(4) mkString " "
      val atFenTruncated        = truncateFen(atFen)
      def compareFen(fen: FEN)  = truncateFen(fen) == atFenTruncated

      def recursivePlyAtFen(sit: Situation, sans: List[San], ply: Int): Validated[String, Int] =
        sans match {
          case Nil => invalid(s"Can't find $atFenTruncated, reached ply $ply")
          case san :: rest =>
            san(StratSituation.wrap(sit)) flatMap { moveOrDrop =>
              val after = moveOrDrop.fold(m => m.finalizeAfter().toFairySF, d => d.finalizeAfter.toFairySF)
              val fen   = Forsyth >> Game(Situation(after, Color.fromPly(ply)), turns = ply)
              if (compareFen(fen)) Validated.valid(ply)
              else recursivePlyAtFen(Situation(after, !sit.color), rest, ply + 1)
            }
        }

      val sit = initialFen.flatMap {
        Forsyth.<<@(variant, _)
      } | Situation(variant)

      Parser.moves(moveStrs, sit.board.variant) andThen { moves =>
        recursivePlyAtFen(sit, moves.value, 1)
      }
    }

  private def makeGame(variant: strategygames.fairysf.variant.Variant, initialFen: Option[FEN]): Game = {
    val g = Game(variant.some, initialFen)
    g.copy(startedAtTurn = g.turns)
  }
}
