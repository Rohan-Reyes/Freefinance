#!/usr/bin/env python3
"""Generate fictional demo data for the "demo" build type (README screenshots, store listings).

Output: app/src/demo/assets/demo_backup.json (DataSyncManager backup format v2).
All names, amounts and card numbers are made up; card numbers are the standard public test numbers.
Dates are relative to the moment the script runs, so charts look current.
Usage: python tools/make_demo_data.py
"""
import json, os, random
from datetime import datetime, timedelta

random.seed(7)
now = datetime.now().replace(second=0, microsecond=0)
ms = lambda d: int(d.timestamp() * 1000)
month_start = now.replace(day=1, hour=0, minute=0)

def argb(hex_rgb):
    v = 0xFF000000 | int(hex_rgb.lstrip('#'), 16)
    return v - (1 << 32)

categories = [
    (1, 'Food & Dining', 'ic_food', 'Expense', '#F97316'), (2, 'Transport', 'ic_transport', 'Expense', '#3B82F6'),
    (3, 'Shopping', 'ic_shopping', 'Expense', '#EC4899'), (4, 'Bills & Utilities', 'ic_utilities', 'Expense', '#EAB308'),
    (5, 'Entertainment', 'ic_entertainment', 'Expense', '#A855F7'), (6, 'Health', 'ic_health', 'Expense', '#22C55E'),
    (7, 'Salary', 'ic_salary', 'Income', '#10B981'), (8, 'Freelance', 'ic_freelance', 'Income', '#06B6D4'),
    (9, 'Savings', 'ic_savings', 'Income', '#14B8A6'),
]
accounts = [  # id, name, type, initial, color, parent, subtype, rate, locked
    (1, 'Cash', 'Cash', 7500, '#64748B', None, None, 0, False),
    (2, 'GCash', 'E-Wallet', 9800, '#0057E7', None, None, 0, False),
    (3, 'Maya', 'E-Wallet', 1800, '#00B464', None, None, 0, False),
    (4, 'MariBank Savings', 'Savings', 38000, '#F26A0A', None, None, 3.25, False),
    (5, 'Time Deposit', 'Savings', 20000, '#F59E0B', 4, 'Time Deposit', 4.5, True),
    (6, 'BDO Savings', 'Bank Account', 26000, '#1046B8', None, None, 0.25, False),
    (7, 'ShopeePay', 'E-Wallet', 650, '#EE4D2D', None, None, 0, False),
]
bal = {a[0]: float(a[3]) for a in accounts}
tx, tid = [], 0

def add(day, typ, amount, acc, cat=None, note=None, to=None, labels=(), status='Cleared', rule='None'):
    global tid
    tid += 1
    tx.append({'id': tid, 'accountId': acc, 'toAccountId': to, 'type': typ, 'amount': round(amount, 2),
               'categoryId': cat, 'date': ms(day), 'status': status, 'recurrenceRule': rule,
               'labels': list(labels), 'note': note})
    if status == 'Cleared':
        if typ == 'Income': bal[acc] += amount
        elif typ == 'Expense': bal[acc] -= amount
        else: bal[acc] -= amount; bal[to] += amount

# five months of history up to today
start = (month_start - timedelta(days=125)).replace(day=1)
d = start
food = ['Jollibee', 'Groceries – SM Hypermarket', 'Coffee', 'Lunch with team', 'Mang Inasal', '7-Eleven', 'Bakery']
while d <= now:
    if d.day in (15, 30) or (d.day == 28 and d.month == 2):
        add(d.replace(hour=9), 'Income', 22500, 6, 7, 'Salary')
        add(d.replace(hour=10), 'Transfer', 6500, 6, None, 'Top up GCash', to=2)
        add(d.replace(hour=10, minute=2), 'Transfer', 5000, 6, None, 'ATM withdrawal', to=1) if d.day == 15 else None
        add(d.replace(hour=10, minute=5), 'Transfer', 3000, 6, None, 'Savings', to=4)
    if random.random() < 0.85:
        add(d.replace(hour=12, minute=random.randint(0, 59)), 'Expense', random.choice([95, 150, 189, 245, 320, 560, 1280]),
            random.choice([1, 1, 2, 2, 3]), 1, random.choice(food))
    if random.random() < 0.5:
        add(d.replace(hour=8, minute=random.randint(0, 59)), 'Expense', random.choice([60, 85, 120, 240]), random.choice([1, 2]), 2,
            random.choice(['Grab', 'Jeepney + MRT', 'Angkas', 'Fuel']))
    if random.random() < 0.12:
        add(d.replace(hour=20), 'Expense', random.choice([399, 799, 1499, 2290]), random.choice([2, 2, 7]), 3,
            random.choice(['Shopee order', 'Uniqlo', 'Lazada order']))
    if d.day == 5:
        add(d.replace(hour=19), 'Expense', 1699, 6, 4, 'PLDT Fibr')
        add(d.replace(hour=19, minute=10), 'Expense', 2350, 2, 4, 'Meralco')
    if d.day == 2:
        add(d.replace(hour=11), 'Transfer', 2500, 6, None, 'Top up Maya', to=3)
        add(d.replace(hour=11, minute=5), 'Transfer', 2000, 2, None, 'Top up ShopeePay', to=7)
    if d.day == 12:
        add(d.replace(hour=21), 'Expense', 549, 3, 5, 'Netflix')
    if random.random() < 0.04:
        add(d.replace(hour=16), 'Expense', random.choice([350, 780, 1200]), 1, 6, random.choice(['Mercury Drug', 'Check-up']))
    if d.day == 20 and d.month % 2 == 0:
        add(d.replace(hour=15), 'Income', 12500, 4, 8, 'Website project payment', labels=['#freelance'])
    d += timedelta(days=1)

# recurring templates (next due dates)
def nxt(day):
    t = now.replace(day=min(day, 28), hour=19, minute=0)
    return t if t > now else (t.replace(day=1) + timedelta(days=32)).replace(day=min(day, 28))
for name, amt, acc, cat, day in [('Netflix', 549, 3, 5, 12), ('PLDT Fibr', 1699, 6, 4, 5), ('Meralco', 2350, 2, 4, 5), ('Spotify', 149, 2, 5, 18)]:
    add(nxt(day), 'Expense', amt, acc, cat, name, status='Planned', rule='Monthly')
add(nxt(15).replace(hour=9), 'Income', 22500, 6, 7, 'Salary', status='Planned', rule='Monthly')

acc_json = []
for (i, n, t, init, c, parent, sub, rate, locked) in accounts:
    acc_json.append({k: v for k, v in {'id': i, 'name': n, 'type': t, 'initialBalance': init, 'currentBalance': round(bal[i], 2), 'color': argb(c),
                     'interestRatePa': rate, 'compoundingFrequency': 'MONTHLY' if rate else 'NONE', 'currencyCode': 'PHP',
                     'isLocked': locked, 'includeInTally': True, 'parentAccountId': parent, 'subAccountType': sub}.items() if v is not None})

data = {
    'version': 2, 'exportedAt': ms(now), 'baseCurrency': 'PHP',
    'settings': {'base_currency': 'PHP', 'app_theme_preset': 'OCEAN_INDIGO', 'app_theme_mode': 'DARK', 'hero_background_style': 'GRADIENT',
                 'include_assets_in_hero': False, 'hide_money': False},
    'categories': [{'id': i, 'name': n, 'iconResId': ic, 'type': t, 'color': argb(c)} for i, n, ic, t, c in categories],
    'accounts': acc_json,
    'transactions': [{k: v for k, v in t.items() if v is not None} for t in tx],
    'budgets': [{'id': i, 'categoryId': c, 'amountLimit': lim, 'period': 'Monthly', 'startDate': ms(month_start),
                 'endDate': ms((month_start + timedelta(days=32)).replace(day=1) - timedelta(minutes=1))}
                for i, (c, lim) in enumerate([(1, 9000), (2, 2500), (3, 3000), (5, 1500), (4, 4500)], 1)],
    'debts': [
        {'id': 1, 'personName': 'Miguel Santos', 'type': 'Lent', 'totalAmount': 1500, 'remainingAmount': 1500, 'dueDate': ms(now + timedelta(days=4)), 'status': 'Active', 'notes': 'Concert tickets', 'createdAt': ms(now - timedelta(days=10))},
        {'id': 2, 'personName': 'Ana Reyes', 'type': 'Lent', 'totalAmount': 3000, 'remainingAmount': 1200, 'dueDate': ms(now + timedelta(days=12)), 'status': 'Active', 'notes': 'Laptop repair', 'createdAt': ms(now - timedelta(days=30))},
        {'id': 3, 'personName': 'Carlo Mendoza', 'type': 'Lent', 'totalAmount': 800, 'remainingAmount': 800, 'dueDate': ms(now - timedelta(days=2)), 'status': 'Active', 'createdAt': ms(now - timedelta(days=20))},
        {'id': 4, 'personName': 'Tita Rose', 'type': 'Borrowed', 'totalAmount': 5000, 'remainingAmount': 2500, 'dueDate': ms(now + timedelta(days=25)), 'status': 'Active', 'createdAt': ms(now - timedelta(days=40))},
    ],
    'freelanceProjects': [{'id': 1, 'projectName': 'Website redesign', 'client': 'Studio Lumen', 'totalExpectedFee': 25000, 'status': 'Active', 'createdAt': ms(now - timedelta(days=45))}],
    'milestones': [
        {'id': 1, 'projectId': 1, 'title': 'Design approval', 'amount': 12500, 'expectedDate': ms(now - timedelta(days=20)), 'status': 'Paid', 'paidAt': ms(now - timedelta(days=18))},
        {'id': 2, 'projectId': 1, 'title': 'Launch', 'amount': 12500, 'expectedDate': ms(now + timedelta(days=14)), 'status': 'Pending'},
    ],
    'exchangeRates': [{'id': 1, 'fromCurrency': 'USD', 'toCurrency': 'PHP', 'rate': 58.5, 'lastUpdated': ms(now)}],
    'paymentCards': [
        {'id': 1, 'cardName': 'GCash Card', 'cardType': 'DEBIT', 'cardNetwork': 'MASTERCARD', 'issuingBank': 'GCash', 'cardholderName': 'JUAN DELA CRUZ', 'cardNumber': '5555555555554444', 'expiryMonth': 8, 'expiryYear': 30, 'cvv': '123', 'currencyCode': 'PHP', 'color': argb('#0057E7'), 'cardFinish': 'ROYAL_SAPPHIRE', 'isVirtual': False, 'isFrozen': False, 'linkedAccountId': 2, 'dailySpendLimit': 0, 'creditLimit': 0, 'currentCreditUsed': 0, 'statementBalance': 0, 'minimumPaymentDue': 0, 'aprInterestRate': 0, 'annualFee': 0, 'createdAt': ms(now - timedelta(days=90))},
        {'id': 2, 'cardName': 'MariBank Debit', 'cardType': 'DEBIT', 'cardNetwork': 'VISA', 'issuingBank': 'MariBank', 'cardholderName': 'JUAN DELA CRUZ', 'cardNumber': '4111111111111111', 'expiryMonth': 3, 'expiryYear': 31, 'cvv': '456', 'currencyCode': 'PHP', 'color': argb('#F26A0A'), 'cardFinish': 'BRONZE_LUXE', 'isVirtual': False, 'isFrozen': False, 'linkedAccountId': 4, 'dailySpendLimit': 0, 'creditLimit': 0, 'currentCreditUsed': 0, 'statementBalance': 0, 'minimumPaymentDue': 0, 'aprInterestRate': 0, 'annualFee': 0, 'createdAt': ms(now - timedelta(days=80))},
        {'id': 3, 'cardName': 'BDO Platinum', 'cardType': 'CREDIT', 'cardNetwork': 'VISA', 'issuingBank': 'BDO', 'cardholderName': 'JUAN DELA CRUZ', 'cardNumber': '4012888888881881', 'expiryMonth': 11, 'expiryYear': 29, 'cvv': '789', 'currencyCode': 'PHP', 'color': argb('#1046B8'), 'cardFinish': 'OBSIDIAN_GOLD', 'isVirtual': False, 'isFrozen': False, 'dailySpendLimit': 0, 'creditLimit': 60000, 'currentCreditUsed': 12450, 'statementBalance': 9800, 'minimumPaymentDue': 850, 'statementDay': 3, 'paymentDueDay': 23, 'aprInterestRate': 3.0, 'annualFee': 3000, 'createdAt': ms(now - timedelta(days=70))},
    ],
    'assets': [
        {'id': 1, 'name': 'Honda Click 125', 'category': 'Vehicle', 'estimatedValue': 78000, 'purchasePrice': 92000, 'purchaseDate': ms(now - timedelta(days=500)), 'currencyCode': 'PHP', 'iconName': 'ic_asset', 'color': argb('#EF4444'), 'includeInNetWorth': True, 'createdAt': ms(now - timedelta(days=100))},
        {'id': 2, 'name': 'MacBook Air M2', 'category': 'Electronics', 'estimatedValue': 52000, 'purchasePrice': 64990, 'purchaseDate': ms(now - timedelta(days=300)), 'currencyCode': 'PHP', 'iconName': 'ic_asset', 'color': argb('#64748B'), 'includeInNetWorth': True, 'createdAt': ms(now - timedelta(days=100))},
    ],
    'plannedPayouts': [
        {'id': 1, 'title': 'Motorcycle insurance', 'amount': 3800, 'currencyCode': 'PHP', 'dueDate': ms(now + timedelta(days=18)), 'targetAccountId': 6, 'categoryId': 2, 'priority': 'NORMAL', 'status': 'PENDING', 'createdAt': ms(now - timedelta(days=5))},
    ],
    'receiptsInbox': [],
    'images': {},
}
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'app', 'src', 'demo', 'assets', 'demo_backup.json')
os.makedirs(os.path.dirname(out), exist_ok=True)
json.dump(data, open(out, 'w', encoding='utf-8'), indent=1)
print({a[1]: round(bal[a[0]]) for a in accounts})
print(f'{len(tx)} transactions -> {os.path.normpath(out)}; net worth ~ PHP {sum(bal.values()):,.2f}')
