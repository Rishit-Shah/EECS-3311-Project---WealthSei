#!/usr/bin/env bash
# Totals the model calls recorded in llm.log files and prints a markdown table.
# Usage:  bash scripts/ai-usage.sh [folder ...]
# With no arguments it reads every ~/.wealthsei*/llm.log. Only calls, tokens and time are in the log
# (never the key, the prompts or your data). The log holds one token total per call, not input and
# output separately, so the money cost must come from the Anthropic Console.
set -euo pipefail

files=()
if [ "$#" -gt 0 ]; then
  for d in "$@"; do [ -f "$d/llm.log" ] && files+=("$d/llm.log"); done
else
  for f in "$HOME"/.wealthsei*/llm.log; do [ -f "$f" ] && files+=("$f"); done
fi
if [ "${#files[@]}" -eq 0 ]; then
  echo "No llm.log files found."
  exit 0
fi

echo "Logs read: ${#files[@]}"
cat "${files[@]}" | awk '
  {
    m = ""; t = 0; ms = 0
    for (i = 1; i <= NF; i++) {
      if ($i ~ /^model=/)  m  = substr($i, 7)
      if ($i ~ /^tokens=/) t  = substr($i, 8)
      if ($i ~ /^ms=/)     ms = substr($i, 4)
    }
    if (m == "") next
    calls[m]++; tok[m] += t; time[m] += ms
    if (first == "" || $1 < first) first = $1
    if ($1 > last) last = $1
  }
  END {
    print ""
    print "| Model | Calls | Tokens | Average tokens per call | Average ms per call |"
    print "|---|---|---|---|---|"
    for (m in calls) {
      tc += calls[m]; tt += tok[m]
      printf "| %s | %d | %d | %.0f | %.0f |\n", m, calls[m], tok[m], tok[m] / calls[m], time[m] / calls[m]
    }
    printf "| **Total** | **%d** | **%d** | | |\n", tc, tt
    print ""
    print "First call: " first "   Last call: " last "   (times are UTC)"
  }'
