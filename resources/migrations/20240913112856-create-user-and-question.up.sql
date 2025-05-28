CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    username VARCHAR(100) UNIQUE,
    is_winner BOOLEAN DEFAULT NULL
);
--;;
CREATE TABLE questions (
    id SERIAL PRIMARY KEY,
    text TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
--;;
CREATE TABLE question_options (
    id SERIAL PRIMARY KEY,
    question_id INT REFERENCES questions(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    is_correct BOOLEAN DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0
);
--;;
CREATE TABLE user_answers (
    id SERIAL PRIMARY KEY,
    question_message_id BIGINT,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    question_id INT REFERENCES questions(id) ON DELETE CASCADE,
    answer_text TEXT,
    option_id INT REFERENCES question_options(id),
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
--;;
INSERT INTO questions (text, sort_order)
VALUES 
    ('В каком подразделении ты работаешь? (можно кратко)', 10),
    ('Твоя должность в компании.', 20),
    ('Слышали ли вы ранее про Центр инноваций?', 30),
    ('Ты работаешь с продуктом(ами) ЛАНИТ? (развиваешь, разрабатываешь. продаешь, продвигаешь)', 40),
    
    ('Отлично! А теперь квиз :) Всего 5 вопросов отделяют тебя от цели!\nПогнали, первый вопрос!\nВыбери правильно сочетание: Продуктовый...', 50),
    ('Как называется резкое изменение направления стартапа?', 60),
    ('Как называется главный проект Центра инноваций по развитию идей и продуктов?', 70),
    ('Если у вас есть идея продукта, куда с ней идти в Центре инноваций?', 80),
    ('Где собрана информация по всем продуктам ЛАНИТ?', 90)
    ;
--;;
INSERT INTO question_options (question_id, text, is_correct, sort_order)
VALUES
    (3, 'Да', FALSE, 1),
    (3, 'Что-то слышал, но не вникал', FALSE, 2),
    (3, 'Нет', FALSE, 3),
    
    (4, 'Да', FALSE, 1),
    (4, 'Нет', FALSE, 2),
    (4, 'Нет, но хотелось бы', FALSE, 3),
    
    (5, '...приход', FALSE, 1),
    (5, '...подход', TRUE, 2),
    (5, '...подлет', FALSE, 3),
    (5, '...улет', FALSE, 4),
    
    (6, 'Пивот', TRUE, 1),
    (6, 'Факап', FALSE, 2),
    (6, 'Ничё се поворот', FALSE, 3),
    (6, 'Ну всё, расходимся', FALSE, 4),

    (7, 'Трансформатор', FALSE, 1),
    (7, 'Коммутатор', FALSE, 2),
    (7, 'Акселератор', TRUE, 3),
    (7, 'Рефрежератор', FALSE, 4),

    (8, 'На ярмарку идей', FALSE, 1),
    (8, 'На портал идей', TRUE, 2),
    (8, 'На склад идей', FALSE, 3),
    (8, 'На фиг', FALSE, 4),
    
    (9, 'Склад продуктов', FALSE, 1),
    (9, 'Реестр продуктов', TRUE, 2),
    (9, 'Продуктовая корзина', FALSE, 3),
    (9, 'Продуктовая антресоль', FALSE, 4)
    ;
