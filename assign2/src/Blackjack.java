/*
 * 
    This implementation uses a standard 52-card deck and follows the basic rules of Blackjack. 
    The game starts by asking the player to place a bet, and then deals two cards to the player and two cards to the dealer. 
    The player can then choose to hit or stand, and the dealer will continue to hit until their score is at least 17. 
    The player wins if their score is higher than the dealer's score, without going over 21. 
    Aces are worth 11, and face cards are worth 10. 
    The player starts with $100 and can continue to play rounds until they run out of money.
    Note that this implementation is relatively simple and does not include more advanced features 
        such as splitting, doubling down, or insurance. 
    It can be customized or extended to include these features, or to add a graphical interface or networking capabilities for multiplayer support.

 * 
 */


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Blackjack {
    static Scanner input = new Scanner(System.in);
    static int playerMoney = 100;
    public static void main(String[] args) {
        int bet = 0;
        while (playerMoney > 0) {
            System.out.println("You have $" + playerMoney);
            System.out.println("How much would you like to bet?");
            bet = input.nextInt();
            if (bet > playerMoney) {
                System.out.println("You don't have enough money to make that bet.");
                continue;
            }
            playerMoney -= bet;
            playRound(bet);
        }
        System.out.println("You are out of money.");
    }

    public static void playRound(int bet) {
        List<String> deck = new ArrayList<>();
        for (int i = 2; i <= 10; i++) {
            for (int j = 0; j < 4; j++) {
                deck.add(Integer.toString(i));
            }
        }
        for (int i = 0; i < 4; i++) {
            deck.add("J");
            deck.add("Q");
            deck.add("K");
            deck.add("A");
        }
        int playerScore = 0;
        int dealerScore = 0;
        List<String> playerHand = new ArrayList<>();
        List<String> dealerHand = new ArrayList<>();
        int cardsLeft = deck.size();
        for (int i = 0; i < 2; i++) {
            int randomIndex = (int) Math.floor(Math.random() * cardsLeft);
            String card = deck.get(randomIndex);
            playerHand.add(card);
            playerScore += getCardValue(card);
            deck.remove(randomIndex);
            cardsLeft--;
            randomIndex = (int) Math.floor(Math.random() * cardsLeft);
            card = deck.get(randomIndex);
            dealerHand.add(card);
            dealerScore += getCardValue(card);
            deck.remove(randomIndex);
            cardsLeft--;
        }
        System.out.println("Dealer shows " + dealerHand.get(0));
        System.out.println("Your hand: " + playerHand);
        while (playerScore < 21) {
            System.out.println("Would you like to hit or stand?");
            String response = input.next();
            if (response.equals("hit")) {
                int randomIndex = (int) Math.floor(Math.random() * cardsLeft);
                String card = deck.get(randomIndex);
                playerHand.add(card);
                playerScore += getCardValue(card);
                deck.remove(randomIndex);
                cardsLeft--;
                System.out.println("Your hand: " + playerHand);
            } else {
                break;
            }
        }
        while (dealerScore < 17) {
            int randomIndex = (int) Math.floor(Math.random() * cardsLeft);
            String card = deck.get(randomIndex);
            dealerHand.add(card);
            dealerScore += getCardValue(card);
            deck.remove(randomIndex);
            cardsLeft--;
        }
        System.out.println("Dealer's hand: " + dealerHand);
        if (playerScore > 21) {
            System.out.println("Bust! You lose.");
        } else if (dealerScore > 21) {
            System.out.println("Dealer busts! You win.");
            playerMoney += 2 * bet;
        } else if (playerScore > dealerScore) {
            System.out.println("You win!");
            playerMoney += 2 * bet;
        } else if (dealerScore > playerScore) {
            System.out.println("You lose.");
        } else {
            System.out.println("It's a tie!");
            playerMoney += bet;
        }
    }
    public static int getCardValue(String card) {
        if (card.equals("A")) {
            return 11;
        } else if (card.equals("J") || card.equals("Q") || card.equals("K")) {
            return 10;
        } else {
            return Integer.parseInt(card);
        }
    }
}
