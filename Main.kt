package indigo

import kotlin.system.exitProcess

class ExceptionExit : Exception()
class IndigoCardGame {
    init {
        println("Indigo Card Game")
    }
    private val playerList = listOf(Player(1, "Player"), Computer(2, "Computer"))
    private val lastWinner = mutableListOf<Player>()
    private val firstMovePlayer = mutableListOf<Player>()
    private var player: Player
        init {
            player = firstPlayer()
            firstMovePlayer.add(player)
        }
    private val ranksAndValueMap = HashMap<String, Int>().apply {
        this["A"] = 1
        this["2"] = 0
        this["3"] = 0
        this["4"] = 0
        this["5"] = 0
        this["6"] = 0
        this["7"] = 0
        this["8"] = 0
        this["9"] = 0
        this["10"] = 1
        this["J"] = 1
        this["Q"] = 1
        this["K"] = 1
    }
    private val suits = setOf('\u2660', '\u2663', '\u2665', '\u2666')
    private val deck = Deck(ranksAndValueMap, suits)
    private val table: CardTable
    init {
        table = CardTable
        table.cardOnTable(deck.getCard(4))
        println("Initial cards on the table: ${table.cardsOnTable.joinToString(" ")}")
    }
    private val isEmptyCard
        get() = playerList.sumOf { it.cardsNumber } + table.cardsNumberOnTable == deck.cardNumberDeck


    fun play() {

        while (!isEmptyCard){
            while (player.playerHand.cardsNumberOnHand != 0) {
                println(table)
                val cardOnTable = try{ player.move() } catch (e: ExceptionExit) { gameOver(); break }
                determiningWinner(cardOnTable, player)
                player = nextPlayer(player)
            }
            if (!isEmptyCard) handOutCards(12)
        }
        gameOver()
    }

    private fun determiningWinner(_cardOnTable: Card, _player: Player) {
        val isNotEmpty = playerList.sumOf { it.cardsNumber } + table.cardsNumberOnTable + 1 != deck.cardNumberDeck
        val playerWin = if (lastWinner.isNotEmpty()) lastWinner.last() else firstMovePlayer.first()
        if (table.cardsNumberOnTable > 0) {
            if (_cardOnTable == table.cardOn) {
                finally(_cardOnTable, _player, true)
                if (!isNotEmpty) finally(null, _player, false, false)
            } else {
                if (isNotEmpty) {
                    table.cardOnTable(_cardOnTable)
                } else {
                    finally(_cardOnTable, playerWin, false)
                }
            }
        } else {
            if (isNotEmpty) {
                firstMovePlayer.add(_player)
                table.cardOnTable(_cardOnTable)
            } else  {
                finally(_cardOnTable, playerWin, false)
            }
        }
    }

    private fun finally(_cardOnTable: Card?, _player: Player, _isNotEmpty: Boolean, _isPrizer: Boolean = _isNotEmpty) {
        assignmentWinner(_cardOnTable, _player, _isNotEmpty)
        if (!_isPrizer) priser()
        printScore()
    }

    private fun priser() {
        val playerPrizerList = playerList.filter { i -> i.cardsNumber == playerList.maxOf { it.cardsNumber } }
        val playerPrizer = if (playerPrizerList.count() > 1) firstMovePlayer.first() else playerPrizerList.first()
        playerPrizer.addPrizePoints(3)
    }

    private fun assignmentWinner(_cardOnTable: Card?, _player: Player, _isNotEmpty: Boolean) {
        if (_cardOnTable != null) {
            table.cardOnTable(_cardOnTable)
        }
        if (_isNotEmpty) println("${_player.name} wins cards") else println(table)
        _player.putInBasket(table.moveCardsOnTable())
        lastWinner.add(_player)
    }

    private fun printScore() {
        val total: List<MutableList<String>> = listOf(
            MutableList(2) { "${playerList[it].name} ${playerList[it].points}"} ,
            MutableList(2) { "${playerList[it].name} ${playerList[it].cardsNumber}"})
        println(total[0].joinToString(" - ", "Score: "))
        println(total[1].joinToString(" - ","Cards: "))
    }

    private fun handOutCards(_number: Int) {        val listHandOut = deck.handOutCards(_number)
        playerList[0].playerHand.cardsOnHand(listHandOut.first)
        playerList[1].playerHand.cardsOnHand(listHandOut.second)
    }

    private fun firstPlayer(): Player {
        while (true) {
            println("Play first?")
            when (readln().lowercase()) {
                "yes" -> {
                    return playerList[0]
                }
                "no" -> {
                    return playerList[1]
                }
                "exit" -> {
                    gameOver()
                }
            }
        }
    }

    private fun nextPlayer(_actualPlayer: Player): Player {
        return if (_actualPlayer.id == playerList.count()) playerList.first() else playerList[_actualPlayer.id]
    }

    private fun gameOver() {
        println("Game Over")
        exitProcess(0)
    }

    //-------------------Card-Begin---------------------------------------------------------------------
    data class Card(val ranks: String, val suits: Char, val value: Int) {

        override fun toString(): String = "$ranks$suits"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Card
            return if (ranks == other.ranks || suits == other.suits) return true else false
        }
    }

    //-------------------Card-End---------------------------------------------------------------------
//-------------------Deck-Begin---------------------------------------------------------------------
    class Deck(_ranksAndValueMap: Map<String, Int>, _suits: Set<Char>) {

        private var deck = HashSet<Card>().apply {
            for (i in _suits) {
                for (j in _ranksAndValueMap.keys) {
                    this.add(Card(j, i, _ranksAndValueMap[j] ?: 0))
                }
            }
        }.shuffled().toMutableSet()
        val cardNumberDeck = deck.count()

        override fun toString() = deck.joinToString(" ")

        fun getCard(_number: Int): Set<Card> {
            val takeDeck = deck.take(_number).toSet()
            deck = deck.drop(_number).toMutableSet()
            return takeDeck
        }

        fun handOutCards(_number: Int): Pair<List<Card>, List<Card>> {
            val cardList = getCard(_number).toList()
            return Pair(cardList.filterIndexed { i, _ -> (i + 1) % 2 != 0 },
                cardList.filterIndexed { i, _ -> (i + 1) % 2 == 0 })
        }

    }

    //-------------------Deck-End---------------------------------------------------------------------

//-------------------Player-Begin---------------------------------------------------------------------
open class Player(_id: Int, _name: String) {
    val id: Int = _id
    val name: String = _name
    val playerHand = Hand()
    private val basketOfCards = mutableListOf<Card>()
    var points = score()
    var cardsNumber = cardsCount()

    open fun move(): Card {
        println(playerHand)
        while(true) {
            println("Choose a card to play (1-${playerHand.cardsNumberOnHand}):")
            val input = readln()
            if (input.lowercase() == "exit") throw ExceptionExit()
            val inputCheck = "[1-${playerHand.cardsNumberOnHand}]".toRegex()
            if (input.matches(inputCheck)) {
                return playerHand.cardMove(input.toInt())
            }
        }
    }

    private fun score(): Int = basketOfCards.sumOf { it.value }
    fun putInBasket(_cardsOnTable: List<Card>) {
        basketOfCards.addAll(_cardsOnTable)
        points = score()
        cardsNumber = cardsCount()
    }

    private fun cardsCount() = basketOfCards.count()

    fun addPrizePoints(_prizePoints: Int) {
        points += _prizePoints
    }

        //----//-------------------Player.Hand-Begin---------------------------------------------------------------
        open class Hand {
            val cardsNumberOnHand get() = hand.count()
            val hand = emptyList<Card>().toMutableList()

            override fun toString(): String {
                var str = "Cards in hand: "
                for (i in hand.indices) {
                    str += "${i + 1})${hand[i]} "
                }
                return str
            }

            fun cardsOnHand(_cardsOnHand: List<Card>) {
                hand.addAll(_cardsOnHand)
            }

            fun cardMove(_number: Int): Card {
                return hand.removeAt(_number - 1)
            }

            fun cardMove(_card: Card): Card {
                hand.removeIf { it.suits == _card.suits && it.ranks == _card.ranks}//(_card)
                return _card
            }

        }
//----//-------------------Player.Hand-Begin---------------------------------------------------------------
}
//-------------------Player-End---------------------------------------------------------------------

    //-------------------Computer-Begin---------------------------------------------------------------------
    class Computer(_id: Int, _name: String) : Player(_id, _name){

        override fun move(): Card  {
            println(playerHand.hand.joinToString(" "))
            val cardForMove: Card

            if (playerHand.hand.count() == 1) {
                cardForMove = playerHand.hand.first()
            } else {
                if (cardsOnTable.isEmpty()) {
                    cardForMove = chooseCardForMove(playerHand.hand, null)
                } else {
                    when (numbercandidateCards()) {
                        0 -> { cardForMove = chooseCardForMove(playerHand.hand, null) }
                        1 -> { cardForMove = playerHand.hand.filter { it == cardOn }.first() }
                        else -> { cardForMove = chooseCardForMove(playerHand.hand, cardOn) }
                    }
                }
            }
            val cardMove = playerHand.cardMove(cardForMove)
            println("Computer plays $cardMove")
            return cardMove
        }

        fun numbercandidateCards(): Int {
            return playerHand.hand.count { it == cardOn }
        }

        fun chooseCardForMove(
            _cardsOnHand: List<Card> = playerHand.hand,
            _compareCard: Card?
        ): Card {
            val cardList: MutableList<Card> = mutableListOf()
            val groupSuits = if (_compareCard != null) {
                _cardsOnHand.groupBy { it.suits.toString() }.filterKeys { it == _compareCard.suits.toString() }.values.flatten()
            } else {
                _cardsOnHand.groupBy { it.suits.toString() }.filterValues { it.count() > 1}.values.flatten()
            }

            if (groupSuits.isNotEmpty()) {
                if (groupSuits.count() > 1) {
                return groupSuits.random()
                } else { cardList += groupSuits }
            }

            val groupRanks = if (_compareCard != null) {
                _cardsOnHand.groupBy { it.ranks }.filterKeys { it == _compareCard.ranks }.values.flatten()
            } else {
                _cardsOnHand.groupBy { it.ranks }.filterValues { it.count() > 1}.values.flatten()
            }

            if (groupRanks.isNotEmpty()) {
                if (groupRanks.count() > 1) {
                    return groupRanks.random()
                } else { cardList += groupRanks }
            }
            if (cardList.isNotEmpty()) {
                cardList.shuffle()
                return cardList.random()
            } else {
                playerHand.hand.shuffle()
                return playerHand.hand.random()
            }
        }
    }
    //-------------------Computer-End---------------------------------------------------------------------

    //-------------------CardTable-Begin---------------------------------------------------------------------
    companion object CardTable/*(_cardsOnTable: Set<Card>)*/ {
        val cardsOnTable = emptyList<Card>().toMutableList()
        val cardsNumberOnTable get() = cardsOnTable.count()
        val cardOn  get() = cardsOnTable.last()

        fun cardOnTable(_card: Card) {
            cardsOnTable.add(_card)
        }
        fun cardOnTable(_cardsOnTable: Set<Card>) {
            cardsOnTable.addAll(_cardsOnTable)
        }
        private fun clearTable() {
            cardsOnTable.clear()
        }
        fun moveCardsOnTable(): List<Card> {
            val list = cardsOnTable.toList()
            clearTable()
            return list
        }

        override fun toString(): String {
            return  if (cardsOnTable.isNotEmpty()) {
                "\n$cardsNumberOnTable cards on the table, and the top card is $cardOn"
            } else {
                "\nNo cards on the table"
            }
        }
    }
//-------------------CardTable-End---------------------------------------------------------------------
}



fun main() {
     IndigoCardGame().play()

}