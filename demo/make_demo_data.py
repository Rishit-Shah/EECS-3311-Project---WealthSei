#!/usr/bin/env python3
"""Writes the invented demo statements in demo/ (same layout as a Neo card export).

Everything here is made up: amounts are random, the only real names are well-known chains,
and no personal data is used. Run: python3 demo/make_demo_data.py
"""
import calendar
import datetime as dt
import pathlib
import random

OUT = pathlib.Path(__file__).parent
rng = random.Random(2026)

# (description stem, city, low, high, weight) - the stem is padded the way a card statement pads it
SHOPS = [
    ("LOBLAWS #0412", "TORONTO", 28, 96, 6),
    ("NO FRILLS #118", "TORONTO", 22, 74, 4),
    ("FARM BOY MARKET", "TORONTO", 15, 52, 2),
    ("TIM HORTONS #3301", "TORONTO", 3, 9, 8),
    ("STARBUCKS 1042", "TORONTO", 5, 13, 4),
    ("MAPLE LEAF BISTRO", "TORONTO", 16, 48, 4),
    ("PIZZA NOVA #207", "TORONTO", 14, 38, 3),
    ("UBER EATS", "TORONTO", 18, 52, 5),
    ("DOORDASH*SUSHI HOUSE", "TORONTO", 22, 61, 3),
    ("PRESTO FARE/AB12CD34", "TORONTO", 20, 40, 4),
    ("UBER *TRIP", "TORONTO", 9, 31, 3),
    ("SHOPPERS DRUG MART 886", "TORONTO", 8, 44, 3),
    ("AMAZON.CA*MK3PL7QX2", "TORONTO", 12, 85, 3),
    ("BEST BUY 944", "TORONTO", 30, 140, 1),
    ("CINEPLEX #6120", "TORONTO", 14, 36, 2),
    ("CAMPUS VENDING MACH-B", "TORONTO", 2, 5, 2),
    ("NORTHSTAR LAUNDROMAT", "TORONTO", 6, 14, 2),
]
# charged every month on a fixed day: (stem, city, day, {month: amount}, default amount)
RECURRING = [
    ("NETFLIX.COM", "LOS GATOS", 3, {}, 16.49),
    ("SPOTIFY", "STOCKHOLM", 7, {10: 12.99, 11: 12.99}, 11.99),
    ("ROGERS ******4417", "TORONTO", 12, {}, 65.00),
    ("ANTHROPIC* CLAUDE SUB", "SAN FRANCISCO", 28, {}, 28.25),
]

MONTHS = [(2026, 8, 31), (2026, 9, 30), (2026, 10, 31), (2026, 11, 4)]


def line(day, post, desc, city, amount, country="CAN"):
    text = f"{desc:<23}{city:<14}{country}".rstrip()
    return f"{day.isoformat()},{post.isoformat()},Posted,{text},{amount:.2f}"


def month_rows(year, month, last_day):
    rows = []
    scale = {8: 0.85, 9: 0.9, 10: 1.0, 11: 1.0}[month]
    days_in = last_day
    count = int(round(58 * scale * days_in / calendar.monthrange(year, month)[1]))
    weights = [s[4] for s in SHOPS]
    for _ in range(count):
        shop = rng.choices(SHOPS, weights)[0]
        day = dt.date(year, month, rng.randint(1, days_in))
        amount = round(rng.uniform(shop[2], shop[3]) * (1.25 if month == 10 and "BISTRO" in shop[0] else 1), 2)
        rows.append((day, shop[0], shop[1], -amount))
    for stem, city, d, by_month, default in RECURRING:
        if d <= days_in:
            rows.append((dt.date(year, month, d), stem, city, -by_month.get(month, default)))
    if days_in >= 20:
        rows.append((dt.date(year, month, 15), "Payment Received - Thank you", "", 40.00))
        rows.append((dt.date(year, month, days_in), "Reward Cashed Out", "", round(rng.uniform(1.5, 4.5), 2)))
    return sorted(rows, key=lambda r: (r[0], r[1]), reverse=True)


for year, month, last in MONTHS:
    rows = month_rows(year, month, last)
    first = dt.date(year, month, 1)
    end = dt.date(year, month, last) if month != 11 else dt.date(2026, 11, 30)
    name = OUT / f"NeoDemoMastercard__{first.isoformat()}_{end.isoformat()}.csv"
    body = ["Transaction Date,Posted Date,Status,Description,Amount"]
    for day, stem, city, amount in rows:
        desc_city = city
        country = "USA" if city in ("LOS GATOS", "SAN FRANCISCO") else ("SWE" if city == "STOCKHOLM" else "CAN")
        if stem in ("Payment Received - Thank you", "Reward Cashed Out"):
            body.append(f"{day.isoformat()},{(day + dt.timedelta(days=1)).isoformat()},Posted,{stem},{amount:.2f}")
        else:
            body.append(line(day, day + dt.timedelta(days=1), stem, desc_city, amount, country))
    name.write_text("\n".join(body) + "\n", encoding="utf-8")
    print("wrote", name.name, len(rows), "rows")
