package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move

class MyBot : Bot {
    var dynamiteSticks = 100
    var roundNumber = 1
    var myCurrentScore = 0
    var theirCurrentScore = 0
    var valueOfRound = 1

    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move
        if (roundNumber > 1) {
            val lastRound = gamestate.rounds.last()
            val lastRoundOutcome = getOutcome(lastRound.p1,lastRound.p2)
            when(lastRoundOutcome) {
                Outcome.WIN -> {myCurrentScore += valueOfRound
                                valueOfRound = 1}
                Outcome.LOSS -> {theirCurrentScore += valueOfRound
                                valueOfRound = 1}
                Outcome.DRAW -> valueOfRound++
            }
        }
        if(myCurrentScore == 1000-valueOfRound || theirCurrentScore == 1000-valueOfRound || roundNumber == 2500){
            print("My final score: $myCurrentScore, Their final score: $theirCurrentScore \n")
        }
        roundNumber ++
        return Move.S
    }

    fun getOutcome(ourMove: Move, theirMove: Move): Outcome{
        return when(ourMove){
            Move.D -> when(theirMove){
                Move.W -> Outcome.LOSS
                Move.D -> Outcome.DRAW
                else -> Outcome.WIN
            }
            Move.W -> when(theirMove){
                Move.W -> Outcome.DRAW
                Move.D -> Outcome.WIN
                else -> Outcome.LOSS
            }
            Move.P -> when(theirMove){
                Move.W -> Outcome.WIN
                Move.D -> Outcome.LOSS
                Move.R -> Outcome.WIN
                Move.S -> Outcome.LOSS
                Move.P -> Outcome.DRAW
            }
            Move.R -> when(theirMove){
                Move.W -> Outcome.WIN
                Move.D -> Outcome.LOSS
                Move.R -> Outcome.DRAW
                Move.S -> Outcome.WIN
                Move.P -> Outcome.LOSS
            }
            Move.S -> when(theirMove){
                Move.W -> Outcome.WIN
                Move.D -> Outcome.LOSS
                Move.R -> Outcome.LOSS
                Move.S -> Outcome.DRAW
                Move.P -> Outcome.WIN
            }
        }
    }

    init {
        // Are you debugging?
        // Put a breakpoint on the line below to see when we start a new match
        println("Started new match")
    }
}