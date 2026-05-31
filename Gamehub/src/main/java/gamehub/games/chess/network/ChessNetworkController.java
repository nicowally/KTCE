package gamehub.games.chess.network;

import gamehub.gamehub.ChessController;
import gamehub.games.chess.Figure;
import javafx.application.Platform;


public class ChessNetworkController extends ChessController implements ChessClient.MessageListener {

        private ChessClient client;
        private String myColor;
        private boolean processingOpponentMove;


        public void initNetwork(ChessClient client) {
            this.client = client;
            this.myColor = client.getMyColor();
            client.setMessageListener(this);

            chessBoard.setFlipped(myColor.equals("b"));
            chessBoard.setDisable(!myColor.equals("w"));
            updateTurnDisplay(true);
        }

        @Override
        public void onMessage(ChessNetworkMessage msg) {
            Platform.runLater(() -> handleMessage(msg));
        }

        private void handleMessage(ChessNetworkMessage msg) {
            switch (msg.type) {

                case ChessNetworkMessage.MOVE -> {
                    int fromCol = msg.getCol(1);
                    int fromRow = msg.getRow(1);
                    int toCol = msg.getCol(2);
                    int toRow = msg.getRow(2);

                    Figure figure = chessBoard.getLogic().getFigureAt(fromCol, fromRow);
                    if (figure != null) {
                        super.showCheckMessage("", false);
                        chessBoard.getLogic().whiteTurn = myColor.equals("b");
                        processingOpponentMove = true;  // <-- Flag setzen
                        chessBoard.moveFigure(figure, toCol, toRow);
                        processingOpponentMove = false; // <-- Flag zurücksetzen
                        chessBoard.getLogic().whiteTurn = myColor.equals("w");
                        updateTurnDisplay(chessBoard.getLogic().whiteTurn);
                    }

                    chessBoard.setDisable(false);
                }

                case ChessNetworkMessage.PROMOTION -> {
                    // Der Gegner hat einen Bauern umgewandelt
                    pendingPromotionChoice = msg.getPart(1);
                }

                case ChessNetworkMessage.CHECK -> {
                    super.showCheckMessage("SCHACH!", true);
                }

                case ChessNetworkMessage.FORFEIT -> {
                    String loserColor = msg.getPart(1);
                    String loser;
                    if (loserColor.equals("w")) {
                        loser = "Weiß";
                    } else loser = "Schwarz";
                    String winner;
                    if (loserColor.equals("w")) {
                        winner = "Schwarz";
                    } else winner = "Weiß";
                    chessBoard.setDisable(true);
                    showForfeitDialog(loser, winner);
                }

                case ChessNetworkMessage.GAME_OVER -> {
                    chessBoard.setDisable(true);
                    String reason = msg.getPart(1);
                    String winner = msg.getPart(2);
                    if (reason.equals("CHECKMATE")) {
                        String winnerName;
                        if (winner.equals("w")) {
                            winnerName = "Weiß";
                        } else winnerName = "Schwarz";
                        showCheckmateDialog(winnerName);
                    } else {
                        showPattDialog();
                    }
                }
                case ChessNetworkMessage.OPPONENT_DISCONNECTED -> {
                    chessBoard.setDisable(true);
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.INFORMATION);
                    alert.setTitle("Verbindung unterbrochen");
                    alert.setHeaderText("Gegner hat die Verbindung getrennt");
                    alert.setContentText("Die Partie wurde abgebrochen.");
                    alert.showAndWait();
                }
            }
        }

        // Wird von ChessBoard.moveFigure() aufgerufen und überschrieben um Nachricht zu senden
        @Override
        public void recordMove(int fromCol, int fromRow, int toCol, int toRow, String pieceName) {
            super.recordMove(fromCol, fromRow, toCol, toRow, pieceName); // Zugliste aktualisieren

            // Im lokalen Modus client == null → nichts senden
            if (client == null) return;

            // whiteTurn ist hier noch der Wert VOR dem Zug
            // → wenn whiteTurn true ist, hat gerade Weiß gezogen
            boolean whiteJustMoved = chessBoard.getLogic().whiteTurn;
            boolean iMyMove = (myColor.equals("w") && whiteJustMoved)
                    || (myColor.equals("b") && !whiteJustMoved);

            if (iMyMove) {
                client.send(ChessNetworkMessage.buildMove(fromCol, fromRow, toCol, toRow));
                chessBoard.setDisable(true);
            }
        }

        @Override
        public void showCheckMessage(String message, boolean visible) {
            super.showCheckMessage(message, visible);
            if (visible && client != null && !processingOpponentMove) {
                client.send(ChessNetworkMessage.buildCheck());
            }
        }

        // Wenn ICH einen Bauern umwandle → Dialog zeigen und Wahl senden
        // Wenn der GEGNER umwandelt → pendingPromotionChoice wurde schon via PROMOTION gesetzt
        String pendingPromotionChoice = null;

        @Override
        public String showPromotionDialog(String colour) {
            if (pendingPromotionChoice != null) {
                // Gegner hat umgewandelt — gespeicherte Wahl verwenden, kein Dialog
                String choice = pendingPromotionChoice;
                pendingPromotionChoice = null;
                return choice;
            }
            // Eigene Umwandlung — Dialog zeigen und Wahl senden
            String choice = super.showPromotionDialog(colour);
            if(client != null) {
                client.send(ChessNetworkMessage.buildPromotion(choice));
            }
            return choice;
        }

        @Override
        protected void forfeit() {
            if (client == null) {
                super.forfeit();
                return;
            }

            String loser;
            if (myColor.equals("w")) {
                loser = "Weiß";
            } else loser = "Schwarz";
            String winner;
            if (myColor.equals("w")) {
                winner = "Schwarz";
            } else winner = "Weiß";

            javafx.scene.control.Alert confirmAlert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Spiel aufgeben");
            confirmAlert.setHeaderText(loser + " gibt auf?");
            confirmAlert.setContentText("Möchtest du die Partie wirklich beenden? " + winner + " hat dann gewonnen!");

            java.util.Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
            if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                client.send(ChessNetworkMessage.buildForfeit(myColor));
                chessBoard.setDisable(true);
                showForfeitDialog(loser, winner);
            }
        }

        @Override
        protected void onBackClick() {
            if (client != null) client.disconnect();
            super.onBackClick();
        }
}
