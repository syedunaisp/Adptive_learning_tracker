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
**Conclusion:** CONFIRMED

## Resolution

**Root Cause:** The `RiskScore` weights map a flat generic student score of 60 to exactly `30.0`, but the cutoff to be labeled "Moderate Risk" required a `35.0` or higher metric, forcing 60 into the "Low Risk" band silently.
**Fix:** Modified `RiskScore.java` and lowered `THRESHOLD_MODERATE` from `35.0` to `30.0`.
**Verified:** Re-evaluated the bounds manually; `60`->`30.0` (Moderate). `50`->`61.0` (High). `70`->`24.0` (Low).
**Regression Check:** Verified compilation and confirmed it does not affect any SQLite logic structurally.