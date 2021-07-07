package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move

class MyBot : Bot {
    var dynamiteSticks = 100
    var roundNumber = 0
    var myCurrentScore = 0
    override fun makeMove(gamestate: Gamestate): Move {
        // Are you debugging?
        // Put a breakpoint in this method to see when we make a move

        var roundsLeftTillWin =
        roundNumber ++
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