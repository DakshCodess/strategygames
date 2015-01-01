package chess

import chess.Variant.{AtomicChess}

class AtomicVariantTest extends ChessTest {

  "Atomic chess" should {

    "Must explode surrounding non pawn pieces on capture" in {
      val fenPosition = "rnbqkbnr/1ppppp1p/p5p1/8/8/1P6/PBPPPPPP/RN1QKBNR w KQkq -"
      val maybeGame = fenToGame(fenPosition, AtomicChess)
      val explodedSquares = List(Pos.H8, Pos.G8)
      val intactPawns = List(Pos.F7, Pos.G6, Pos.H7)

      val explosionGame = maybeGame flatMap (_.playMoves( (Pos.B2, Pos.H8) ))

      explosionGame must beSuccess.like{
        case game =>
          explodedSquares.forall(pos => game.situation.board(pos).isEmpty) must beTrue
          intactPawns.forall(pos => game.situation.board(pos).isDefined) must beTrue
      }
    }

    "Must explode all surrounding non pawn pieces on capture (contrived situation)" in {
      val fenPosition = "k7/3bbn2/3rqn2/3qr3/8/7B/8/1K6 w - -"
      val maybeGame = fenToGame(fenPosition, AtomicChess)
      val explodedSquares = List(Pos.D5, Pos.E5, Pos.D6, Pos.E6, Pos.F6, Pos.D7, Pos.E7, Pos.F7)

      val explosionGame = maybeGame flatMap (_.playMoves( (Pos.H3, Pos.E6) ))

      explosionGame must beSuccess.like {
        case game =>
          explodedSquares.forall(pos => game.situation.board(pos).isEmpty) must beTrue
      }
    }

    "Must explode all surrounding non pawn pieces on capture (contrived situation with bottom right position)" in {
      val fenPosition = "k7/3bbn2/3rqn2/4rq2/8/1B6/8/K7 w - -"
      val maybeGame = fenToGame(fenPosition, AtomicChess)
      val explodedSquares = List(Pos.F5, Pos.E5, Pos.D6, Pos.E6, Pos.F6, Pos.D7, Pos.E7, Pos.F7)

      val explosionGame = maybeGame flatMap (_.playMoves( (Pos.B3, Pos.E6) ))

      explosionGame must beSuccess.like {
        case game =>
          explodedSquares.forall(pos => game.situation.board(pos).isEmpty) must beTrue
      }
    }

    "Not allow a king to capture a piece" in {
      val fenPosition = "8/8/8/1k6/8/8/8/1Kr5 w - -"
      val maybeGame = fenToGame(fenPosition, AtomicChess)

      val errorGame = maybeGame flatMap (_.playMoves((Pos.B1,Pos.C1)))

      errorGame must beFailure.like {
        case failMsg => failMsg mustEqual scalaz.NonEmptyList("A king cannot capture in atomic chess")
      }
    }

    "The game must end with the correct winner when a king explodes in the perimeter of a captured piece" in {
      val fenPosition = "rnb1kbnr/ppp1pppp/8/3q4/8/7P/PPPP1PP1/RNBQKBNR b KQkq -"
      val maybeGame = fenToGame(fenPosition, AtomicChess)

      val gameWin = maybeGame flatMap (_.playMoves((Pos.D5,Pos.D2)))

      gameWin must beSuccess.like{
        case winningGame =>
          winningGame.situation.end must beTrue
          winningGame.situation.variantEnd must beTrue
          winningGame.situation.winner must beSome.like{
            case winner => winner == Black
          }
      }
    }

    "The game must end by a traditional checkmate (atomic mate)" in {
      val fenPosition = "1k6/8/8/8/8/8/PP5r/K7 b - -"
      val maybeGame = fenToGame(fenPosition, AtomicChess)

      val gameWin = maybeGame flatMap (_.playMoves((Pos.H2,Pos.H1)))

      gameWin must beSuccess.like{
        case winningGame =>
          winningGame.situation.end must beTrue
          winningGame.situation.variantEnd must beFalse
          winningGame.situation.winner must beSome.like{case color => color == Black}
      }
    }

    "Must be a stalemate if a king could usually take a piece to get out of check, but can't because it would explode" in {
      val positionFen = "k7/8/1R6/8/8/8/8/5K2 w - -"
      val maybeGame = fenToGame(positionFen, AtomicChess)

      val gameWin = maybeGame flatMap (_.playMoves((Pos.B6,Pos.B7)))

      gameWin must beSuccess.like{
        case game =>
          game.situation.moves
          game.situation.end must beTrue
          game.situation.staleMate must beTrue
      }

    }

  }

}
