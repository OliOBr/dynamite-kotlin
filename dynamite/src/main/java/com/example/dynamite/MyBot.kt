package com.example.dynamite

import com.softwire.dynamite.bot.Bot
import com.softwire.dynamite.game.Gamestate
import com.softwire.dynamite.game.Move
import com.softwire.dynamite.game.Round
import java.lang.Math.max
import java.lang.Math.min
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.floor


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
            val lastRoundOutcome = getOutcome(lastRound)
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
            if(valueOfRound >= 3 && myDynamiteSticks != 0) {
                whatBeatsThisHighstakes(nextMove)
            } else{
                whatBeatsThis(nextMove)
            }
        } else {
            if(myDynamiteSticks != 0 && valueOfRound >= 3) {
                Move.D
            } else {
                pickRandomMove()
            }
        }
    }
    //TODO: Consider changing the minimum value for highstakes strat

    fun detectBeatPreviousMoveStrat(){

    }

    fun shouldPlayWater(gamestate: Gamestate, riskFactor: Double): Boolean {
        var wins = 0
        var draws = 0
        var losses = 0
        for (gameRound in gamestate.rounds) {
            if(gameRound.p1 == Move.W){
                when(getOutcome(gameRound)){
                    Outcome.WIN -> wins++
                    Outcome.DRAW -> draws++
                    Outcome.LOSS -> losses++
                }
            }
        }
        return if (wins+draws ==0) {
            true
        } else{
            riskFactor*(wins+draws).toDouble()/(wins+draws+losses) >= ThreadLocalRandom.current().nextDouble()
        }
    }

    fun nextInSequenceOfDraws(gamestate: Gamestate): Pair<Boolean, Move>{
        val currentSnippet = getCurrentSequenceOfDraws(gamestate)
        val nextMoves = getMoveAfterThisSequenceOfDraws(gamestate, currentSnippet)
        if (nextMoves.size > 2){
            if(theirDynamiteSticks == 0 || !shouldPlayWater(gamestate,1.0)) {
                nextMoves.removeIf { it == Move.D }
                return if(nextMoves.isNotEmpty()){
                    Pair(true, nextMoves.shuffled().first())
                } else {
                    Pair(false, Move.R)
                }
            }
            return Pair(true,nextMoves.takeLast(2).shuffled().first())
        } else {
            if(detectSpammedMoves(gamestate, 2) && gamestate.rounds.last().p2 == Move.D) {
                if(currentSnippet.size < 7) {
                    return Pair(true, Move.D)
                }else {
                    return Pair(true, Move.W)
                }
            }
        }
        return Pair(false, Move.R)
    }

    fun getMoveAfterThisSequenceOfDraws(gamestate: Gamestate, currentSnippet: List<Round>): MutableList<Move> {
        val drawStates = getAllDraws(gamestate)
        val numOfDraws = currentSnippet.size
        var nextMoves = mutableListOf<Move>()
        var range = 0 until (drawStates.size - numOfDraws)
        for (index in range) {
            if(drawStates[index + numOfDraws - 1].first - drawStates[index].first ==  numOfDraws - 1 && (drawStates[index].first ==0 || !isDraw(gamestate.rounds[drawStates[index].first - 1]))){
                val snippet = drawStates.slice(index..(index+numOfDraws-1))
                var matchesCurrent = true
                snippet.forEachIndexed{ i, drawState -> if(!(matchType(drawState.second.p2, currentSnippet[i].p2))) {matchesCurrent = false}  }
                if(matchesCurrent) {
                    nextMoves.add(gamestate.rounds[drawStates[index].first + numOfDraws].p2)
                }
            }
        }
        return nextMoves
    }

    fun getAllDraws(gamestate: Gamestate): Array<Pair<Int,Round>>{
        var drawStates = arrayOf<Pair<Int,Round>>()
        var range = 0 until (roundNum -2)
        for (index in range) {
            if(isDraw(gamestate.rounds[index])){
                drawStates += Pair(index,gamestate.rounds[index])
            }
        }
        return  drawStates
    }

    fun getCurrentSequenceOfDraws(gamestate: Gamestate): List<Round>{
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
        return gamestate.rounds.takeLast(numOfDraws)
    }

    fun isDraw(gameRound: Round): Boolean{
        return gameRound.p1 == gameRound.p2
    }

    fun matchType(move1: Move, move2: Move): Boolean {
        return when(move1){
            Move.W -> move2==Move.W
            Move.D -> move2==Move.D
            Move.S -> when(move2){
                Move.S -> true
                Move.P -> true
                Move.R -> true
                else -> false
            }
            Move.R -> when(move2){
                Move.S -> true
                Move.P -> true
                Move.R -> true
                else -> false
            }
            Move.P -> when(move2){
                Move.S -> true
                Move.P -> true
                Move.R -> true
                else -> false
            }
        }
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

        val currentRateOfDynamiteUse = max((100-myDynamiteSticks).toDouble()/roundNum,0.1)
        val dynamiteProbabilty = if (myDynamiteSticks != 0 && (myDynamiteSticks > expectedTurnsLeft*currentRateOfDynamiteUse)){
                myDynamiteSticks.toDouble()/expectedTurnsLeft // Only drop dynamite randomly to prevent finishing the game with excess
        } else {
            0.0
        }
        val waterProbabilty = if(theirDynamiteSticks == 100){
            0.0
        } else {
            min(theirDynamiteSticks.toDouble()/expectedTurnsLeft,0.0) // effectively 0
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
        return Move.S
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
            Move.W -> randomRPS()
            Move.R -> Move.P
            Move.S -> Move.R
            Move.P -> Move.S
        }
    }
    fun randomRPS() : Move {
        val randomNumberBetween0and3 = floor(ThreadLocalRandom.current().nextDouble() * 3.0).toInt()
        val listOfMoves = listOf<Move>(Move.P,Move.R,Move.S)
        return listOfMoves[randomNumberBetween0and3]
    }

    fun whatBeatsThisHighstakes(move: Move): Move {
        return when(move) {
            Move.D -> Move.W
            Move.W -> randomRPS()
            Move.R -> Move.D
            Move.S -> Move.D
            Move.P -> Move.D
        }
    }

    fun getOutcome(gameRound: Round): Outcome{
        val ourMove = gameRound.p1
        val theirMove = gameRound.p2
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