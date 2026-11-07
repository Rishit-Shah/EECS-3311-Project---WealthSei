#!/usr/bin/env bash
# Loads the invented demo data into its own data folder (~/.wealthsei-demo), so your real data is never touched.
# Then start the app on it:   WEALTHSEI_HOME=~/.wealthsei-demo mvn -q exec:java -Dexec.mainClass=wealthsei.Main -Dexec.args="gui"
set -euo pipefail
cd "$(dirname "$0")/.."
DEMO_HOME="${WEALTHSEI_HOME:-$HOME/.wealthsei-demo}"
case "$DEMO_HOME" in
  *.wealthsei-demo*) ;;
  *) echo "Refusing to use '$DEMO_HOME': the demo folder name must contain .wealthsei-demo (so real data is never deleted)." >&2; exit 1 ;;
esac
rm -rf "$DEMO_HOME"
export WEALTHSEI_HOME="$DEMO_HOME"
mvn -q compile
run() { java -cp target/classes wealthsei.Main "$@"; }

for f in demo/NeoDemoMastercard__*.csv; do run import "$f"; done
run plan balance 2860.40
run plan buffer 200
run plan income Paycheque 1685 biweekly 2026-11-13
run plan bill Rent 1250 monthly 2026-12-01 rent
for m in 2026-10 2026-11; do
  run budget set "$m" dining=450 groceries=400 shopping=150 transport=150 subscriptions=70
done
id_of() { sed -n 's/.*Goal added (\([0-9a-f]*\)).*/\1/p'; }
laptop=$(run goal add "Laptop" 1400 2026-12-20 350 | id_of)
fund=$(run goal add "Emergency fund" 3000 2027-06-01 900 | id_of)
run goal add "Trip to Vancouver" 1800 2027-03-15 400 > /dev/null
run goal monthly "$laptop" 420
run goal monthly "$fund" 300
echo
echo "Demo data is ready in $DEMO_HOME"
echo "Open the GUI on it:  WEALTHSEI_HOME=$DEMO_HOME mvn -q exec:java -Dexec.mainClass=wealthsei.Main -Dexec.args=\"gui\""
