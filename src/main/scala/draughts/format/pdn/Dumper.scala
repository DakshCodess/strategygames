package strategygames.draughts
package format.pdn

object Dumper {

  def apply(_situation: Situation, data: strategygames.draughts.Move, _next: Situation): String =
    data.orig.shortKey + (if (data.captures) "x" else "-") + data.dest.shortKey

  def apply(data: strategygames.draughts.Move): String = apply(
    data.situationBefore,
    data,
    data.afterWithLastMove() situationOf !data.color
  )

}
