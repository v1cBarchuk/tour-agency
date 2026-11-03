-- =====================================================================
--  Программное средство поддержки ключевых бизнес-процессов
--  туристической компании с учетом потребительских предпочтений
--  Скрипт генерации базы данных (MySQL 8.0)
--  Демонстрационные данные: demo_data.sql (выполнять после этого скрипта)
-- =====================================================================

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS tour_agency;
CREATE DATABASE tour_agency
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE tour_agency;

-- ---------------------------------------------------------------------
-- 1. Справочники
--    code — неизменяемый ключ для логики и перевода интерфейса (RU/EN)
-- ---------------------------------------------------------------------

CREATE TABLE roles (
    role_id     TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)      NOT NULL,
    PRIMARY KEY (role_id),
    UNIQUE KEY uq_roles_code (code)
) ENGINE = InnoDB;

CREATE TABLE booking_statuses (
    status_id   TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)      NOT NULL,
    name_ru     VARCHAR(50)      NOT NULL,
    name_en     VARCHAR(50)      NOT NULL,
    sort_order  TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (status_id),
    UNIQUE KEY uq_booking_statuses_code (code)
) ENGINE = InnoDB;

CREATE TABLE vacation_types (
    vacation_type_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code             VARCHAR(30)  NOT NULL,
    name_ru          VARCHAR(50)  NOT NULL,
    name_en          VARCHAR(50)  NOT NULL,
    PRIMARY KEY (vacation_type_id),
    UNIQUE KEY uq_vacation_types_code (code)
) ENGINE = InnoDB;

CREATE TABLE meal_types (
    meal_type_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code         VARCHAR(10)  NOT NULL,              -- RO, BB, HB, FB, AI, UAI
    name_ru      VARCHAR(50)  NOT NULL,
    name_en      VARCHAR(50)  NOT NULL,
    sort_order   TINYINT UNSIGNED NOT NULL,          -- для сравнения «не хуже, чем»
    PRIMARY KEY (meal_type_id),
    UNIQUE KEY uq_meal_types_code (code)
) ENGINE = InnoDB;

CREATE TABLE amenities (
    amenity_id  INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(30)  NOT NULL,
    name_ru     VARCHAR(60)  NOT NULL,
    name_en     VARCHAR(60)  NOT NULL,
    PRIMARY KEY (amenity_id),
    UNIQUE KEY uq_amenities_code (code)
) ENGINE = InnoDB;

-- Скидки. Условия применения зависят от типа:
--   SEASONAL      — дата начала тура в [valid_from; valid_to]
--   EARLY_BOOKING — бронь не позднее чем за min_days_before дней до начала тура
--   LOYALTY       — у клиента не менее min_completed_trips завершённых поездок
-- К одному бронированию применяется не более одной скидки (наибольшая).
CREATE TABLE discounts (
    discount_id         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name_ru             VARCHAR(80)  NOT NULL,
    name_en             VARCHAR(80)  NOT NULL,
    discount_type       ENUM('SEASONAL', 'LOYALTY', 'EARLY_BOOKING') NOT NULL,
    percent             DECIMAL(5,2) NOT NULL,
    valid_from          DATE         NULL,
    valid_to            DATE         NULL,
    min_days_before     SMALLINT UNSIGNED NULL,
    min_completed_trips TINYINT UNSIGNED  NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (discount_id),
    CONSTRAINT chk_discounts_percent CHECK (percent > 0 AND percent < 100),
    CONSTRAINT chk_discounts_period  CHECK (valid_to IS NULL OR valid_from IS NULL
                                            OR valid_to >= valid_from),
    CONSTRAINT chk_discounts_params  CHECK (
        (discount_type = 'SEASONAL'      AND valid_from IS NOT NULL AND valid_to IS NOT NULL) OR
        (discount_type = 'EARLY_BOOKING' AND min_days_before IS NOT NULL) OR
        (discount_type = 'LOYALTY'       AND min_completed_trips IS NOT NULL))
) ENGINE = InnoDB;

-- Системные настройки (курс валют, параметры штрафов и т.п.)
CREATE TABLE settings (
    setting_key   VARCHAR(50)  NOT NULL,
    setting_value VARCHAR(200) NOT NULL,
    description   VARCHAR(200) NULL,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
                               ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. География и отели
-- ---------------------------------------------------------------------

CREATE TABLE countries (
    country_id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name_ru        VARCHAR(80)  NOT NULL,
    name_en        VARCHAR(80)  NOT NULL,
    description_ru TEXT         NULL,
    description_en TEXT         NULL,
    image_path     VARCHAR(255) NULL,
    PRIMARY KEY (country_id),
    UNIQUE KEY uq_countries_name_ru (name_ru),
    UNIQUE KEY uq_countries_name_en (name_en)
) ENGINE = InnoDB;

CREATE TABLE cities (
    city_id     INT UNSIGNED NOT NULL AUTO_INCREMENT,
    country_id  INT UNSIGNED NOT NULL,
    name_ru     VARCHAR(80)  NOT NULL,
    name_en     VARCHAR(80)  NOT NULL,
    image_path  VARCHAR(255) NULL,
    PRIMARY KEY (city_id),
    UNIQUE KEY uq_cities_country_name (country_id, name_ru),
    CONSTRAINT fk_cities_country FOREIGN KEY (country_id)
        REFERENCES countries (country_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Рейтинг отеля не хранится: вычисляется по отзывам (AVG), чтобы не нарушать 3НФ
CREATE TABLE hotels (
    hotel_id       INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    city_id        INT UNSIGNED     NOT NULL,
    name           VARCHAR(120)     NOT NULL,
    stars          TINYINT UNSIGNED NOT NULL,
    address        VARCHAR(200)     NULL,
    description_ru TEXT             NULL,
    description_en TEXT             NULL,
    PRIMARY KEY (hotel_id),
    UNIQUE KEY uq_hotels_city_name (city_id, name),
    CONSTRAINT fk_hotels_city FOREIGN KEY (city_id)
        REFERENCES cities (city_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_hotels_stars CHECK (stars BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- Удобства отеля: M:N
CREATE TABLE hotel_amenities (
    hotel_id    INT UNSIGNED NOT NULL,
    amenity_id  INT UNSIGNED NOT NULL,
    PRIMARY KEY (hotel_id, amenity_id),
    CONSTRAINT fk_hotel_amenities_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (hotel_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_hotel_amenities_amenity FOREIGN KEY (amenity_id)
        REFERENCES amenities (amenity_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- Фотографии отеля: 1:N (идентифицирующая связь, PK включает hotel_id)
CREATE TABLE hotel_photos (
    hotel_id    INT UNSIGNED     NOT NULL,
    photo_no    TINYINT UNSIGNED NOT NULL,           -- 1 — главное фото
    image_path  VARCHAR(255)     NOT NULL,
    PRIMARY KEY (hotel_id, photo_no),
    CONSTRAINT fk_hotel_photos_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (hotel_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. Пользователи и анкета предпочтений
-- ---------------------------------------------------------------------

CREATE TABLE users (
    user_id       INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    role_id       TINYINT UNSIGNED NOT NULL,
    login         VARCHAR(50)      NOT NULL,
    password_hash CHAR(64)         NOT NULL,           -- SHA-256(salt + пароль), hex
    salt          CHAR(32)         NOT NULL,
    full_name     VARCHAR(120)     NOT NULL,
    email         VARCHAR(120)     NOT NULL,
    phone         VARCHAR(20)      NULL,
    language      ENUM('RU', 'EN') NOT NULL DEFAULT 'RU',
    is_blocked    BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_login (login),
    UNIQUE KEY uq_users_email (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id)
        REFERENCES roles (role_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE = InnoDB;

-- Анкета: 1:0..1 к users (идентифицирующая связь). Все критерии необязательны.
CREATE TABLE client_preferences (
    user_id         INT UNSIGNED     NOT NULL,
    min_budget      DECIMAL(10,2)    NULL,             -- BYN на человека
    max_budget      DECIMAL(10,2)    NULL,
    desired_from    DATE             NULL,
    desired_to      DATE             NULL,
    min_nights      TINYINT UNSIGNED NULL,
    max_nights      TINYINT UNSIGNED NULL,
    tourists_count  TINYINT UNSIGNED NULL,
    min_hotel_stars TINYINT UNSIGNED NULL,
    meal_type_id    INT UNSIGNED     NULL,
    updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP
                                     ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_prefs_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_prefs_meal FOREIGN KEY (meal_type_id)
        REFERENCES meal_types (meal_type_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_prefs_budget  CHECK (max_budget IS NULL OR min_budget IS NULL
                                        OR max_budget >= min_budget),
    CONSTRAINT chk_prefs_dates   CHECK (desired_to IS NULL OR desired_from IS NULL
                                        OR desired_to >= desired_from),
    CONSTRAINT chk_prefs_nights  CHECK (max_nights IS NULL OR min_nights IS NULL
                                        OR max_nights >= min_nights),
    CONSTRAINT chk_prefs_stars   CHECK (min_hotel_stars IS NULL
                                        OR min_hotel_stars BETWEEN 1 AND 5),
    CONSTRAINT chk_prefs_tourists CHECK (tourists_count IS NULL OR tourists_count > 0)
) ENGINE = InnoDB;

CREATE TABLE preference_countries (
    user_id     INT UNSIGNED NOT NULL,
    country_id  INT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, country_id),
    CONSTRAINT fk_pref_countries_pref FOREIGN KEY (user_id)
        REFERENCES client_preferences (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_pref_countries_country FOREIGN KEY (country_id)
        REFERENCES countries (country_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE preference_vacation_types (
    user_id          INT UNSIGNED NOT NULL,
    vacation_type_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, vacation_type_id),
    CONSTRAINT fk_pref_vtypes_pref FOREIGN KEY (user_id)
        REFERENCES client_preferences (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_pref_vtypes_vtype FOREIGN KEY (vacation_type_id)
        REFERENCES vacation_types (vacation_type_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. Туры
--    Кол-во ночей = DATEDIFF(end_date, start_date);
--    свободные места = total_seats − SUM(persons_count) неотменённых броней.
--    Метки «Горящий», «Новинка», «Популярный» вычисляются, «Рекомендуем» — is_featured.
-- ---------------------------------------------------------------------

CREATE TABLE tours (
    tour_id          INT UNSIGNED      NOT NULL AUTO_INCREMENT,
    hotel_id         INT UNSIGNED      NOT NULL,
    vacation_type_id INT UNSIGNED      NOT NULL,
    meal_type_id     INT UNSIGNED      NOT NULL,
    created_by       INT UNSIGNED      NOT NULL,       -- менеджер
    title_ru         VARCHAR(150)      NOT NULL,
    title_en         VARCHAR(150)      NOT NULL,
    description_ru   TEXT              NULL,
    description_en   TEXT              NULL,
    start_date       DATE              NOT NULL,
    end_date         DATE              NOT NULL,
    price_per_person DECIMAL(10,2)     NOT NULL,       -- BYN
    total_seats      SMALLINT UNSIGNED NOT NULL,
    image_path       VARCHAR(255)      NULL,           -- если NULL — главное фото отеля
    is_featured      BOOLEAN           NOT NULL DEFAULT FALSE,
    is_active        BOOLEAN           NOT NULL DEFAULT TRUE,
    created_at       DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tour_id),
    KEY idx_tours_dates (start_date, end_date),
    KEY idx_tours_price (price_per_person),
    CONSTRAINT fk_tours_hotel FOREIGN KEY (hotel_id)
        REFERENCES hotels (hotel_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tours_vtype FOREIGN KEY (vacation_type_id)
        REFERENCES vacation_types (vacation_type_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tours_meal FOREIGN KEY (meal_type_id)
        REFERENCES meal_types (meal_type_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_tours_creator FOREIGN KEY (created_by)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_tours_dates CHECK (end_date > start_date),
    CONSTRAINT chk_tours_price CHECK (price_per_person > 0),
    CONSTRAINT chk_tours_seats CHECK (total_seats > 0)
) ENGINE = InnoDB;

-- Избранное: M:N между клиентами и турами
CREATE TABLE favorites (
    user_id     INT UNSIGNED NOT NULL,
    tour_id     INT UNSIGNED NOT NULL,
    added_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tour_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_favorites_tour FOREIGN KEY (tour_id)
        REFERENCES tours (tour_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. Бронирования
-- ---------------------------------------------------------------------

-- Цены хранятся «как на момент брони»: base_price и discount_amount фиксируют
-- исторические значения и не зависят от последующих изменений тура и скидок.
CREATE TABLE bookings (
    booking_id      INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    user_id         INT UNSIGNED     NOT NULL,         -- клиент
    tour_id         INT UNSIGNED     NOT NULL,
    status_id       TINYINT UNSIGNED NOT NULL,         -- текущий статус
    discount_id     INT UNSIGNED     NULL,
    persons_count   TINYINT UNSIGNED NOT NULL,
    base_price      DECIMAL(10,2)    NOT NULL,         -- цена × туристы, без скидки
    discount_amount DECIMAL(10,2)    NOT NULL DEFAULT 0,
    total_price     DECIMAL(10,2)    NOT NULL,         -- к оплате
    contact_phone   VARCHAR(20)      NOT NULL,
    contact_email   VARCHAR(120)     NOT NULL,
    cancel_penalty  DECIMAL(10,2)    NULL,             -- заполняется при отмене
    refund_amount   DECIMAL(10,2)    NULL,
    created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (booking_id),
    KEY idx_bookings_tour_status (tour_id, status_id),
    KEY idx_bookings_user (user_id),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_tour FOREIGN KEY (tour_id)
        REFERENCES tours (tour_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_status FOREIGN KEY (status_id)
        REFERENCES booking_statuses (status_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_discount FOREIGN KEY (discount_id)
        REFERENCES discounts (discount_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT chk_bookings_persons CHECK (persons_count > 0),
    CONSTRAINT chk_bookings_prices  CHECK (base_price >= 0 AND discount_amount >= 0
                                           AND total_price >= 0)
) ENGINE = InnoDB;

-- Туристы в бронировании: 1:N (идентифицирующая связь)
CREATE TABLE booking_tourists (
    booking_id  INT UNSIGNED     NOT NULL,
    tourist_no  TINYINT UNSIGNED NOT NULL,
    full_name   VARCHAR(120)     NOT NULL,
    birth_date  DATE             NOT NULL,
    PRIMARY KEY (booking_id, tourist_no),
    CONSTRAINT fk_booking_tourists_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (booking_id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE = InnoDB;

-- История статусов: временная шкала заявки
CREATE TABLE booking_status_history (
    history_id  INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    booking_id  INT UNSIGNED     NOT NULL,
    status_id   TINYINT UNSIGNED NOT NULL,
    changed_by  INT UNSIGNED     NULL,
    comment     VARCHAR(300)     NULL,
    changed_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    KEY idx_history_booking (booking_id, changed_at),
    CONSTRAINT fk_history_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (booking_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_history_status FOREIGN KEY (status_id)
        REFERENCES booking_statuses (status_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_history_user FOREIGN KEY (changed_by)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE payments (
    payment_id  INT UNSIGNED  NOT NULL AUTO_INCREMENT,
    booking_id  INT UNSIGNED  NOT NULL,
    amount      DECIMAL(10,2) NOT NULL,
    method      ENUM('CARD', 'CASH', 'TRANSFER') NOT NULL,
    paid_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (booking_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE = InnoDB;

-- Отзыв привязан к бронированию (только завершённая поездка, один отзыв на бронь).
-- Клиент, тур и отель определяются через бронирование — без дублирования.
CREATE TABLE reviews (
    review_id   INT UNSIGNED     NOT NULL AUTO_INCREMENT,
    booking_id  INT UNSIGNED     NOT NULL,
    rating      TINYINT UNSIGNED NOT NULL,
    comment     TEXT             NULL,
    created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (review_id),
    UNIQUE KEY uq_reviews_booking (booking_id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (booking_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 6. Уведомления и журнал действий
-- ---------------------------------------------------------------------

-- Текст уведомления формируется клиентом по type и связанным объектам
-- (так он переводится на RU/EN); message — дополнительный текст.
CREATE TABLE notifications (
    notification_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         INT UNSIGNED NOT NULL,
    type            ENUM('NEW_MATCHING_TOUR', 'DISCOUNT_OFFER', 'BOOKING_STATUS', 'SYSTEM') NOT NULL,
    tour_id         INT UNSIGNED NULL,
    booking_id      INT UNSIGNED NULL,
    message         VARCHAR(300) NULL,
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (notification_id),
    KEY idx_notifications_user_read (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_notifications_tour FOREIGN KEY (tour_id)
        REFERENCES tours (tour_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_notifications_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (booking_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE audit_log (
    log_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     INT UNSIGNED    NULL,
    action      VARCHAR(50)     NOT NULL,              -- LOGIN, CREATE, UPDATE, ...
    object_type VARCHAR(30)     NULL,                  -- TOUR, BOOKING, USER, ...
    object_id   INT UNSIGNED    NULL,
    details     TEXT            NULL,                  -- изменённые данные
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    KEY idx_audit_created (created_at),
    KEY idx_audit_object (object_type, object_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 7. Обязательные справочные данные
-- ---------------------------------------------------------------------

INSERT INTO roles (role_id, code) VALUES (1, 'ADMIN'), (2, 'MANAGER'), (3, 'CLIENT');

INSERT INTO booking_statuses (status_id, code, name_ru, name_en, sort_order) VALUES
    (1, 'NEW',                  'Новая заявка',             'New request',           1),
    (2, 'PENDING_CONFIRMATION', 'Ожидает подтверждения',    'Pending confirmation',  2),
    (3, 'CONFIRMED',            'Подтверждена менеджером',  'Confirmed by manager',  3),
    (4, 'AWAITING_PAYMENT',     'Ожидает оплаты',           'Awaiting payment',      4),
    (5, 'PAID',                 'Оплачена',                 'Paid',                  5),
    (6, 'COMPLETED',            'Тур завершён',             'Trip completed',        6),
    (7, 'CANCELLED',            'Отменена',                 'Cancelled',             7);

INSERT INTO vacation_types (vacation_type_id, code, name_ru, name_en) VALUES
    (1, 'BEACH',      'Пляжный',       'Beach'),
    (2, 'FAMILY',     'Семейный',      'Family'),
    (3, 'ROMANTIC',   'Романтический', 'Romantic'),
    (4, 'SIGHTSEEING','Экскурсионный', 'Sightseeing'),
    (5, 'ACTIVE',     'Активный',      'Active'),
    (6, 'SKI',        'Горнолыжный',   'Ski'),
    (7, 'PREMIUM',    'Премиальный',   'Premium');

INSERT INTO meal_types (meal_type_id, code, name_ru, name_en, sort_order) VALUES
    (1, 'RO',  'Без питания',          'Room only',          1),
    (2, 'BB',  'Завтраки',             'Bed & breakfast',    2),
    (3, 'HB',  'Полупансион',          'Half board',         3),
    (4, 'FB',  'Полный пансион',       'Full board',         4),
    (5, 'AI',  'Всё включено',         'All inclusive',      5),
    (6, 'UAI', 'Ультра всё включено',  'Ultra all inclusive',6);

INSERT INTO amenities (amenity_id, code, name_ru, name_en) VALUES
    (1,  'WIFI',        'Бесплатный Wi-Fi',       'Free Wi-Fi'),
    (2,  'POOL',        'Бассейн',                'Swimming pool'),
    (3,  'BEACH',       'Собственный пляж',       'Private beach'),
    (4,  'SPA',         'СПА-центр',              'Spa'),
    (5,  'GYM',         'Фитнес-зал',             'Gym'),
    (6,  'KIDS_CLUB',   'Детский клуб',           'Kids club'),
    (7,  'AQUAPARK',    'Аквапарк',               'Water park'),
    (8,  'RESTAURANT',  'Ресторан à la carte',    'A la carte restaurant'),
    (9,  'TRANSFER',    'Трансфер из аэропорта',  'Airport transfer'),
    (10, 'PARKING',     'Парковка',               'Parking'),
    (11, 'SKI_STORAGE', 'Лыжехранилище',          'Ski storage'),
    (12, 'CITY_CENTER', 'Центр города рядом',     'Near city centre');

INSERT INTO settings (setting_key, setting_value, description) VALUES
    ('currency.base',        'BYN',  'Основная валюта системы'),
    ('currency.rate.USD',    '3.27', 'Курс: 1 USD в BYN (демо, обновляется вручную или через API)'),
    ('currency.rate.EUR',    '3.55', 'Курс: 1 EUR в BYN'),
    ('penalty.scale',        '31:5;15:15;7:30;3:50;0:80',
                             'Штраф при отмене: «не менее N дней до тура : процент», от большего к меньшему'),
    ('tour.hot.days',        '14',   'Тур «горящий», если до начала не более N дней'),
    ('tour.new.days',        '10',   'Тур «новинка», если добавлен не более N дней назад'),
    ('tour.popular.bookings', '2',   'Тур «популярный», если у него не меньше N бронирований'),
    ('match.notify.percent', '60',   'Уведомлять клиента о новом туре, если совпадение не ниже N%');

-- Администратор по умолчанию: логин admin, пароль admin123
INSERT INTO users (user_id, role_id, login, password_hash, salt, full_name, email)
VALUES (1, 1, 'admin',
        SHA2(CONCAT('0123456789abcdef0123456789abcdef', 'admin123'), 256),
        '0123456789abcdef0123456789abcdef',
        'Администратор системы', 'admin@touragency.local');
