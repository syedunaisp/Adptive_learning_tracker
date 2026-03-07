package tracker.ui.fx.util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Callback;

public class AvatarCellFactory<S> implements Callback<TableColumn<S, String>, TableCell<S, String>> {
    @Override
    public TableCell<S, String> call(TableColumn<S, String> param) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.trim().isEmpty()) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox container = new HBox(12);
                    container.setAlignment(Pos.CENTER_LEFT);

                    // Extract initials
                    String[] parts = item.trim().split("\\s+");
                    String initials = "";
                    if (parts.length > 0)
                        initials += parts[0].charAt(0);
                    if (parts.length > 1)
                        initials += parts[parts.length - 1].charAt(0);
                    initials = initials.toUpperCase();

                    // Avatar Circle
                    StackPane avatar = new StackPane();
                    Circle circle = new Circle(16);
                    circle.setStyle("-fx-fill: -alip-bg-input; -fx-stroke: -alip-border; -fx-stroke-width: 1;");

                    Label initLabel = new Label(initials);
                    initLabel.setStyle("-fx-text-fill: -alip-accent; -fx-font-size: 11px; -fx-font-weight: bold;");

                    avatar.getChildren().addAll(circle, initLabel);

                    // Name Label
                    Label nameLabel = new Label(item);
                    nameLabel
                            .setStyle("-fx-text-fill: -alip-text-primary; -fx-font-size: 13px; -fx-font-weight: bold;");

                    container.getChildren().addAll(avatar, nameLabel);
                    setGraphic(container);
                    setText(null);
                }
            }
        };
    }
}
