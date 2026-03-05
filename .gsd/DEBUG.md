# Debug Session: Risk Score Color Thresholds

## Symptom
A student with a score of exactly 60 maps to "Low Risk" (Green) instead of "Moderate Risk" (Yellow) on the pie chart.

**When:** During pie chart rendering or risk score calculation.
**Expected:** Score = 60 evaluates to Moderate Risk.
**Actual:** Score = 60 evaluates to Low Risk.

## Hypotheses

| # | Hypothesis | Likelihood | Status |
|---|------------|------------|--------|
| 1 | The numerical RiskScore evaluation of 60 falls below the `THRESHOLD_MODERATE` boundary line. | 95% | UNTESTED |
| 2 | CSS styling sets the Moderate color improperly or overrides it. | 5% | UNTESTED |

## Attempts

### Attempt 1
**Testing:** H1 — The numerical RiskScore evaluation of 60 falls below the `THRESHOLD_MODERATE` boundary line.
**Action:** Evaluated the `RiskPredictor.assessRisk()` heuristic algorithm natively. A flat score of 60 results in a total risk numeric integer of 30.0. The `THRESHOLD_MODERATE` is statically bound to 35.0, yielding a LOW risk. Lowering `THRESHOLD_MODERATE` to 30.0 in `tracker.model.RiskScore` should mathematically force exactly 60 into the Moderate tier, 70 into the Low tier, and 50 into the High risk tier.
**Result:** Analysis confirmed the mathematical mapping defect. By altering the constant `THRESHOLD_MODERATE` to `30.0`, score heuristics correctly shift tiers matching user expectations.
**Conclusion:** CONFIRMED (but visually insufficient, see Attempt 2)

### Attempt 2
**Testing:** H2 — CSS styling sets the Moderate color improperly or overrides it.
**Action:** Investigated JavaFX `PieChart` default rendering behavior. When a data node is added with a value of `0` (e.g. `High Risk = 0`), JavaFX does not render an arc, which forces the `PieChart` to assign the first available color (`.default-color0` Orange/Red) to the very first *visible* slice. Thus, "Moderate Risk" inherited Red, and "Low Risk" inherited Green depending on mathematical conditions. Wrote explicit node property listeners in `TeacherDashboardController.java` to hardcode the hex styles (`#ffa502`, `#ff4757`, `#2ed573`) onto the arcs purely by their string labels.
**Result:** Verified that `PieChart` correctly overrides internal indices and styles slices based solely on their labels.
**Conclusion:** CONFIRMED

## Resolution

**Root Cause 1:** The `RiskScore` weights map a flat generic student score of 60 to exactly `30.0` (with a Stable trend), but the cutoff to be labeled "Moderate Risk" required a `35.0` or higher metric, forcing 60 into the "Low Risk" band silently.
**Root Cause 2:** JavaFX `PieChart` dynamically assigns its `.default-color` indices based on the active DOM nodes created. A value of `0` creates no arc, corrupting the color order and causing "Moderate Risk" to inherit "High Risk" CSS styles when High Risk was absent.
**Fix:** Lowered `THRESHOLD_MODERATE` from `35.0` to `30.0` in `RiskScore.java`. Hardcoded CSS properties natively to the `PieChart.Data.nodeProperty()` listeners in `TeacherDashboardController.java` to guarantee pure colors.
**Verified:** Re-evaluated bounds and bypassed implicit CSS array ordering.
**Regression Check:** Verified compilation and checked that listener hooks only target active nodes.