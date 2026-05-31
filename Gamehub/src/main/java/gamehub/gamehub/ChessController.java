package gamehub.gamehub;

import gamehub.games.chess.Figure;
import gamehub.games.chess.figures.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

import java.io.IOException;
import java.util.*;

public class ChessController {

    @FXML
    private StackPane chessBoardPane;
    @FXML
    private Label statusLabel;
    @FXML
    private Rectangle turnIndicator;
    @FXML
    private Label checkLabel;
    @FXML
    private FlowPane whiteCapturedBox;
    @FXML
    private FlowPane blackCapturedBox;
    @FXML
    public ChessBoard chessBoard;
    @FXML
    private ListView<String> moveHistoryListView;

    private int fullMoveCount = 1;
    private final Map<String, Label> capturedCountLabels = new HashMap<>();

    @FXML
    public void initialize() {
        this.chessBoard = new ChessBoard();
        StackPane.setAlignment(chessBoard, javafx.geometry.Pos.CENTER);
        chessBoardPane.getChildren().add(chessBoard);
        setStartPosition(chessBoard);
        chessBoard.setController(this);
        setupCapturedFiguresDisplay();
    }

    @FXML
    protected void onBackClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    GamehubApplication.class.getResource("main-menu.fxml")
            );
            chessBoardPane.getScene().setRoot(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void forfeit() {
        String loser;
        if (statusLabel.getText().contains("Weiß")) {
            loser = "Weiß";
        } else {
            loser = "Schwarz";
        }
        String winner;
        if (loser.equals("Weiß")) {
            winner = "Schwarz";
        } else {
            winner = "Weiß";
        }
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Spiel aufgeben");
        confirmAlert.setHeaderText(loser + " gibt auf?");
        confirmAlert.setContentText("Möchtest du die Partie wirklich beenden? " + winner + " hat dann gewonnen!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            showForfeitDialog(loser, winner);
        }
    }

    public void updateTurnDisplay(boolean whiteTurn) {
        if (whiteTurn) {
            statusLabel.setText("Weiß ist am Zug");
            turnIndicator.setFill(Color.WHITE);
        } else {
            statusLabel.setText("Schwarz ist am Zug");
            turnIndicator.setFill(Color.BLACK);
        }
    }

    protected void setStartPosition(ChessBoard chessBoard) {
        chessBoard.placeFigure(new King("w", 4, 7));chessBoard.placeFigure(new King("b", 4, 0));
        chessBoard.placeFigure(new Queen("w", 3, 7));chessBoard.placeFigure(new Queen("b", 3, 0));
        chessBoard.placeFigure(new Bishop("w", 2, 7));chessBoard.placeFigure(new Bishop("w", 5, 7));
        chessBoard.placeFigure(new Bishop("b", 2, 0));chessBoard.placeFigure(new Bishop("b", 5, 0));
        chessBoard.placeFigure(new Knight("w", 1, 7));chessBoard.placeFigure(new Knight("w", 6, 7));
        chessBoard.placeFigure(new Knight("b", 1, 0));chessBoard.placeFigure(new Knight("b", 6, 0));
        chessBoard.placeFigure(new Rook("w", 0, 7));chessBoard.placeFigure(new Rook("w", 7, 7));
        chessBoard.placeFigure(new Rook("b", 0, 0));chessBoard.placeFigure(new Rook("b", 7, 0));
        chessBoard.placeFigure(new Pawn("w", 0, 6));chessBoard.placeFigure(new Pawn("w", 1, 6));
        chessBoard.placeFigure(new Pawn("w", 2, 6));chessBoard.placeFigure(new Pawn("w", 3, 6));
        chessBoard.placeFigure(new Pawn("w", 4, 6));chessBoard.placeFigure(new Pawn("w", 5, 6));
        chessBoard.placeFigure(new Pawn("w", 6, 6));chessBoard.placeFigure(new Pawn("w", 7, 6));
        chessBoard.placeFigure(new Pawn("b", 0, 1));chessBoard.placeFigure(new Pawn("b", 1, 1));
        chessBoard.placeFigure(new Pawn("b", 2, 1));chessBoard.placeFigure(new Pawn("b", 3, 1));
        chessBoard.placeFigure(new Pawn("b", 4, 1));chessBoard.placeFigure(new Pawn("b", 5, 1));
        chessBoard.placeFigure(new Pawn("b", 6, 1));chessBoard.placeFigure(new Pawn("b", 7, 1));
    }
    protected String showPromotionDialog(String colour) {
        List<String> choices = Arrays.asList("Dame", "Turm", "Springer", "Läufer");
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Dame", choices);
        dialog.setTitle("Bauern-Umwandlung");
        dialog.setHeaderText("Dein Bauer hat das Ende erreicht!");
        dialog.setContentText("Wähle deine neue Figur:");

        // Popup anzeigen und auf Antwort warten
        Optional<String> result = dialog.showAndWait();

        // Falls der User das Fenster schließt - wird automatisch der Bauer in eine Dame umgewandelt
        if (result.isPresent()) {
            return result.get();
        } else {
            return "Dame";
        }
    }

    protected void showCheckMessage(String message, boolean visible) {
        checkLabel.setText(message);
        checkLabel.setVisible(visible);
    }
    protected void showCheckmateDialog(String winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Spiel beendet");
        alert.setHeaderText("SCHACHMATT!");
        alert.setContentText(winner + " hat gewonnen!");

        ButtonType backButton = new ButtonType("Zurück zum GameHub");
        alert.getButtonTypes().setAll(backButton);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> event.consume());

        alert.showAndWait().ifPresent(result -> {
            if (result == backButton) {
                onBackClick();
            }
        });
    }
    protected void showPattDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Spiel beendet");
        alert.setHeaderText("Remis!");
        alert.setContentText("Patt - Unentschieden!");

        ButtonType backButton = new ButtonType("Zurück zum GameHub");
        alert.getButtonTypes().setAll(backButton);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> event.consume());

        alert.showAndWait().ifPresent(result -> {
            if (result == backButton) {
                onBackClick();
            }
        });
    }
    protected void showForfeitDialog(String loser, String winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Spiel beendet");
        alert.setHeaderText("AUFGABE");
        alert.setContentText(loser + " hat das Spiel aufgegeben.\n" + winner + " gewinnt die Partie!");

        ButtonType backButton = new ButtonType("Zurück zum GameHub");
        alert.getButtonTypes().setAll(backButton);
        Window window = alert.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> event.consume());

        alert.showAndWait().ifPresent(result -> {
            if (result == backButton) {
                onBackClick();
            }
        });
    }

    private void setupCapturedFiguresDisplay() {
        String[] figureTypes = {"P", "N", "B", "R", "Q"};
        String[] colors = {"w", "b"};

        for (String color : colors) {
            FlowPane targetPane;
            if (color.equals("w")) {
                targetPane = whiteCapturedBox;
            } else {
                targetPane = blackCapturedBox;
            }
            targetPane.getChildren().clear();

            for (String type : figureTypes) {
                String figureKey = color + type;

                Image image = Figure.cache.get(figureKey);

                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(30);
                imageView.setFitHeight(30);

                Label countLabel = new Label("0");
                capturedCountLabels.put(figureKey, countLabel);

                VBox figureDisplay = new VBox(imageView, countLabel);
                figureDisplay.setSpacing(2);
                figureDisplay.setStyle("-fx-alignment: CENTER; " + "-fx-background-color: rgba(236, 240, 241, 0.8); " +
                        "-fx-background-radius: 5; " + "-fx-padding: 5;");

                targetPane.getChildren().add(figureDisplay);
            }
        }
    }

    public void updateCapturedFiguresUI() {
        // Iterieren über die gespeicherten Labels und aktualisieren die Anzahl
        for (Map.Entry<String, Label> entry : capturedCountLabels.entrySet()) {
            String figureKey = entry.getKey();
            Label countLabel = entry.getValue();

            String color = figureKey.substring(0, 1);
            String type = figureKey.substring(1);

            int count = chessBoard.getLogic().getCapturedFigureCount(color, type);
            countLabel.setText(String.valueOf(count));
        }
    }

    public void recordMove(int fromCol, int fromRow, int toCol, int toRow, String pieceName) {
        String germanName = switch (pieceName) {
            case "Pawn" -> "Bauer";
            case "Rook" -> "Turm";
            case "Knight" -> "Springer";
            case "Bishop" -> "Läufer";
            case "Queen" -> "Dame";
            case "King" -> "König";
            default -> pieceName;
        };

        // Umwandlung der Koordinaten
        String from = convertToChessNotation(fromCol, fromRow);
        String to = convertToChessNotation(toCol, toRow);
        String colorPrefix;
        if (statusLabel.getText().contains("Weiß")) {
            colorPrefix = "W";
        } else {
            colorPrefix = "S";
        }
        String entry = String.format("%d. %s: %s -> %s (%s)", fullMoveCount, colorPrefix, from, to, germanName);
        moveHistoryListView.getItems().add(entry);
        moveHistoryListView.scrollTo(entry);
        if (colorPrefix.equals("S")) {
            fullMoveCount++;
        }
    }
    private String convertToChessNotation(int col, int row) {
        char columnChar = (char) ('a' + col); // 0 = a
        int chessRow = 8 - row; // 0 (oben) -> 8, 7 (unten) -> 1
        return "" + columnChar + chessRow;
    }
}