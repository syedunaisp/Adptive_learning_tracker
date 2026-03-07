package tracker.ui.fx.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class TableColumnFormatter {

    /**
     * Binds a TableColumn's preferred width to a specific percentage of the parent
     * TableView's width.
     * Note: This works best when the TableView uses CONSTRAINED_RESIZE_POLICY.
     *
     * @param column     The TableColumn to format
     * @param table      The parent TableView
     * @param percentage The percentage of width to consume (e.g. 0.3 for 30%)
     */
    public static void bindColumnWidth(TableColumn<?, ?> column, TableView<?> table, double percentage) {
        // We subtract a tiny offset (e.g. 2px) from the total table width to account
        // for borders/padding
        // and prevent the horizontal scrollbar from triggering in constrained mode.
        column.prefWidthProperty().bind(table.widthProperty().subtract(4).multiply(percentage));
    }
}
