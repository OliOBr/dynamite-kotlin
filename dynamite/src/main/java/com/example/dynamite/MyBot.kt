package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.lang.Math.max
import java.lang.Math.min
import java.util.concurrent.ThreadLocalRandom


class MyBot : Bot {
    var myDynamiteSticks = 100
    var theirDynamiteSticks = 100
    var roundNum = 0
    var myCurrentScore = 0
    var theirCurrentScore = 0
    var valueOfRound = 1


    override fun makeMove(gamestate: Gamestate): Move {
        roundNum ++
        return if (roundNum > 1) {
            val lastRound = gamestate.rounds.last()
            val lastRoundOutcome = getOutcome(lastRound.p1,lastRound.p2)
            updateScores(lastRoundOutcome)
            updateDynamite(lastRound)
            if(roundNum > 5 && detectSpammedMoves(gamestate, 5)){
                whatBeatsThis(lastRound.p2)
            } else if (lastRoundOutcome == Outcome.DRAW) {
                onDraw(gamestate)
            } else {
                pickRandomMove()
            }
        } else {
            Move.W
        }
    }

    fun onDraw(gamestate: Gamestate): Move {
        val (drawSequenceSeenBefore, nextMove) = nextInSequenceOfDraws(gamestate)
        return if(drawSequenceSeenBefore){
            //print(nextMove)
            whatBeatsThis(nextMove)
        } else {
            if(myDynamiteSticks != 0 && valueOfRound >= 3) {
                Move.D
            } else {
                pickRandomMove()
            }
        }
    }

    fun detectBeatPreviousMoveStrat(){

    }

    fun nextInSequenceOfDraws(gamestate: Gamestate): Pair<Boolean, Move>{
        var stillDrawing = true
        var numOfDraws = 0
        while(stillDrawing) {
            if(numOfDraws + 1 <= gamestate.rounds.size) {
                val possibleDraw = gamestate.rounds.takeLast(numOfDraws + 1)[0]
                if (possibleDraw.p1 == possibleDraw.p2) {
                    numOfDraws++
                } else {
                    stillDrawing = false
                }
            } else {
                stillDrawing = false
            }
        }
        val currentSnippet = gamestate.rounds.takeLast(numOfDraws)
        var drawStates = arrayOf<Pair<Int,Round>>()
        var range = 0 until (roundNum -2)
        for (index in range) {
            if(gamestate.rounds[index].p1 == gamestate.rounds[index].p2){
                drawStates += Pair(index,gamestate.rounds[index])
            }
        }
        var nextMoves = mutableListOf<Move>()
        range = 0 until (drawStates.size - numOfDraws)
        for (index in range) {
            //print(drawStates[index + numOfDraws - 1].first - drawStates[index].first)
            //print(" ")
            if(drawStates[index + numOfDraws - 1].first - drawStates[index].first ==  numOfDraws - 1){
                // error is probably in this block, but also another error that occurs when running against itself so try that locally
                val snippet = drawStates.slice(index..(index+numOfDraws-1))
                var matchesCurrent = true
                snippet.forEachIndexed{ i, drawState -> if(drawState.second.p2 != currentSnippet[i].p2) {matchesCurrent = false}  }
                if(matchesCurrent) {
                    nextMoves.add(gamestate.rounds[drawStates[index].first + numOfDraws].p2)
                }
            }
        }

        if (nextMoves.isNotEmpty()){
            //print(nextMoves)
            if(theirDynamiteSticks == 0) {
                nextMoves.removeIf { it == Move.D }
                return if(nextMoves.isNotEmpty()){
                    Pair(true, nextMoves.shuffled().first())
                } else {
                    Pair(false, Move.R)
                }
            }
            return Pair(true,nextMoves.shuffled().first())
        }
        return Pair(false, Move.R)
    }

    fun detectSameAfterDraw(gamestate: Gamestate): Pair<Boolean, Move> {
        var drawStates = mutableListOf<Round>()
        var range = 0 until (roundNum -2)
        for (index in range) {
            if(gamestate.rounds[index].p1 == gamestate.rounds[index].p2){
                drawStates.add(gamestate.rounds[index+1])
            }
        }
        if (drawStates.isNotEmpty()){
            return Pair(drawStates.all{it.p2 == drawStates.last().p2}, drawStates.last().p2)
        }
        return Pair(false, Move.D)
    }

    fun detectSpammedMoves(gamestate: Gamestate, n: Int): Boolean {
        val lastFiveRounds = gamestate.rounds.takeLast(n)
        return lastFiveRounds.all{it.p2 == gamestate.rounds.last().p2}
    }

    fun pickRandomMove(): Move {
        // Change water weighting depending on how much dynamite opponent has left
        val sample = ThreadLocalRandom.current().nextDouble()
        val maxCurrentScore = max(myCurrentScore, theirCurrentScore)
        val expectedTurnsLeft = (1000-maxCurrentScore)/(maxCurrentScore.toDouble()/(roundNum))
        val dynamiteProbabilty = if (myDynamiteSticks != 0) {
            myDynamiteSticks.toDouble()/expectedTurnsLeft
        } else {
            0.0
        }
        val waterProbabilty = if(theirDynamiteSticks == 100){
            0.0
        } else {
            min(theirDynamiteSticks.toDouble()/expectedTurnsLeft,0.0)
        }
        val rpsWeighting = (1-dynamiteProbabilty-waterProbabilty)/3
        val distribution = mapOf(
            Move.W to waterProbabilty,
            Move.P to rpsWeighting,
            Move.S to rpsWeighting,
            Move.R to rpsWeighting,
            Move.D to dynamiteProbabilty
        )
        var cumulativeProbability = 0.0;
        distribution.forEach{(k,v)->
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

    fun whatBeatsThis(move: Move): Move {
        return when(move) {
            Move.D -> Move.W
            Move.W -> Move.R
            Move.R -> Move.P
            Move.S -> Move.R
            Move.P -> Move.S
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