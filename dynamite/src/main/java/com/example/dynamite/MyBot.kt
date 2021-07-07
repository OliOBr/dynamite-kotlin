package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.lang.Math.max
import kotlin.random.Random

class MyBot : Bot {
    var myDynamiteSticks = 100
    var theirDynamiteSticks = 100
    var roundNum = 0
    var myCurrentScore = 0
    var theirCurrentScore = 0
    var valueOfRound = 1
    var waterWeighting = 0.0


    override fun makeMove(gamestate: Gamestate): Move {
        roundNum ++
        val move:Move
        if (roundNum > 1) {
            val lastRound = gamestate.rounds.last()
            val lastRoundOutcome = getOutcome(lastRound.p1,lastRound.p2)
            updateScores(lastRoundOutcome)
            updateDynamite(lastRound)
            move = pickRandomMove()
        } else {
            move = Move.W
        }
        printFinalScores()

        return move
    }

    fun pickRandomMove(): Move {
        val sample = Random.nextDouble()
        val maxCurrentScore = max(myCurrentScore, theirCurrentScore)
        val expectedTurnsLeft = (1000-maxCurrentScore)/(maxCurrentScore.toDouble()/(roundNum))
        val dynamiteProbabilty = myDynamiteSticks.toDouble()/expectedTurnsLeft
        val newDistribution = mutableMapOf<Move, Double>()
        val rpsWeighting = (1-waterWeighting)/3
        val distribution = mapOf(
            Move.D to dynamiteProbabilty,
            Move.W to waterWeighting*(1-dynamiteProbabilty),
            Move.P to rpsWeighting*(1-dynamiteProbabilty),
            Move.S to rpsWeighting*(1-dynamiteProbabilty),
            Move.R to rpsWeighting*(1-dynamiteProbabilty)
        )
        distribution.forEach{ (k, v) ->
            newDistribution[k] = v
        }
        var cumulativeProbability = 0.0;
        newDistribution.forEach{(k,v)->
            cumulativeProbability += v
            if(sample <= cumulativeProbability){
                return k
            }
        }
        return Move.R
    }

    fun printFinalScores() {
        if(myCurrentScore == 1000-valueOfRound || theirCurrentScore == 1000-valueOfRound || roundNum == 2500){
            print("My final score: $myCurrentScore, Their final score: $theirCurrentScore \n")
        }
    }

    fun updateDynamite(lastRound: Round){
        if (lastRound.p2 == Move.D) {
            theirDynamiteSticks--
        }
        if (lastRound.p1 == Move.D) {
            myDynamiteSticks--
        }
    }

    fun updateScores(lastRoundOutcome: Outcome){
        when(lastRoundOutcome) {
            Outcome.WIN -> {
                myCurrentScore += valueOfRound
                valueOfRound = 1
            }
            Outcome.LOSS -> {
                theirCurrentScore += valueOfRound
                valueOfRound = 1
            }
            Outcome.DRAW -> valueOfRound++
        }
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

        println("Started new match")
    }
}