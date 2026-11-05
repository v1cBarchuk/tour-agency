"""Generates demo_data.sql for the tour agency coursework DB."""
import hashlib
import random
from datetime import date, datetime, timedelta

random.seed(42)
TODAY = date(2026, 11, 2)
SALT = "0123456789abcdef0123456789abcdef"
out = []


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (date, datetime)):
        return "'" + v.isoformat(sep=" ") + "'" if isinstance(v, datetime) else "'" + v.isoformat() + "'"
    return "'" + str(v).replace("\\", "\\\\").replace("'", "''") + "'"


def insert(table, cols, rows):
    out.append(f"INSERT INTO {table} ({', '.join(cols)}) VALUES")
    out.append(",\n".join("    (" + ", ".join(q(v) for v in r) + ")" for r in rows) + ";\n")


def pwd(p):
    return hashlib.sha256((SALT + p).encode()).hexdigest()


def slug(s):
    return "".join(c.lower() if c.isalnum() else "-" for c in s).strip("-").replace("--", "-")


# ---------------- countries / cities ----------------
countries = [
    (1, "Турция", "Turkey", "Тёплое море, отели «всё включено» и богатая история Средиземноморья.",
     "Warm sea, all-inclusive resorts and the rich history of the Mediterranean."),
    (2, "Египет", "Egypt", "Круглогодичное солнце, коралловые рифы Красного моря и пирамиды.",
     "Year-round sunshine, Red Sea coral reefs and the pyramids."),
    (3, "ОАЭ", "UAE", "Небоскрёбы, роскошные курорты и пустынные сафари.",
     "Skyscrapers, luxury resorts and desert safaris."),
    (4, "Таиланд", "Thailand", "Тропические острова, тайская кухня и буддийские храмы.",
     "Tropical islands, Thai cuisine and Buddhist temples."),
    (5, "Мальдивы", "Maldives", "Виллы над водой, белый песок и лучший дайвинг Индийского океана.",
     "Overwater villas, white sand and the best diving in the Indian Ocean."),
    (6, "Грузия", "Georgia", "Горы Кавказа, черноморское побережье, вино и гостеприимство.",
     "Caucasus mountains, the Black Sea coast, wine and hospitality."),
    (7, "Италия", "Italy", "Античный Рим, адриатические пляжи и итальянская кухня.",
     "Ancient Rome, Adriatic beaches and Italian cuisine."),
    (8, "Испания", "Spain", "Барселона Гауди, Коста-Дорада и средиземноморская атмосфера.",
     "Gaudi's Barcelona, Costa Dorada and the Mediterranean vibe."),
]
insert("countries", ["country_id", "name_ru", "name_en", "description_ru", "description_en", "image_path"],
       [(c[0], c[1], c[2], c[3], c[4], f"images/countries/{slug(c[2])}.jpg") for c in countries])

cities = [
    (1, 1, "Анталья", "Antalya"), (2, 1, "Белек", "Belek"), (3, 1, "Кемер", "Kemer"), (4, 1, "Сиде", "Side"),
    (5, 2, "Хургада", "Hurghada"), (6, 2, "Шарм-эль-Шейх", "Sharm El Sheikh"),
    (7, 3, "Дубай", "Dubai"), (8, 3, "Абу-Даби", "Abu Dhabi"),
    (9, 4, "Пхукет", "Phuket"), (10, 4, "Паттайя", "Pattaya"),
    (11, 5, "Северный Мале атолл", "North Male Atoll"),
    (12, 6, "Батуми", "Batumi"), (13, 6, "Гудаури", "Gudauri"), (14, 6, "Тбилиси", "Tbilisi"),
    (15, 7, "Рим", "Rome"), (16, 7, "Римини", "Rimini"),
    (17, 8, "Барселона", "Barcelona"), (18, 8, "Салоу", "Salou"),
]
insert("cities", ["city_id", "country_id", "name_ru", "name_en", "image_path"],
       [(c[0], c[1], c[2], c[3], f"images/cities/{slug(c[3])}.jpg") for c in cities])
city_country = {c[0]: c[1] for c in cities}
city_name = {c[0]: (c[2], c[3]) for c in cities}

# ---------------- hotels ----------------
# id, city, name, stars, default meal, vacation type, amenities, base price per person BYN
hotels = [
    (1, 2, "Rixos Premium Belek", 5, 6, 7, [1, 2, 3, 4, 5, 6, 7, 8, 9], 3600),
    (2, 1, "Titanic Deluxe Lara", 5, 6, 2, [1, 2, 3, 4, 5, 6, 7, 9], 2900),
    (3, 3, "Maxx Royal Kemer Resort", 5, 6, 7, [1, 2, 3, 4, 5, 8, 9], 4300),
    (4, 4, "Barut Hemera", 5, 5, 2, [1, 2, 3, 5, 6, 7], 2300),
    (5, 1, "Delphin Imperial Lara", 5, 5, 1, [1, 2, 3, 4, 6, 7], 2500),
    (6, 5, "Hilton Hurghada Plaza", 5, 5, 1, [1, 2, 3, 4, 5, 9], 1900),
    (7, 5, "Steigenberger Aqua Magic", 5, 5, 2, [1, 2, 3, 6, 7, 9], 2100),
    (8, 6, "Rixos Sharm El Sheikh", 5, 6, 3, [1, 2, 3, 4, 5, 8, 9], 2800),
    (9, 7, "Atlantis The Palm", 5, 3, 7, [1, 2, 3, 4, 5, 6, 7, 8, 9], 7800),
    (10, 7, "Rove Downtown", 3, 2, 4, [1, 2, 5, 10, 12], 2600),
    (11, 8, "Saadiyat Rotana Resort", 5, 3, 3, [1, 2, 3, 4, 5, 8], 5200),
    (12, 9, "Centara Grand Beach Resort Phuket", 5, 2, 1, [1, 2, 3, 4, 6, 9], 4200),
    (13, 10, "Hilton Pattaya", 5, 2, 4, [1, 2, 4, 5, 12], 3500),
    (14, 11, "Kurumba Maldives", 5, 4, 3, [1, 2, 3, 4, 8, 9], 9800),
    (15, 12, "Sheraton Batumi", 5, 2, 1, [1, 2, 4, 5, 12], 1500),
    (16, 13, "Gudauri Lodge", 4, 3, 6, [1, 4, 10, 11], 1300),
    (17, 14, "Rooms Hotel Tbilisi", 4, 2, 4, [1, 5, 8, 12], 1200),
    (18, 15, "Hotel Artemide", 4, 2, 4, [1, 4, 12], 2700),
    (19, 16, "Grand Hotel Rimini", 5, 3, 3, [1, 2, 3, 4, 8], 3100),
    (20, 17, "H10 Marina Barcelona", 4, 2, 4, [1, 2, 5, 12], 2800),
    (21, 18, "PortAventura Hotel Caribe", 4, 3, 2, [1, 2, 6, 7, 10], 2400),
    (22, 3, "Akka Antedon", 4, 5, 5, [1, 2, 3, 5, 10], 1700),
]
vt_desc = {
    1: ("пляжного отдыха", "a beach holiday"), 2: ("семейного отдыха с детьми", "a family holiday"),
    3: ("романтического отдыха вдвоём", "a romantic getaway"), 4: ("экскурсий и прогулок по городу", "sightseeing"),
    5: ("активного отдыха", "an active holiday"), 6: ("горнолыжного отдыха", "a ski holiday"),
    7: ("отдыха премиум-класса", "a premium holiday"),
}
hrows, harows, hprows = [], [], []
for h in hotels:
    hid, cid, name, stars, meal, vt, am, _ = h
    cru, cen = city_name[cid]
    hrows.append((hid, cid, name, stars, f"{cen}, {name}",
                  f"Отель {stars}* в городе {cru}. Отличный выбор для {vt_desc[vt][0]}: "
                  f"продуманный сервис, комфортные номера и удобное расположение.",
                  f"A {stars}-star hotel in {cen}. A great choice for {vt_desc[vt][1]}: "
                  f"attentive service, comfortable rooms and a convenient location."))
    harows += [(hid, a) for a in am]
    for n, kind in enumerate(["main", "pool", "room", "restaurant"], start=1):
        hprows.append((hid, n, f"images/hotels/{slug(name)}-{kind}.jpg"))
insert("hotels", ["hotel_id", "city_id", "name", "stars", "address", "description_ru", "description_en"], hrows)
insert("hotel_amenities", ["hotel_id", "amenity_id"], harows)
insert("hotel_photos", ["hotel_id", "photo_no", "image_path"], hprows)

# ---------------- users ----------------
users = [
    (2, 2, "manager", "manager123", "Ольга Смирнова", "o.smirnova@touragency.local", "+375 29 111-22-33"),
    (3, 2, "manager2", "manager123", "Дмитрий Ковалёв", "d.kovalev@touragency.local", "+375 29 222-33-44"),
]
clients = ["Иван Петров", "Анна Кузнецова", "Сергей Волков", "Мария Новикова", "Алексей Морозов",
           "Екатерина Лебедева", "Павел Соколов", "Наталья Козлова", "Андрей Михайлов", "Юлия Федорова",
           "Максим Орлов", "Ольга Андреева", "Николай Захаров", "Татьяна Григорьева", "Артём Белов"]
translit = {"Иван": "ivan", "Анна": "anna", "Сергей": "sergey", "Мария": "maria", "Алексей": "alexey",
            "Екатерина": "ekaterina", "Павел": "pavel", "Наталья": "natalia", "Андрей": "andrey",
            "Юлия": "yulia", "Максим": "maxim", "Ольга": "olga", "Николай": "nikolay", "Татьяна": "tatiana",
            "Артём": "artem"}
client_ids = []
for i, full in enumerate(clients):
    uid = 4 + i
    login = "client" if i == 0 else f"{translit[full.split()[0]]}{i + 1}"
    users.append((uid, 3, login, "client123", full, f"{login}@mail.by",
                  f"+375 {random.choice(['29', '33', '44', '25'])} {random.randint(100, 999)}-"
                  f"{random.randint(10, 99)}-{random.randint(10, 99)}"))
    client_ids.append(uid)
insert("users", ["user_id", "role_id", "login", "password_hash", "salt", "full_name", "email", "phone",
                 "language", "created_at"],
       [(u[0], u[1], u[2], pwd(u[3]), SALT, u[4], u[5], u[6], "EN" if u[0] == 9 else "RU",
         datetime(2026, 1, 10) + timedelta(days=7 * k)) for k, u in enumerate(users)])
user_by_id = {u[0]: u for u in users}

# ---------------- discounts ----------------
discounts = [
    (1, "Зимнее солнце", "Winter sun", "SEASONAL", 10, date(2026, 12, 1), date(2027, 2, 28), None, None),
    (2, "Весенний сезон", "Spring season", "SEASONAL", 7, date(2027, 3, 1), date(2027, 4, 30), None, None),
    (3, "Раннее бронирование −12%", "Early booking −12%", "EARLY_BOOKING", 12, None, None, 60, None),
    (4, "Раннее бронирование −6%", "Early booking −6%", "EARLY_BOOKING", 6, None, None, 30, None),
    (5, "Постоянный клиент", "Loyal customer", "LOYALTY", 5, None, None, None, 1),
    (6, "Золотой клиент", "Gold customer", "LOYALTY", 9, None, None, None, 3),
]
insert("discounts", ["discount_id", "name_ru", "name_en", "discount_type", "percent", "valid_from", "valid_to",
                     "min_days_before", "min_completed_trips"], discounts)

# ---------------- tours ----------------
# past tours (for completed trips) + future tours
past_starts = [date(2026, 5, 10), date(2026, 6, 5), date(2026, 6, 20), date(2026, 7, 8), date(2026, 7, 25),
               date(2026, 8, 12), date(2026, 9, 3), date(2026, 9, 20)]
past_hotels = [2, 6, 15, 1, 7, 12, 17, 4]
future = [  # hotel, start, nights, seats, featured, created offset days ago
    (1, date(2026, 11, 10), 7, 20, True, 40), (5, date(2026, 11, 14), 10, 4, False, 30),
    (6, date(2026, 11, 20), 7, 30, False, 25), (8, date(2026, 12, 5), 7, 16, True, 20),
    (9, date(2026, 12, 18), 7, 12, True, 60), (10, date(2026, 12, 27), 5, 14, False, 15),
    (12, date(2027, 1, 9), 11, 18, True, 35), (13, date(2027, 1, 16), 10, 20, False, 33),
    (14, date(2027, 1, 22), 7, 8, True, 50), (16, date(2027, 1, 30), 7, 24, False, 45),
    (11, date(2027, 2, 6), 7, 12, False, 12), (7, date(2027, 2, 14), 10, 26, False, 9),
    (17, date(2027, 2, 20), 5, 18, False, 8), (18, date(2027, 3, 12), 6, 16, False, 6),
    (20, date(2027, 3, 26), 7, 20, False, 5), (3, date(2027, 4, 18), 10, 14, True, 4),
    (21, date(2027, 4, 30), 7, 30, False, 3), (2, date(2027, 5, 8), 10, 28, False, 2),
    (19, date(2027, 6, 5), 9, 18, False, 2), (22, date(2027, 6, 12), 10, 22, False, 1),
    (4, date(2027, 6, 19), 12, 26, False, 1), (15, date(2027, 7, 3), 8, 24, False, 1),
]
hotel_info = {h[0]: h for h in hotels}
tours = []
tid = 1


def make_tour(hid, start, nights, seats, featured, created):
    global tid
    h = hotel_info[hid]
    _, cid, name, stars, meal, vt, _, base = h
    cru, cen = city_name[cid]
    price = round(base * nights / 7 * random.uniform(0.9, 1.1) / 10) * 10
    t = dict(id=tid, hotel=hid, vt=vt, meal=meal, start=start, end=start + timedelta(days=nights),
             price=price, seats=seats, featured=featured, created=created, country=city_country[cid],
             stars=stars)
    t["title_ru"] = f"{cru}: {name}, {nights} ноч."
    t["title_en"] = f"{cen}: {name}, {nights} nights"
    tours.append(t)
    tid += 1
    return t


for s, hid in zip(past_starts, past_hotels):
    make_tour(hid, s, random.choice([7, 10]), random.choice([16, 20, 24]), False,
              datetime.combine(s - timedelta(days=90), datetime.min.time()))
for hid, s, n, seats, f, ago in future:
    make_tour(hid, s, n, seats, f, datetime.combine(TODAY - timedelta(days=ago), datetime.min.time()) +
              timedelta(hours=10))
manager_for = lambda t: 2 if t["id"] % 3 else 3
insert("tours", ["tour_id", "hotel_id", "vacation_type_id", "meal_type_id", "created_by", "title_ru", "title_en",
                 "description_ru", "description_en", "start_date", "end_date", "price_per_person", "total_seats",
                 "is_featured", "created_at"],
       [(t["id"], t["hotel"], t["vt"], t["meal"], manager_for(t), t["title_ru"], t["title_en"],
         f"Перелёт из Минска, трансфер, проживание и медицинская страховка включены. "
         f"Тур подойдёт для {vt_desc[t['vt']][0]}.",
         f"Flight from Minsk, transfers, accommodation and medical insurance included. "
         f"Ideal for {vt_desc[t['vt']][1]}.",
         t["start"], t["end"], t["price"], t["seats"], t["featured"], t["created"]) for t in tours])
tour_by_id = {t["id"]: t for t in tours}

# ---------------- preferences ----------------
prefs = {  # user: (min,max budget, from,to, min,max nights, tourists, stars, meal, countries, vtypes)
    4: (2000, 3500, date(2026, 11, 1), date(2027, 1, 31), 7, 10, 2, 5, 5, [1, 2], [1, 2]),
    5: (3000, 9000, date(2026, 12, 1), date(2027, 2, 28), 7, 10, 2, 5, None, [3, 5], [3, 7]),
    6: (1000, 2500, None, None, 5, 8, 1, 4, None, [6], [6, 5]),
    7: (2500, 5000, date(2027, 1, 1), date(2027, 3, 31), 9, 14, 3, 4, 2, [4], [1, 2]),
    8: (None, 4000, date(2027, 3, 1), date(2027, 5, 31), 5, 8, 2, 4, None, [7, 8], [4]),
    9: (1500, 3000, None, None, 7, 10, 4, 5, 5, [1, 2], [2]),
    10: (6000, 15000, None, None, 7, 10, 2, 5, 4, [5, 3], [3, 7]),
    11: (None, 2000, None, None, None, None, None, None, None, [6, 2], []),
    12: (2000, 4000, date(2027, 4, 1), date(2027, 6, 30), 10, 12, 4, 5, 6, [1], [2, 1]),
    13: (1800, 3200, date(2026, 11, 10), date(2026, 12, 31), 7, 7, 2, 5, None, [2], [1, 3]),
}
insert("client_preferences", ["user_id", "min_budget", "max_budget", "desired_from", "desired_to", "min_nights",
                              "max_nights", "tourists_count", "min_hotel_stars", "meal_type_id"],
       [(u,) + p[:9] for u, p in prefs.items()])
insert("preference_countries", ["user_id", "country_id"], [(u, c) for u, p in prefs.items() for c in p[9]])
pvt = [(u, v) for u, p in prefs.items() for v in p[10]]
insert("preference_vacation_types", ["user_id", "vacation_type_id"], pvt)

# ---------------- bookings ----------------
ST = {"NEW": 1, "PENDING_CONFIRMATION": 2, "CONFIRMED": 3, "AWAITING_PAYMENT": 4, "PAID": 5,
      "COMPLETED": 6, "CANCELLED": 7}
FLOW = ["NEW", "PENDING_CONFIRMATION", "CONFIRMED", "AWAITING_PAYMENT", "PAID", "COMPLETED"]
completed_trips = {}  # user -> number completed before a given booking date (simplified: running count)
bookings, tourists, history, payments = [], [], [], []
bid = 1
first_names = ["Иван", "Анна", "Мария", "Пётр", "Елена", "Кирилл", "София", "Михаил"]


def pick_discount(user, tour, created):
    best = None
    days_before = (tour["start"] - created.date()).days
    for d in discounts:
        did, _, _, typ, pct, vf, vt, mdb, mct = d
        ok = (typ == "SEASONAL" and vf <= tour["start"] <= vt) or \
             (typ == "EARLY_BOOKING" and days_before >= mdb) or \
             (typ == "LOYALTY" and completed_trips.get(user, 0) >= mct)
        if ok and (best is None or pct > best[4]):
            best = d
    return best


def add_booking(user, tour_id, persons, final_status, created, penalty_days=None):
    global bid
    t = tour_by_id[tour_id]
    d = pick_discount(user, t, created)
    base = t["price"] * persons
    disc = round(base * d[4] / 100, 2) if d else 0
    total = round(base - disc, 2)
    u = user_by_id[user]
    penalty = refund = None
    hist_flow = FLOW[:FLOW.index(final_status) + 1] if final_status != "CANCELLED" else FLOW[:5]
    if final_status == "CANCELLED":
        hist_flow = FLOW[:5] + ["CANCELLED"]
        cancel_date = t["start"] - timedelta(days=penalty_days)
        pct = 5 if penalty_days > 30 else 15 if penalty_days >= 15 else 30 if penalty_days >= 7 else \
            50 if penalty_days >= 3 else 80
        penalty = round(total * pct / 100, 2)
        refund = round(total - penalty, 2)
    bookings.append((bid, user, tour_id, ST[final_status], d[0] if d else None, persons, base, disc, total,
                     u[6], u[5], penalty, refund, created))
    for n in range(persons):
        name = u[4] if n == 0 else f"{random.choice(first_names)} {u[4].split()[1]}"
        bd = date(1975 + random.randint(0, 25), random.randint(1, 12), random.randint(1, 28)) if n < 2 else \
            date(2012 + random.randint(0, 8), random.randint(1, 12), random.randint(1, 28))
        tourists.append((bid, n + 1, name, bd))
    ts = created
    for code in hist_flow:
        if code == "CANCELLED":
            ts = datetime.combine(cancel_date, datetime.min.time()) + timedelta(hours=11)
        elif code == "COMPLETED":
            ts = datetime.combine(t["end"], datetime.min.time()) + timedelta(hours=12)
        changed_by = user if code in ("NEW", "PAID", "CANCELLED") else manager_for(t)
        comment = {"CANCELLED": f"Отмена клиентом за {penalty_days} дн. до начала тура"}.get(code)
        history.append((bid, ST[code], changed_by, comment, ts))
        if code == "PAID":
            payments.append((bid, total, random.choice(["CARD", "CARD", "TRANSFER", "CASH"]), ts))
        if code not in ("CANCELLED", "COMPLETED"):
            ts = ts + timedelta(hours=random.randint(3, 30))
    if final_status == "COMPLETED":
        completed_trips[user] = completed_trips.get(user, 0) + 1
    bid += 1
    return bid - 1


def dt(d, h=12):
    return datetime.combine(d, datetime.min.time()) + timedelta(hours=h)


# completed trips on past tours (ids 1..8)
completed_plan = [(4, 1, 2), (5, 2, 2), (6, 3, 1), (4, 4, 2), (7, 4, 3), (8, 5, 2), (9, 6, 4), (10, 7, 2),
                  (11, 8, 2), (5, 6, 2), (12, 2, 4), (13, 5, 2)]
completed_ids = []
for u, t, p in completed_plan:
    completed_ids.append(add_booking(u, t, p, "COMPLETED", dt(tour_by_id[t]["start"] - timedelta(days=45))))

# future bookings (tour ids 9..30)
plan = [
    (4, 9, 2, "PAID", 20), (14, 10, 2, "PAID", 25), (15, 10, 1, "CONFIRMED", 5),   # tour 10: 4 seats -> 1 left
    (5, 13, 2, "PAID", 18), (10, 13, 2, "AWAITING_PAYMENT", 6), (6, 18, 1, "PAID", 30),
    (7, 15, 3, "CONFIRMED", 10), (16, 11, 2, "PENDING_CONFIRMATION", 2), (17, 12, 4, "NEW", 1),
    (8, 22, 2, "NEW", 1), (9, 24, 4, "AWAITING_PAYMENT", 3), (18, 17, 2, "PAID", 28),
    (11, 21, 2, "PENDING_CONFIRMATION", 2), (12, 26, 4, "CONFIRMED", 2),
]
for u, t, p, st, ago in plan:
    add_booking(u, t, p, st, dt(TODAY - timedelta(days=ago), 10 + ago % 8))
# cancellations with different penalty brackets
add_booking(13, 16, 2, "CANCELLED", dt(TODAY - timedelta(days=40)), penalty_days=52)
add_booking(14, 14, 2, "CANCELLED", dt(TODAY - timedelta(days=50)), penalty_days=20)
add_booking(15, 9, 1, "CANCELLED", dt(TODAY - timedelta(days=30)), penalty_days=9)

insert("bookings", ["booking_id", "user_id", "tour_id", "status_id", "discount_id", "persons_count", "base_price",
                    "discount_amount", "total_price", "contact_phone", "contact_email", "cancel_penalty",
                    "refund_amount", "created_at"], bookings)
insert("booking_tourists", ["booking_id", "tourist_no", "full_name", "birth_date"], tourists)
insert("booking_status_history", ["booking_id", "status_id", "changed_by", "comment", "changed_at"], history)
insert("payments", ["booking_id", "amount", "method", "paid_at"], payments)

# seat check
for t in tours:
    used = sum(b[5] for b in bookings if b[2] == t["id"] and b[3] != ST["CANCELLED"])
    assert used <= t["seats"], (t["id"], used, t["seats"])

# ---------------- reviews ----------------
texts = [
    (5, "Прекрасный отель, отличная кухня и внимательный персонал. Обязательно вернёмся!"),
    (4, "Хорошее расположение и чистое море. Немного шумно вечером, но в целом всё понравилось."),
    (5, "Лучший отдых за последние годы: анимация для детей, большой аквапарк."),
    (3, "Номер соответствовал описанию, но заселение заняло больше часа."),
    (5, "Идеально для отдыха вдвоём, красивые закаты и уютный ресторан."),
    (4, "Хороший сервис, вкусные завтраки, удобно добираться до центра."),
    (5, "Всё на высшем уровне: от трансфера до номера с видом на море."),
    (4, "Отличный пляж и бассейны, питание разнообразное."),
    (2, "Ожидали большего от пятизвёздочного отеля: старый ремонт в номере."),
    (5, "Экскурсии организованы прекрасно, гид говорил по-русски."),
    (4, "Спокойный отдых, приятная территория, рекомендую семьям."),
    (5, "Сказочное место, персонал запоминает гостей по именам."),
]
rrows = []
for bid_, (rating, text) in zip(completed_ids, texts):
    b = bookings[bid_ - 1]
    created = dt(tour_by_id[b[2]]["end"] + timedelta(days=3), 19)
    rrows.append((bid_, rating, text, created))
insert("reviews", ["booking_id", "rating", "comment", "created_at"], rrows)

# ---------------- favorites ----------------
fav = [(4, 11), (4, 23), (5, 13), (5, 17), (6, 18), (7, 15), (7, 16), (8, 22), (8, 23), (9, 24), (10, 17),
       (10, 13), (12, 26), (13, 9), (14, 20), (16, 19)]
insert("favorites", ["user_id", "tour_id", "added_at"],
       [(u, t, dt(TODAY - timedelta(days=(u + t) % 20), 15)) for u, t in fav])

# ---------------- notifications ----------------
notes = [  # message: процент совпадения или код статуса — текст строит клиент на нужном языке
    (4, "NEW_MATCHING_TOUR", 9, None, "92", True, 39),
    (4, "BOOKING_STATUS", None, 13, "PAID", True, 19),
    (5, "NEW_MATCHING_TOUR", 13, None, "88", True, 59),
    (5, "DISCOUNT_OFFER", 17, None, "10", False, 3),
    (6, "NEW_MATCHING_TOUR", 18, None, "95", False, 44),
    (7, "BOOKING_STATUS", None, 19, "CONFIRMED", False, 9),
    (10, "DISCOUNT_OFFER", 17, None, "12", False, 4),
    (12, "NEW_MATCHING_TOUR", 26, None, "90", False, 1),
    (13, "NEW_MATCHING_TOUR", 9, None, "81", False, 2),
    (9, "NEW_MATCHING_TOUR", 29, None, "86", False, 1),
    (11, "NEW_MATCHING_TOUR", 30, None, "78", False, 1),
    (14, "SYSTEM", None, None, "WELCOME", True, 60),
    (4, "NEW_MATCHING_TOUR", 23, None, "84", False, 2),
    (4, "DISCOUNT_OFFER", 11, None, "12", False, 1),
]
insert("notifications", ["user_id", "type", "tour_id", "booking_id", "message", "is_read", "created_at"],
       [(u, ty, t, b, m, r, dt(TODAY - timedelta(days=d), 9)) for u, ty, t, b, m, r, d in notes])

# ---------------- audit log ----------------
audit = [
    (1, "LOGIN", None, None, "Вход в систему", 5),
    (2, "CREATE", "TOUR", 30, "Создан тур: Батуми, Sheraton Batumi", 1),
    (2, "UPDATE", "TOUR", 10, "price_per_person: 2600 → 2550", 2),
    (3, "UPDATE", "BOOKING", 19, "status: PENDING_CONFIRMATION → CONFIRMED", 9),
    (1, "UPDATE", "USER", 18, "is_blocked: false → true", 3),
    (1, "UPDATE", "USER", 18, "is_blocked: true → false", 2),
    (2, "CREATE", "DISCOUNT", 2, "Создана скидка «Весенний сезон» 7%", 6),
]
insert("audit_log", ["user_id", "action", "object_type", "object_id", "details", "created_at"],
       [(u, a, ot, oi, d, dt(TODAY - timedelta(days=ago), 11)) for u, a, ot, oi, d, ago in audit])


# ---------------- v2: личный кабинет, промокоды, история цен, просмотры, погода ----------------
coords = {1: (36.8969, 30.7133), 2: (36.8625, 31.0556), 3: (36.5983, 30.5594), 4: (36.7675, 31.3889),
          5: (27.2579, 33.8116), 6: (27.9158, 34.3300), 7: (25.2048, 55.2708), 8: (24.4539, 54.3773),
          9: (7.8804, 98.3923), 10: (12.9236, 100.8825), 11: (4.4170, 73.5000), 12: (41.6168, 41.6367),
          13: (42.4783, 44.4806), 14: (41.7151, 44.8271), 15: (41.9028, 12.4964), 16: (44.0678, 12.5695),
          17: (41.3874, 2.1686), 18: (41.0764, 1.1316)}
out.append("-- координаты городов для прогноза погоды (Open-Meteo)")
out.extend(f"UPDATE cities SET latitude = {la}, longitude = {lo} WHERE city_id = {cid};"
           for cid, (la, lo) in coords.items())
out.append("")
out.append("-- безвизовый въезд для граждан Беларуси (демо-данные: перед поездкой правила стоит уточнить)")
out.extend(f"UPDATE countries SET visa_free = TRUE WHERE country_id = {cid};" for cid in (1, 5, 6))
out.append("")
out.append("-- данные личного кабинета")
out.append("UPDATE users SET birth_date = '1998-04-12', home_city = 'Минск' WHERE user_id = 4;")
out.append("UPDATE users SET birth_date = '1995-09-30', home_city = 'Гродно' WHERE user_id = 5;")
out.append("")
insert("discounts", ["discount_id", "name_ru", "name_en", "discount_type", "percent", "valid_from", "valid_to",
                     "promo_code", "max_uses"],
       [(7, "Промокод студента", "Student promo code", "PROMO_CODE", 15, date(2026, 9, 1), date(2027, 6, 30),
         "STUDENT15", 50),
        (8, "Промокод «Добро пожаловать»", "Welcome promo code", "PROMO_CODE", 8, None, None, "WELCOME8", None)])
insert("tour_price_history", ["tour_id", "old_price", "new_price", "changed_by", "changed_at"],
       [(9, 3990, 3790, 2, dt(TODAY - timedelta(days=6), 10)),
        (11, 2290, 2020, 2, dt(TODAY - timedelta(days=3), 10)),
        (23, 2750, 2600, 2, dt(TODAY - timedelta(days=2), 10))])
insert("notifications", ["user_id", "type", "tour_id", "booking_id", "message", "is_read", "created_at"],
       [(4, "PRICE_DROP", 11, None, "2290;2020", False, dt(TODAY - timedelta(days=3), 10)),
        (4, "PRICE_DROP", 23, None, "2750;2600", False, dt(TODAY - timedelta(days=2), 10))])
insert("tour_views", ["user_id", "tour_id", "viewed_at", "views_count"],
       [(4, 12, dt(TODAY - timedelta(days=1), 18), 2), (4, 17, dt(TODAY - timedelta(days=1), 17), 1),
        (4, 23, dt(TODAY - timedelta(days=2), 20), 3), (4, 13, dt(TODAY - timedelta(days=3), 12), 1),
        (4, 24, dt(TODAY - timedelta(days=4), 15), 1)])

# групповая бронь: тур №13 занят больше чем на 80 % — метка «Почти распродан»
gu = user_by_id[7]
gp = tour_by_id[13]["price"] * 6
insert("bookings", ["booking_id", "user_id", "tour_id", "status_id", "discount_id", "persons_count", "base_price",
                    "discount_amount", "total_price", "contact_phone", "contact_email", "created_at"],
       [(bid, 7, 13, ST["CONFIRMED"], None, 6, gp, 0, gp, gu[6], gu[5], dt(TODAY - timedelta(days=2), 13))])
insert("booking_tourists", ["booking_id", "tourist_no", "full_name", "birth_date"],
       [(bid, n + 1, gu[4] if n == 0 else f"Турист {n + 1} {gu[4].split()[1]}", date(1980 + n * 4, 3 + n, 10))
        for n in range(6)])
insert("booking_status_history", ["booking_id", "status_id", "changed_by", "comment", "changed_at"],
       [(bid, ST["NEW"], 7, "Групповая поездка", dt(TODAY - timedelta(days=2), 13)),
        (bid, ST["PENDING_CONFIRMATION"], 2, None, dt(TODAY - timedelta(days=2), 17)),
        (bid, ST["CONFIRMED"], 2, None, dt(TODAY - timedelta(days=1), 11))])

header = """-- =====================================================================
--  Демонстрационные данные (выполнять ПОСЛЕ tour_agency_db.sql)
--  Даты рассчитаны относительно 02.11.2026.
--  Учётные записи: admin / admin123, manager / manager123, manager2 / manager123,
--  клиенты: client / client123 (Иван Петров), anna2 ... artem15 / client123
-- =====================================================================

SET NAMES utf8mb4;
USE tour_agency;

"""
open("demo_data.sql", "w", encoding="utf-8").write(header + "\n".join(out))
print("tours", len(tours), "bookings", len(bookings), "reviews", len(rrows), "hotels", len(hotels),
      "cities", len(cities), "clients", len(client_ids), "notes", len(notes))
print("logins:", [u[2] for u in users])
