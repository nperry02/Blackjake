import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class blackjack {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final int SHOW_DELAY_MS = 300;
    private static final int RESHUFFLE_DELAY_MS = 1000;
    private static final int RESHUFFLE_THRESHOLD = 17;
    private static class Card {
        final String rank;
        final String suit;

        Card(String rank, String suit) {
            this.rank = rank;
            this.suit = suit;
        }

        int value() {
            if ("A".equals(rank)) return 11;
            if ("K".equals(rank) || "Q".equals(rank) || "J".equals(rank)) return 10;
            return Integer.parseInt(rank);
        }
        // no One
        @Override
        public String toString() {
            return rank + suit;
        }
    }

    private static class Deck {
        private final List<Card> cards = new ArrayList<>();

        Deck() {
            reset();
        }

        void reset() {
            cards.clear();
            String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
            String[] suits = {"S", "H", "D", "C"};
            for (String suit : suits) {
                for (String rank : ranks) {
                    cards.add(new Card(rank, suit));
                }
            }
            Collections.shuffle(cards);
        }

        Card draw() {
            return cards.remove(cards.size() - 1);
        }

        int size() {
            return cards.size();
        }
    }

    private static int handValue(List<Card> hand) {
        int total = 0;
        int aces = 0;
        for (Card c : hand) {
            total += c.value();
            if ("A".equals(c.rank)) aces++;
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    private static String handString(List<Card> hand) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(hand.get(i));
        }
        return sb.toString();
    }

    private static String[] cardArtLines(Card card) {
        String rank = card.rank;
        String left = rank.length() == 1 ? rank + " " : rank;
        String right = rank.length() == 1 ? " " + rank : rank;
        String suit = card.suit;
        String color;
        if ("H".equals(suit) || "D".equals(suit)) {
            color = ANSI_RED;
        } else {
            color = ANSI_BLUE;
        }
        String reset = color.isEmpty() ? "" : ANSI_RESET;
        return new String[] {
            "+-----+",
            "|" + color + left + reset + "   |",
            "|  " + color + suit + reset + "  |",
            "|   " + color + right + reset + "|",
            "+-----+"
        };
    }

    private static String[] hiddenCardArtLines() {
        return new String[] {
            "+-----+",
            "|?    |",
            "|  ?  |",
            "|    ?|",
            "+-----+"
        };
    }

    private static String[] handArtLines(List<Card> hand) {
        if (hand.isEmpty()) return new String[0];
        String[][] cards = new String[hand.size()][];
        for (int i = 0; i < hand.size(); i++) {
            cards[i] = cardArtLines(hand.get(i));
        }
        String[] lines = new String[5];
        for (int line = 0; line < 5; line++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cards.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(cards[i][line]);
            }
            lines[line] = sb.toString();
        }
        return lines;
    }

    private static void pauseShow() {
        try {
            Thread.sleep(SHOW_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String[] handArtLinesDealer(List<Card> dealer, boolean revealDealer) {
        if (dealer.isEmpty()) return new String[0];
        String[][] cards = new String[dealer.size()][];
        for (int i = 0; i < dealer.size(); i++) {
            if (!revealDealer && i > 0) {
                cards[i] = hiddenCardArtLines();
            } else {
                cards[i] = cardArtLines(dealer.get(i));
            }
        }
        String[] lines = new String[5];
        for (int line = 0; line < 5; line++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cards.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(cards[i][line]);
            }
            lines[line] = sb.toString();
        }
        return lines;
    }

    private static void showHands(List<Card> player, List<Card> dealer, boolean revealDealer, int balance, int bet) {
        showHands(player, dealer, revealDealer, balance, bet, bet, "You");
    }

    private static void showHands(List<Card> player, List<Card> dealer, boolean revealDealer,
                                  int balance, int reservedBet, int bet, String playerName) {
        pauseShow();
        String[] dealerLines = handArtLinesDealer(dealer, revealDealer);
        String[] playerLines = handArtLines(player);
        String gap = "    ";
        String dealerLabel = revealDealer ? "Dealer (" + handValue(dealer) + "):" : "Dealer:";
        int displayBalance = balance - reservedBet;
        String playerLabel = playerName + " (" + handValue(player) + ") $" + displayBalance + " bet:$" + bet + ":";
        int dealerWidth = dealerLines.length > 0 ? dealerLines[0].length() : 0;
        int leftWidth = Math.max(dealerWidth, dealerLabel.length());
        System.out.println(padRight(dealerLabel, leftWidth) + gap + playerLabel);
        int lines = Math.max(dealerLines.length, playerLines.length);
        for (int i = 0; i < lines; i++) {
            String left = i < dealerLines.length ? dealerLines[i] : "";
            String right = i < playerLines.length ? playerLines[i] : "";
            System.out.println(padRight(left, leftWidth) + gap + right);
        }
    }

    private static String padRight(String text, int width) {
        int padding = width - text.length();
        if (padding <= 0) return text;
        StringBuilder sb = new StringBuilder(width);
        sb.append(text);
        for (int i = 0; i < padding; i++) sb.append(' ');
        return sb.toString();
    }

    private static void pauseReshuffle() {
        try {
            Thread.sleep(RESHUFFLE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void reshuffleIfLow(Deck deck) {
        if (deck.size() < RESHUFFLE_THRESHOLD) {
            System.out.println("Reshuffling...");
            pauseReshuffle();
            deck.reset();
        }
    }

    private static Card drawCard(Deck deck) {
        return deck.draw();
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Deck deck = new Deck();
        int balance = 25;

        while (true) {
            if (balance <= 0) {
                System.out.println("You're out of money. Game over.");
                break;
            }

            int bet = 0;
            while (true) {
                System.out.print("Balance: $" + balance + ". Enter your bet: ");
                String betInput = scanner.nextLine().trim();
                try {
                    bet = Integer.parseInt(betInput);
                } catch (NumberFormatException e) {
                    bet = 0;
                }
                if (bet > 0 && bet <= balance) break;
                System.out.println("Please enter a bet between 1 and " + balance + ".");
            }

            reshuffleIfLow(deck);
            List<Card> dealer = new ArrayList<>();
            List<Card> player = new ArrayList<>();

            player.add(drawCard(deck));
            dealer.add(drawCard(deck));
            player.add(drawCard(deck));
            dealer.add(drawCard(deck));

            boolean canSplit = player.size() == 2 && player.get(0).rank.equals(player.get(1).rank);
            boolean didSplit = false;
            int bet1 = bet;
            int bet2 = 0;
            int reservedBet = bet;

            showHands(player, dealer, false, balance, reservedBet, bet1, "You");

            if (canSplit && balance >= bet * 2) {
                System.out.print("Split? (y/n): ");
                String splitChoice = scanner.nextLine().trim().toLowerCase();
                if ("y".equals(splitChoice)) {
                    didSplit = true;
                    bet2 = bet;
                    reservedBet = bet1 + bet2;
                    System.out.println("Split! Additional bet of $" + bet2 + " placed.");
                }
            } else if (canSplit) {
                System.out.println("Not enough balance to split.");
            }

            List<Card> hand1 = new ArrayList<>();
            List<Card> hand2 = new ArrayList<>();
            if (didSplit) {
                hand1.add(player.get(0));
                hand2.add(player.get(1));
                hand1.add(drawCard(deck));
                hand2.add(drawCard(deck));
            } else {
                hand1.addAll(player);
            }

            showHands(hand1, dealer, false, balance, reservedBet, bet1, didSplit ? "You (Hand 1)" : "You");

            boolean hand1Busted = false;
            boolean hand2Busted = false;
            if (!didSplit && hand1.size() == 2 && handValue(hand1) == 21) {
                // Natural blackjack handled later.
            } else {
                while (true) {
                    int playerValue = handValue(hand1);
                    if (playerValue >= 21) break;
                    boolean canDouble = hand1.size() == 2;
                    if (canDouble) {
                        System.out.print("Hit, stand, or double? (h/s/d): ");
                    } else {
                        System.out.print("Hit or stand? (h/s): ");
                    }
                    String choice = scanner.nextLine().trim().toLowerCase();
                    if ("h".equals(choice)) {
                        hand1.add(drawCard(deck));
                        showHands(hand1, dealer, false, balance, reservedBet, bet1, didSplit ? "You (Hand 1)" : "You");
                    } else if ("d".equals(choice) && canDouble) {
                        int extra = bet1;
                        if (balance - reservedBet < extra) {
                            System.out.println("Not enough balance to double down.");
                            continue;
                        }
                        bet1 += extra;
                        reservedBet += extra;
                        System.out.println("Double down! Bet increased to $" + bet1 + ".");
                        hand1.add(drawCard(deck));
                        showHands(hand1, dealer, false, balance, reservedBet, bet1, didSplit ? "You (Hand 1)" : "You");
                        break;
                    } else if ("s".equals(choice)) {
                        break;
                    } else {
                        System.out.println(canDouble ? "Please enter h, s, or d." : "Please enter h or s.");
                    }
                }
            }

            if (handValue(hand1) > 21) {
                hand1Busted = true;
            }

            if (didSplit) {
                showHands(hand2, dealer, false, balance, reservedBet, bet2, "You (Hand 2)");
                while (true) {
                    int playerValue = handValue(hand2);
                    if (playerValue >= 21) break;
                    boolean canDouble = hand2.size() == 2;
                    if (canDouble) {
                        System.out.print("Hit, stand, or double? (h/s/d): ");
                    } else {
                        System.out.print("Hit or stand? (h/s): ");
                    }
                    String choice = scanner.nextLine().trim().toLowerCase();
                    if ("h".equals(choice)) {
                        hand2.add(drawCard(deck));
                        showHands(hand2, dealer, false, balance, reservedBet, bet2, "You (Hand 2)");
                    } else if ("d".equals(choice) && canDouble) {
                        int extra = bet2;
                        if (balance - reservedBet < extra) {
                            System.out.println("Not enough balance to double down.");
                            continue;
                        }
                        bet2 += extra;
                        reservedBet += extra;
                        System.out.println("Double down! Bet increased to $" + bet2 + ".");
                        hand2.add(drawCard(deck));
                        showHands(hand2, dealer, false, balance, reservedBet, bet2, "You (Hand 2)");
                        break;
                    } else if ("s".equals(choice)) {
                        break;
                    } else {
                        System.out.println(canDouble ? "Please enter h, s, or d." : "Please enter h or s.");
                    }
                }
                if (handValue(hand2) > 21) {
                    hand2Busted = true;
                }
            }

            boolean playerBlackjack = !didSplit && hand1.size() == 2 && handValue(hand1) == 21;
            boolean dealerBlackjack = dealer.size() == 2 && handValue(dealer) == 21;

            if ((!hand1Busted || (!hand2Busted && didSplit)) && !dealerBlackjack) {
                showHands(hand1, dealer, true, balance, reservedBet, bet1, didSplit ? "You (Hand 1)" : "You");
                if (didSplit) {
                    showHands(hand2, dealer, true, balance, reservedBet, bet2, "You (Hand 2)");
                }
                while (handValue(dealer) < 17) {
                    dealer.add(drawCard(deck));
                    showHands(hand1, dealer, true, balance, reservedBet, bet1, didSplit ? "You (Hand 1)" : "You");
                    if (didSplit) {
                        showHands(hand2, dealer, true, balance, reservedBet, bet2, "You (Hand 2)");
                    }
                }
            }

            int dealerValue = handValue(dealer);
            int net = 0;

            System.out.println("Final hands:");
            showHands(hand1, dealer, true, balance, reservedBet, bet1, didSplit ? "You (Hand 1)" : "You");
            if (didSplit) {
                showHands(hand2, dealer, true, balance, reservedBet, bet2, "You (Hand 2)");
            }

            if (playerBlackjack && dealerBlackjack) {
                System.out.println("Both have blackjack. Push.");
            } else if (playerBlackjack) {
                int blackjackWin = (bet1 * 3) / 2;
                net = blackjackWin;
                balance += blackjackWin;
                System.out.println("Blackjack! You win $" + blackjackWin + ".");
            } else if (dealerBlackjack) {
                net = -(bet1 + bet2);
                balance -= (bet1 + bet2);
                System.out.println("Dealer has blackjack. Dealer wins.");
            } else {
                int hand1Value = handValue(hand1);
                int hand2Value = handValue(hand2);
                if (hand1Busted) {
                    net -= bet1;
                } else if (dealerValue > 21 || hand1Value > dealerValue) {
                    net += bet1;
                } else if (hand1Value < dealerValue) {
                    net -= bet1;
                }
                if (didSplit) {
                    if (hand2Busted) {
                        net -= bet2;
                    } else if (dealerValue > 21 || hand2Value > dealerValue) {
                        net += bet2;
                    } else if (hand2Value < dealerValue) {
                        net -= bet2;
                    }
                }
                if (net > 0) {
                    balance += net;
                    System.out.println("You win!");
                } else if (net < 0) {
                    balance += net;
                    System.out.println("Dealer wins.");
                } else {
                    System.out.println("Push.");
                }
            }

            if (net > 0) {
                System.out.println("You won $" + net + " this hand.");
            } else if (net < 0) {
                System.out.println("You lost $" + (-net) + " this hand.");
            } else {
                System.out.println("You won $0 this hand.");
            }

            String again;
            while (true) {
                System.out.print("Play again? (y/n): ");
                again = scanner.nextLine().trim().toLowerCase();
                if ("y".equals(again) || "n".equals(again)) break;
                System.out.println("Please enter y or n.");
            }
            if ("n".equals(again)) {
                break;
            }
            System.out.println("----------------------------------------");
        }
    }
}
